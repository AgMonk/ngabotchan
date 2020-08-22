package com.gin.ngabotchan.service.impl;

import com.gin.ngabotchan.service.ConfigService;
import com.gin.ngabotchan.service.NgaService;
import com.gin.ngabotchan.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class NgaServiceImpl implements NgaService {


    /**
     * @param title   标题
     * @param content 正文
     * @param fid     版面id
     * @param cookie  cookie
     * @return 返回
     */
    @Override
    public String newTheme(String title, String content, String fid, String cookie) {
        cookie = findCookieFidTid(cookie, ConfigService.COOKIE_MAP);
        fid = findCookieFidTid(fid, ConfigService.FID_MAP);

        Map<String, String[]> paramMap = new HashMap<>(10);
        paramMap.put("action", new String[]{"new"});
        paramMap.put("fid", new String[]{fid});
        paramMap.put("lite", new String[]{"htmljs"});
        paramMap.put("step", new String[]{"2"});
        paramMap.put("post_subject", new String[]{title});
        paramMap.put("post_content", new String[]{content});

        String post = RequestUtil.post("https://bbs.nga.cn/post.php",
                "", paramMap, null, cookie, "gbk");

        if (post.contains("发贴完毕")) {
            int s = post.lastIndexOf("/read.php?tid=");
            int e = post.lastIndexOf("&_ff");
            post = "https://bbs.nga.cn" + post.substring(s, e);
        }
        if (post.contains("你没有登录")) {
            post = "请先在cookie.txt文件中设置发帖的账号cookie，并选择发帖账号";

        }
        System.err.println(post);

        return post;
    }

    /**
     * 发表回复
     *
     * @param content 正文
     * @param fid     版面id
     * @param tid     帖子id
     * @param cookie  cookie
     * @return 返回
     */
    @Override
    public String reply(String content, String title, String fid, String tid, String cookie) {
        cookie = findCookieFidTid(cookie, ConfigService.COOKIE_MAP);
        fid = findCookieFidTid(fid, ConfigService.FID_MAP);
        tid = findCookieFidTid(tid, ConfigService.TID_MAP);


        Map<String, String[]> paramMap = new HashMap<>(10);
        paramMap.put("action", new String[]{"reply"});
        paramMap.put("fid", new String[]{fid});
        paramMap.put("tid", new String[]{tid});
        paramMap.put("lite", new String[]{"htmljs"});
        paramMap.put("step", new String[]{"2"});
        paramMap.put("post_subject", new String[]{title});
        paramMap.put("post_content", new String[]{content});
        paramMap.put("tpic_misc_bit1", new String[]{"40"});
        String post = RequestUtil.post("https://bbs.nga.cn/post.php",
                "", paramMap, null, cookie, "gbk");

        if (post.contains("发贴完毕")) {
            int s = post.indexOf("/read.php?tid=");
            int e = post.indexOf("\",\"5\"");
            post = "https://bbs.nga.cn" + post.substring(s, e);
        }
        if (post.contains("你没有登录")) {
            post = "请先在cookie.txt文件中设置发帖的账号cookie，并选择发帖账号";
        }

        System.err.println(post);
        return post;
    }

    /**
     * 把html代码中的a标签替换为NGA格式的链接
     *
     * @param html html代码
     */
    public static String replaceLinks(String html) {
        Document document = Jsoup.parse(html);
        html = document.text();
        Elements aTags = document.getElementsByTag("a");
        for (Element aTag : aTags) {
            String href = aTag.attr("href").replace("\\\"", "");
            String text = aTag.text();
            String ngaTag = "[url=" + href + "]" + text + "[/url]";
            html = html.replace(text, ngaTag);
        }

        return html;
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
