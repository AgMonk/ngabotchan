package com.gin.ngabotchan.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gin.ngabotchan.entity.WeiboCard;
import com.gin.ngabotchan.service.ConfigService;
import com.gin.ngabotchan.service.NgaService;
import com.gin.ngabotchan.service.WeiboService;
import com.gin.ngabotchan.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @author bx002
 */
@Slf4j
@RestController
@RequestMapping("/wb")
public class WeiboController {
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
    final WeiboService weiboService;

    public WeiboController(NgaService ngaService, WeiboService weiboService) {
        this.ngaService = ngaService;
        this.weiboService = weiboService;
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


    @RequestMapping("/gf")
    public List<WeiboCard> gf() {
        List<WeiboCard> list = get("5611537367", true);

        list.forEach(card -> weiboService.parseGirlsFrontLineCards(card, false));


        return list;
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

            boolean recent = at.contains("刚") || "1分钟前".equals(at) || "2分钟前".equals(at);
            if (!idList.contains(card.getId()) && recent) {

                weiboService.parseGirlsFrontLineCards(card, true);


                Collection<String> cookies = ConfigService.COOKIE_MAP.keySet();

                for (String cookie : cookies) {
                    log.info("自动发帖：" + card.getTitle());
                    String s;
                    if (testMode) {
                        s = ngaService.reply(card.getContent(), card.getTitle(), "少女前线", "少前水楼", cookie);
                    } else {
                        s = ngaService.newTheme(card.getTitle(), card.getContent(), "少女前线", cookie);
                    }
                    if (s.contains("http")) {
                        idList.add(card.getId());
                        break;
                    }
                }
            }

        }

    }


}
