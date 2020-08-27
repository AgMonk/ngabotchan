package com.gin.ngabotchan.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gin.ngabotchan.entity.WeiboCard;
import com.gin.ngabotchan.service.NgaService;
import com.gin.ngabotchan.service.WeiboService;
import com.gin.ngabotchan.util.ReqUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class WeiboServiceImpl implements WeiboService {

    final Executor scanExecutor;
    final Executor downloadExecutor;
    final NgaService ngaService;

    Long start = System.currentTimeMillis();

    public WeiboServiceImpl(Executor scanExecutor, Executor downloadExecutor, NgaService ngaService) {
        this.scanExecutor = scanExecutor;
        this.downloadExecutor = downloadExecutor;
        this.ngaService = ngaService;
    }


    /**
     * 转发
     *
     * @param fid  版面id
     *             ++++++++++     * @param tid  帖子id
     * @param card 微博
     * @return 响应结果
     */
    @Override
    public String autoRepost(String fid, String tid, WeiboCard card, String cookie, boolean testMode) {
        log.info("自动发帖：" + card.getTitle());
        String s = "";
        if (testMode) {
            s = ngaService.reply(card.getBbsCode(), card.getTitle(), fid, tid, cookie, card.getPicFiles());
        } else {
            s = ngaService.newTheme(card.getBbsCode(), card.getTitle(), fid, cookie, card.getPicFiles());
        }
        if (s.contains("http")) {
            log.info("发帖成功 地址:{}", s);
        }
        return s;
    }

    @Override
    public void updateCards(String uid) {
        Long now = System.currentTimeMillis();

        start = now;

        //查询
        log.info("查询微博 UID:" + uid);
        String result = ReqUtil.get("https://m.weibo.cn/profile/info?uid=", "", uid, null);
        JSONObject json = JSONObject.parseObject(result);
        JSONArray cards = json.getJSONObject("data").getJSONArray("statuses");

        //检查是否有新微博
        boolean b = true;
        List<WeiboCard> oldList = CARD_MAP.get(uid);
        List<WeiboCard> newList = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            JSONObject o = cards.getJSONObject(i);
            if (o.getInteger("repost_type") != null && o.getInteger("repost_type").equals(1)) {
                continue;
            }
            WeiboCard e = new WeiboCard(o);
            if (oldList == null || !oldList.contains(e)) {
                b = false;
            }
            newList.add(e);
        }

        if (b) {
            log.info("没有新微博 UID:" + uid);
            return;
        }

        log.debug("更新微博数据 UID：" + uid);
        CountDownLatch latch = new CountDownLatch(newList.size());
        for (WeiboCard card : newList) {
            scanExecutor.execute(() -> {
                card.fullText();
                card.parseTitle(uid);
                card.downloadPics(downloadExecutor, log);
                //添加到新卡片Map
                if (System.currentTimeMillis() - card.getCreatedTime() < 2 * 60 * 1000) {
                    synchronized (CARD_MAP_NEW) {
                        List<WeiboCard> cardList = CARD_MAP_NEW.get(uid);
                        cardList = cardList == null ? new ArrayList<>() : cardList;
                        cardList.add(card);
                        CARD_MAP_NEW.put(uid, cardList);
                    }
                }
                latch.countDown();
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        CARD_MAP.put(uid, newList);
        Long end = System.currentTimeMillis();
        log.info("更新结束 UID:{} 耗时: {}毫秒", uid, (end - now));
        log.info("发现新微博({})", CARD_MAP_NEW.get(uid) == null ? 0 : CARD_MAP_NEW.get(uid).size());

    }

}
