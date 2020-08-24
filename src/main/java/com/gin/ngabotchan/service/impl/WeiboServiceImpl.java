package com.gin.ngabotchan.service.impl;

import com.gin.ngabotchan.entity.WeiboCard;
import com.gin.ngabotchan.service.NgaService;
import com.gin.ngabotchan.service.WeiboService;
import com.gin.ngabotchan.util.RequestUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.gin.ngabotchan.service.impl.NgaServiceImpl.replaceLinks;

@Service
public class WeiboServiceImpl implements WeiboService {
    final static List<String> invalidKeyword = new ArrayList<>();

    static {
        invalidKeyword.add("亲爱的指挥官们，");
    }

    static String nbsp = NgaService.NBSP;

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


    }
}
