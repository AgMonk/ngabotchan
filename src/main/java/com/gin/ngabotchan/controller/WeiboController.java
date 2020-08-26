package com.gin.ngabotchan.controller;

import com.gin.ngabotchan.entity.WeiboCard;
import com.gin.ngabotchan.service.ConfigService;
import com.gin.ngabotchan.service.NgaService;
import com.gin.ngabotchan.service.WeiboService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author bx002
 */
@Slf4j
@RestController
@RequestMapping("/wb")
public class WeiboController {
    /**
     * 测试模式开关，测试模式下将把搬运内容发到水楼；关闭后发到主版面
     */
    static boolean testMode = true;

    final NgaService ngaService;
    final WeiboService weiboService;

    public WeiboController(NgaService ngaService, WeiboService weiboService) {
        this.ngaService = ngaService;
        this.weiboService = weiboService;
    }

    @RequestMapping("/getCards")
    public List<WeiboCard> getCards(String uid, Integer newCard) {
        List<WeiboCard> cardList = WeiboService.CARD_MAP.get(uid);
        if (cardList == null) {
            weiboService.updateCards(uid);
        }

        if (newCard == null) {
            return WeiboService.CARD_MAP.get(uid);
        }
        return WeiboService.CARD_MAP_NEW.get(uid);
    }

    @RequestMapping("/gf")
    public List<WeiboCard> gf() {
        return getCards(WeiboCard.UID_GIRLS_FRONT_LINE, null);
    }


    @RequestMapping("/testMode")
    public String testMode() {
        testMode = !testMode;
        return "测试模式： " + (testMode ? "开启" : "关闭");
    }


    @Scheduled(cron = "1/30 * 15-16 * * *")
    @Scheduled(cron = "1/10 * 17-18 * * *")
    @Scheduled(cron = "0 0/30 * * * *")
    public void scanGf() {
        String girlsFrontLine = WeiboCard.UID_GIRLS_FRONT_LINE;

        weiboService.updateCards(girlsFrontLine);

        List<WeiboCard> listNew = WeiboService.CARD_MAP_NEW.get(girlsFrontLine);
        if (listNew == null) {
            return;
        }
        int size = listNew.size();
        if (size > 0) {
            for (WeiboCard card : listNew) {
                Collection<String> cookies = ConfigService.COOKIE_MAP.keySet();

                for (String cookie : cookies) {
                    String result = weiboService.repost("少女前线", "少前水楼", card, cookie, testMode);
                    if (result.contains("http")) {
                        break;
                    }
                }
            }
            WeiboService.CARD_MAP_NEW.put(girlsFrontLine, new ArrayList<>());
        }
    }

    @RequestMapping("/repost")
    public String repost(String fid, String tid, String cardId, String cookie, String action) {
        WeiboCard card = null;
        for (List<WeiboCard> value : WeiboService.CARD_MAP.values()) {
            for (WeiboCard c : value) {
                if (c.getId().equals(cardId)) {
                    card = c;
                    break;
                }
            }
        }
        return weiboService.repost(fid, tid, card, cookie, !"new".equals(action));
    }

}
