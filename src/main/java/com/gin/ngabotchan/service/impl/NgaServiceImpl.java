package com.gin.ngabotchan.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.gin.ngabotchan.service.ConfigService;
import com.gin.ngabotchan.service.NgaService;
import com.gin.ngabotchan.util.ReqUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NgaServiceImpl implements NgaService {
    static final int MAX_TITLE_LENGTH = 100;

    /**
     * @param title   标题
     * @param content 正文
     * @param fid     版面id
     * @param cookie  cookie
     * @param files
     * @return 返回
     */
    @Override
    public String newTheme(String title, String content, String fid, String cookie, File[] files) {
        cookie = findCookieFidTid(cookie, ConfigService.COOKIE_MAP);
        fid = findCookieFidTid(fid, ConfigService.FID_MAP);

        Map<String, String[]> paramMap = createParamMap("new", fid, null, title);

        List<String> urlList = uploadFiles(paramMap, files, cookie, "new", fid, "");

        String post = send(content, cookie, paramMap, urlList);

        if (post.contains("发贴完毕")) {
            int s = post.lastIndexOf("/read.php?tid=");
            int e = post.lastIndexOf("&_ff");
            post = "https://bbs.nga.cn" + post.substring(s, e);
            log.info("已发帖 标题： " + title + " 地址：" + post);
        }


        return post;
    }

    /**
     * 发表回复
     *
     * @param content 正文
     * @param fid     版面id
     * @param tid     帖子id
     * @param cookie  cookie
     * @param files
     * @return 返回
     */
    @Override
    public String reply(String title, String content, String fid, String tid, String cookie, File[] files) {
        cookie = findCookieFidTid(cookie, ConfigService.COOKIE_MAP);
        fid = findCookieFidTid(fid, ConfigService.FID_MAP);
        tid = findCookieFidTid(tid, ConfigService.TID_MAP);

        Map<String, String[]> paramMap = createParamMap("reply", fid, tid, title);

        List<String> urlList = uploadFiles(paramMap, files, cookie, "reply", fid, tid);

        String post = send(content, cookie, paramMap, urlList);

        if (post.contains("发贴完毕")) {
            int s = post.indexOf("/read.php?tid=");
            int e = post.indexOf("\",\"5\"");
            post = "https://bbs.nga.cn" + post.substring(s, e);
            log.info("已发帖 标题： " + title);
        }

        return post;
    }

    private static Map<String, String[]> createParamMap(String action, String fid, String tid, String title) {

        Map<String, String[]> paramMap = new HashMap<>(10);
        paramMap.put("action", new String[]{action});
        paramMap.put("fid", new String[]{fid});
        paramMap.put("tid", new String[]{tid});
        paramMap.put("lite", new String[]{"htmljs"});
        paramMap.put("step", new String[]{"2"});
        paramMap.put("post_subject", new String[]{title});
        paramMap.put("tpic_misc_bit1", new String[]{"40"});

        return paramMap;
    }

    private static String send(String content, String cookie, Map<String, String[]> paramMap, List<String> urlList) {
        StringBuilder sb = new StringBuilder(content);
        if (urlList != null) {
            for (String url : urlList) {
                if (url.contains("mp4")) {
                    sb.append("[flash=video]./").append(url).append("[/flash]").append(NBSP);

                } else {
                    sb.append("[img]./").append(url).append("[/img]").append(NBSP);
                }
            }
        }
        paramMap.put("post_content", new String[]{sb.toString()});
        String post = ReqUtil.post("https://bbs.nga.cn/post.php", null, null,
                paramMap, cookie, null, null, null, null, "gbk");

        if (!post.contains("发贴完毕")) {
            log.info(post);
        }

        if (post.contains("你没有登录")) {
            post = "请先在cookie.txt文件中设置发帖的账号cookie，并选择发帖账号";

        }
        if (post.contains("标题过短或过长")) {
            log.info("标题:{}", paramMap.get("post_subject")[0]);
            post = "标题过短或过长";
        }
        return post;
    }

    private static List<String> uploadFiles(Map<String, String[]> paramMap, File[] files
            , String cookie, String action, String fid, String tid) {
        List<String> urls = null;
        if (files != null) {

            //请求attach_url和auth
            StringBuilder urlbuilder = new StringBuilder("https://bbs.nga.cn/post.php?");
            urlbuilder
                    .append("action=").append(action).append("&")
                    .append("fid=").append(fid).append("&")
            ;
            if (tid != null && !"".equals(tid)) {
                urlbuilder.append("tid=").append(tid).append("&");
            }
            urlbuilder.append("__output=14").append("&");
            String post = ReqUtil.post(urlbuilder.toString(), null
                    , null, null, cookie, null, null, null, null, "gbk");
            JSONObject json = JSONObject.parseObject(post);
            Integer code = json.getInteger("code");

            while (code == 39) {
                log.debug("发言CD中");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                post = ReqUtil.post(urlbuilder.toString(), null
                        , null, null, cookie, null, null, null, null, "gbk");
                json = JSONObject.parseObject(post);
                code = json.getInteger("code");
            }
            JSONObject result = json.getJSONArray("result").getJSONObject(0);
            String attach_url = result.getString("attach_url");
            String auth = result.getString("auth");


            StringBuilder ab = new StringBuilder();
            StringBuilder acb = new StringBuilder();

            for (int i = 0; i < files.length; i++) {
                JSONObject uploaded = uploadFile(i + 1, files[i], auth, attach_url, fid);
                JSONObject data = uploaded.getJSONObject("data");
                String attachments = data.getString("attachments");
                String attachments_check = data.getString("attachments_check");
                String url = data.getString("url");
                ab.append(attachments).append("\t");
                acb.append(attachments_check).append("\t");

                urls = urls == null ? new ArrayList<>() : urls;
                urls.add(url);
            }
//            ab.deleteCharAt(ab.length() - 1);
//            acb.deleteCharAt(acb.length() - 1);
            paramMap.put("attachments", new String[]{ab.toString()});
            paramMap.put("attachments_check", new String[]{acb.toString()});


        }
        return urls;
    }

    private static JSONObject uploadFile(int i, File file, String auth, String attach_url, String fid) {
        HashMap<String, String> formData = new HashMap<>();
        formData.put("attachment_file" + i + "_watermark", "");
        formData.put("attachment_file" + i + "_dscp", "image" + i);
        formData.put("attachment_file" + i + "_url_utf8_name", ReqUtil.encode(file.getName(), "utf-8"));
        if (file.length() >= 4 * 1024 * 1024) {
            formData.put("attachment_file" + i + "_auto_size", "1");
        } else {
            formData.put("attachment_file" + i + "_auto_size", "0");
        }
        formData.put("attachment_file" + i + "_img", "1");
        formData.put("func", "upload");
        formData.put("v2", "1");
        formData.put("auth", auth);
        formData.put("origin_domain", "bbs.nga.cn");
        formData.put("fid", fid);
        formData.put("__output", "8");
        HashMap<String, File> fileMap = new HashMap<>();
        fileMap.put("attachment_file" + i, file);
        log.info("上传文件:" + file.getName());
        String s = ReqUtil.post(attach_url, "", null,
                null, null, null, formData, fileMap,
                null, "gbk");
        return JSONObject.parseObject(s);
    }


    /**
     * 根据名称查找对应的fid、tid或cookie
     *
     * @param v   名称
     * @param map 查找目标
     * @return 返回
     */
    private static String findCookieFidTid(String v, Map<String, String> map) {
        String fromMap = map.get(v);
        v = !StringUtils.isEmpty(fromMap) ? fromMap : v;
        v = StringUtils.isEmpty(v) ? map.entrySet().iterator().next().getValue() : v;
        return v;
    }
}
