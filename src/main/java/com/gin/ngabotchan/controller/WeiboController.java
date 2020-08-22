package com.gin.ngabotchan.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gin.ngabotchan.entity.WeiboCard;
import com.gin.ngabotchan.service.ConfigService;
import com.gin.ngabotchan.service.NgaService;
import com.gin.ngabotchan.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static com.gin.ngabotchan.service.impl.NgaServiceImpl.replaceLinks;

/**
 * @author bx002
 */
@Slf4j
@RestController
@RequestMapping("/wb")
public class WeiboController {
    static String nbsp = NgaService.NBSP;
    /**
     * 自动搬运开关
     */
    static boolean auto = false;
    /**
     * 测试模式开关，测试模式下将把搬运内容发到水楼；关闭后发到主版面
     */
    static boolean testMode = true;

    /**
     * 已经转发过的微博id
     */
    static List<String> idList = new ArrayList<>();

    final NgaService ngaService;

    public WeiboController(NgaService ngaService) throws UnsupportedEncodingException {
        this.ngaService = ngaService;
    }

    /**
     * @param uid 目标微博用户ui
     * @param b   是否使用第二个接口获取准确时间
     * @return 第一页微博内容
     */
    @RequestMapping("get")
    public List<WeiboCard> get(String uid, Boolean b) {
        b = b == null ? true : b;
        //第一个接口
        String u = "https://m.weibo.cn/api/container/getIndex?type=uid&value=" + uid + "&containerid=107603" + uid;
        String result = RequestUtil.get(u, "", null, "", "utf-8");
        JSONObject json = JSONObject.parseObject(result);
        JSONArray cards = json.getJSONObject("data").getJSONArray("cards");
        JSONArray statuses;
        Map<String, String> createdAtMap = new HashMap<>(cards.size());

        if (b) {
            //另一个接口
            String u2 = "https://m.weibo.cn/profile/info?uid=" + uid;
            String result2 = RequestUtil.get(u2, "", null, "", "utf-8");
            JSONObject json2 = JSONObject.parseObject(result2);
            statuses = json2.getJSONObject("data").getJSONArray("statuses");

            createdAtMap = new HashMap<>(statuses.size());
            for (int i = 0; i < statuses.size(); i++) {
                String createdAt = statuses.getJSONObject(i).getString("created_at");
                String id = statuses.getJSONObject(i).getString("id");
                createdAtMap.put(id, createdAt);
            }
        }

        List<WeiboCard> list = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            JSONObject card = cards.getJSONObject(i);
            WeiboCard w = new WeiboCard(card);

            if (StringUtils.isEmpty(w.getId()) || w.getRawText().length() < 20) {
                continue;
            }
            if (b) {
                w.setCreatedTime(createdAtMap.get(w.getId()));
            }


            list.add(w);
        }
        return list;
    }


    @RequestMapping("/gf")
    public List<WeiboCard> gf() {
        List<WeiboCard> list = get("5611537367", true);

        list.forEach(card -> parseGirlsFrontLineCards(card, false));


        return list;
    }

    @RequestMapping("/auto")
    public String auto() {
        auto = !auto;
        return "自动转发： " + (auto ? "开启" : "关闭");
    }

    @RequestMapping("/testMode")
    public String testMode() {
        testMode = !testMode;
        return "测试模式： " + (testMode ? "开启" : "关闭");
    }

    /**
     * 自动转发
     */
    @Scheduled(cron = "0/10 * * * * *")
    public void scScheduledGf() {
        if (!auto) {
            return;
        }

        List<WeiboCard> list = get("5611537367", false);

        for (WeiboCard card : list) {
            String at = card.getCreatedAt();

            boolean recent = at.contains("刚") || at.contains("1分钟") || at.contains("2分钟");
            if (!idList.contains(card.getId()) && recent) {

                parseGirlsFrontLineCards(card, true);


                Collection<String> cookies = ConfigService.COOKIE_MAP.keySet();

                for (String cookie : cookies) {
                    log.info("自动发帖：" + card.getTitle());
                    String s;
                    if (testMode) {
                        s = reply(card.getContent(), card.getTitle(), "少女前线", "少前水楼", cookie);
                    } else {
                        s = newTheme(card.getTitle(), card.getContent(), "少女前线", cookie);
                    }
                    if (s.contains("http")) {
                        idList.add(card.getId());
                        break;
                    }
                }
            }

        }

    }


    @RequestMapping("/newTheme")
    public String newTheme(String title, String content, String fid, String cookie) {
        fid = ConfigService.FID_MAP.get(fid);
        cookie = ConfigService.COOKIE_MAP.get(cookie);
        String result = ngaService.newTheme(title, content, fid, cookie);
        return result;
    }

    @RequestMapping("/reply")
    public String reply(String content, String title, String fid, String tid, String cookie) {
        fid = ConfigService.FID_MAP.get(fid);
        cookie = ConfigService.COOKIE_MAP.get(cookie);
        tid = ConfigService.TID_MAP.get(tid);
        String result = ngaService.reply(content, title, fid, tid, cookie);
        return result;
    }

    /**
     * 解析少前微博的发言格式，设置发帖的标题和正文
     *
     * @param card
     * @param fullText 是否请求全文
     * @return
     */
    private static WeiboCard parseGirlsFrontLineCards(WeiboCard card, boolean fullText) {
        //设置标题
        String rawText = card.getRawText();
        String title = "";
        if (rawText.contains("#")) {
            title = rawText.substring(0, rawText.lastIndexOf("#") + 1);
            rawText = rawText.substring(rawText.lastIndexOf("#") + 1);
            boolean b = true;
            while (title.contains("#")) {
                if (b) {
                    title = title.replaceFirst("#", "[");
                } else {
                    title = title.replaceFirst("#", "]");
                }
                b = !b;
            }
        }
        if (rawText.contains("！")) {
            title += rawText.substring(0, rawText.indexOf("！") + 1);
        }
        //如果是维护公告
        if (rawText.contains("维护具体结束时间")) {
            int start = rawText.indexOf("计划于") + 3;
            int end = rawText.indexOf("进行");
            title = "[微博拌匀] 维护公告 " + rawText.substring(start, end);
        }
        //如果是维护延长
        if (rawText.contains("维护延长")) {
            int start = rawText.indexOf("原定于");
            int end = rawText.indexOf("开服。") + 3;
            title = "[微博拌匀] 维护延长公告 " + rawText.substring(start, end);
        }
        //设置title  保险
        if ("".equals(title)) {
            title = rawText.substring(0, 20);
        }

        //设置正文
        StringBuilder content = new StringBuilder(rawText);

        if (fullText) {
            //获取全文
            String result = RequestUtil.get("https://m.weibo.cn/status/" + card.getId(),
                    null, null, null, "utf-8");
            result = result.substring(result.indexOf("\"text\":") + 9);
            result = result.substring(0, result.indexOf("\","));
            result = replaceLinks(result);
            content = new StringBuilder(result);
        }


        List<String> pics = card.getPics();
        if (pics != null) {
            content.append(nbsp);

            for (String pic : pics) {
                content.append(nbsp).append("[img]").append(pic).append("[/img]");
            }

        }

        content.append(nbsp).append(nbsp);
        content.append("[url=").append(card.getSourceUrl()).append("]原微博[/url]");


        card.setTitle(title);
        card.setContent(content.toString());


        return card;
    }
}
