package com.gin.ngabotchan.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gin.ngabotchan.entity.WeiboCard;
import com.gin.ngabotchan.service.ConfigService;
import com.gin.ngabotchan.service.NgaService;
import com.gin.ngabotchan.service.WeiboService;
import com.gin.ngabotchan.util.ReqUtil;
import com.gin.ngabotchan.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static com.gin.ngabotchan.service.impl.NgaServiceImpl.replaceLinks;

@Slf4j
@Service
public class WeiboServiceImpl implements WeiboService {
    final static List<String> invalidKeyword = new ArrayList<>();

    static {
        invalidKeyword.add("亲爱的指挥官们，");
        invalidKeyword.add("亲爱的指挥官，");
    }

    final Executor scanExecutor;
    final Executor downloadExecutor;
    final NgaService ngaService;

    Long start = System.currentTimeMillis();

    static String nbsp = NgaService.NBSP;

    public WeiboServiceImpl(Executor scanExecutor, Executor downloadExecutor, NgaService ngaService) {
        this.scanExecutor = scanExecutor;
        this.downloadExecutor = downloadExecutor;
        this.ngaService = ngaService;
    }


    /**
     * 转发到回复
     *
     * @param fid  版面id
     * @param tid  帖子id
     * @param card 微博
     * @return 响应结果
     */
    @Override
    public String repost(String fid, String tid, WeiboCard card, boolean testMode) {
        Collection<String> cookies = ConfigService.COOKIE_MAP.keySet();

        for (String cookie : cookies) {
            log.info("发帖：" + card.getTitle());
            String s = "";
            if (testMode) {
                s = ngaService.reply(card.getBbsCode(), card.getTitle(), fid, tid, cookie, card.getPicFiles());
            } else {
                s = ngaService.newTheme(card.getTitle(), card.getBbsCode(), fid, cookie, card.getPicFiles());
            }
            if (s.contains("http")) {
                log.info("发帖成功 地址:{}", s);
                return s;
            }
        }

        return null;
    }

    /**
     * 解析少前微博的发言格式，设置发帖的标题和正文
     *
     * @param card     微博
     * @param fullText 是否请求全文
     */
    @Override
    public void parseGirlsFrontLineCards(WeiboCard card, boolean fullText) {
        //设置标题
        String rawText = card.getRawText();
        String title = "";
        String well = "#";
        if (rawText.contains(well)) {
            title = rawText.substring(0, rawText.lastIndexOf(well) + 1);
            rawText = rawText.substring(rawText.lastIndexOf(well) + 1);
            boolean b = true;
            while (title.contains(well)) {
                if (b) {
                    title = title.replaceFirst(well, "[");
                } else {
                    title = title.replaceFirst(well, "]");
                }
                b = !b;
            }
        }


        String Exclamation = "！";
        String period = "。";
        if (rawText.contains(Exclamation) || rawText.contains(period)) {
            int e1 = rawText.indexOf(Exclamation);
            int e2 = rawText.indexOf(period);
            e1 = e1 == -1 ? 100 : e1;
            e2 = e2 == -1 ? 100 : e2;
            title += rawText.substring(0, Math.min(e1, e2) + 1);
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

        //删除废话
        for (String s : invalidKeyword) {
            if (title.contains(s)) {
                title = title.replace(s, "");
            }
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


//        List<String> pics = card.getPics();
//        if (pics != null) {
//            content.append(nbsp);
//
//            for (String pic : pics) {
//                content.append(nbsp).append("[img]").append(pic).append("[/img]");
//            }
//
//        }

        content.append(nbsp);

        content.append("[url=").append(card.getSourceUrl()).append("]原微博[/url]").append(nbsp).append(nbsp);


        card.setTitle(title);
        card.setContent(content.toString());


    }

    @Override
    public void updateCards(String uid) {
        Long now = System.currentTimeMillis();

        start = now;

        //查询
        String result = ReqUtil.get("https://m.weibo.cn/profile/info?uid=", "", uid, null);
        JSONObject json = JSONObject.parseObject(result);
        JSONArray cards = json.getJSONObject("data").getJSONArray("statuses");

        //检查是否有新微博
        boolean b = true;
        List<WeiboCard> oldList = CARD_MAP.get(uid);
        for (int i = 0; i < cards.size(); i++) {
            JSONObject o = cards.getJSONObject(i);
            WeiboCard card = new WeiboCard(o, true);
            if (oldList == null || !oldList.contains(card)) {
                b = false;
                break;
            }
        }

        if (b) {
            return;
        }

        log.debug("更新微博数据 UID：" + uid);
        List<WeiboCard> list = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(cards.size());
        for (int i = 0; i < cards.size(); i++) {
            JSONObject o = cards.getJSONObject(i);
            WeiboCard card = new WeiboCard(o, true);
            scanExecutor.execute(() -> {
                card.fullText();
                card.parseTitle(uid);
                card.downloadPics(downloadExecutor, log);
                //添加到新卡片Map
                if (System.currentTimeMillis() - card.getCreatedTime() < 5 * 60 * 1000) {
                    synchronized (CARD_MAP_NEW) {
                        List<WeiboCard> cardList = CARD_MAP_NEW.get(uid);
                        cardList = cardList == null ? new ArrayList<>() : cardList;
                        cardList.add(card);
                        CARD_MAP_NEW.put(uid, cardList);
                    }
                }
                latch.countDown();
            });
            list.add(card);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        CARD_MAP.put(uid, list);
        Long end = System.currentTimeMillis();
        log.info("更新结束 UID:{} 耗时: {}毫秒", uid, (end - now));
        log.info("发现新微博({})", CARD_MAP_NEW.get(uid) == null ? 0 : CARD_MAP_NEW.get(uid).size());

    }

}
