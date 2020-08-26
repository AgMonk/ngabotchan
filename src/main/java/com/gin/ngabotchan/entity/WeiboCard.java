package com.gin.ngabotchan.entity;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gin.ngabotchan.util.ReqUtil;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * 一条微博
 *
 * @author bx002
 */
@Data
public class WeiboCard {
    //    static String nbsp = NgaService.NBSP;
    static String nbsp = "\r\n";
    static String pattern = "EEE MMM dd HH:mm:ss Z yyyy";
    static String pattern2 = "yyyy-MM-dd HH:mm:ss";
    static SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
    static SimpleDateFormat format = new SimpleDateFormat(pattern2);

    /**
     * 少女前线 5611537367
     */
    final static String UID_GIRLS_FRONT_LINE = "5611537367";
    /**
     * 云图计划 7308178516
     */
    final static String UID_PLAN_OF_CLOUD = "7308178516";

    final static List<String> invalidKeyword = new ArrayList<>();

    static {
        invalidKeyword.add("亲爱的指挥官们，");
        invalidKeyword.add("亲爱的指挥官，");
        invalidKeyword.add("[少女前线] ");
        invalidKeyword.add("现在为您带来的是");
        invalidKeyword.add("现在为各位指挥官带来");
        invalidKeyword.add("现在为大家带来");
    }

    String sourceUrl, id, createdAt, rawText, createdStr, content, title, bbsCode;
    Long createdTime;
    List<String> pics;
    File[] picFiles;


    public WeiboCard(JSONObject json) {
        JSONObject mblog = json.getJSONObject("mblog");

        if (mblog == null) {
            return;
        }

        id = mblog.getString("mid");
        sourceUrl = "https://m.weibo.cn/status/" + id;
        createdAt = mblog.getString("created_at");
        rawText = mblog.getString("raw_text");

        JSONArray pics = mblog.getJSONArray("pics");

        if (pics != null) {
            this.pics = new ArrayList<>(pics.size());
            for (int i = 0; i < pics.size(); i++) {
                String s = pics.getJSONObject(i).getJSONObject("large").getString("url");
                s = s.replace("https", "http");
                this.pics.add(s);
            }

        }
    }

    public WeiboCard(JSONObject json, boolean b) {
        this.createdAt = json.getString("created_at");
        this.id = json.getString("id");
        this.rawText = json.getString("text");
        this.sourceUrl = "https://m.weibo.cn/status/" + id;
        JSONArray pics = json.getJSONArray("pics");
        if (pics != null) {
            this.pics = new ArrayList<>(pics.size());
            for (int i = 0; i < pics.size(); i++) {
                String s = pics.getJSONObject(i).getJSONObject("large").getString("url");
                s = s.replace("https", "http");
                this.pics.add(s);
            }
        }

        try {
            this.createdTime = sdf.parse(this.createdAt).getTime();
            this.createdStr = format.format(new Date(this.createdTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void setCreatedTime(String createdTime) {
        try {
            this.createdTime = sdf.parse(createdTime).getTime();

            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CTT"));

            createdStr = format.format(new Date(this.createdTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String fullText() {
        String result = ReqUtil.get(sourceUrl, null, null, null);
        result = result.substring(result.indexOf("\"text\":") + 9);
        result = result.substring(0, result.indexOf("\","));
        this.rawText = result;
        this.content = Jsoup.parse(result).text();
        this.bbsCode = replaceLinks(result);
        this.bbsCode += "[url=" + sourceUrl + "]原微博[/url]" + nbsp + nbsp;
        return result;
    }

    public void downloadPics(Executor downloadExecutor, Logger log) {
        if (pics == null) {
            return;
        }

        //文件夹
        String dirPath = "card/" + id;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        CountDownLatch latch = new CountDownLatch(pics.size());
        for (String pic : pics) {
            downloadExecutor.execute(() -> {
                String pathname = dirPath + "/" + pic.substring(pic.lastIndexOf("/") + 1);
                File img = new File(pathname);
                if (!img.exists()) {
                    log.debug("下载文件({})：" + pic, id);
                    HttpUtil.downloadFile(pic, dir, 30 * 1000);
                }
                latch.countDown();
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        picFiles = dir.listFiles();
    }


    /**
     * 把html代码中的a标签替换为NGA格式的链接
     *
     * @param html html代码
     */
    private static String replaceLinks(String html) {
        Document document = Jsoup.parse(html);
        html = document.text();
        Elements aTags = document.getElementsByTag("a");
        for (Element aTag : aTags) {
            String href = aTag.attr("href").replace("\\\"", "");
            String text = aTag.text();
            String ngaTag = "[url=" + href + "]" + text + "[/url]";
            html = html.replace(text, ngaTag);
        }

        html += nbsp + nbsp;
        return html;
    }


    /**
     * 根据uid解析标题
     *
     * @param uid 微博用户id
     */
    public void parseTitle(String uid) {
        switch (uid) {
            case UID_GIRLS_FRONT_LINE:
                parseGirlsFrontLineTitle();
                break;
            case UID_PLAN_OF_CLOUD:
                break;
            default:
                break;
        }
    }

    /**
     * 按照少前微博格式解析帖子标题
     */
    private void parseGirlsFrontLineTitle() {
        String well = "#";
        String c = this.content;
        if (c == null || "".equals(c)) {
            return;
        }
        StringBuilder tb = new StringBuilder();

        boolean b = true;
        while (c.contains(well)) {
            if (b) {
                c = c.replaceFirst(well, "[");
            } else {
                c = c.replaceFirst(well, "]");
            }
            b = !b;
        }

        //删除废话
        for (String s : invalidKeyword) {
            if (c.contains(s)) {
                c = c.replace(s, "");
            }
        }

        String exclamation = "！";
        String period = "。";
        String comma = "，";
        if (c.contains("主题装扮") && c.contains("介绍第")) {
            tb.append(c, 0, c.indexOf(comma) + 1);
            String t = c.substring(c.indexOf(comma) + 1);
            int start = t.indexOf("介绍的是") + 4;
            int end = t.indexOf(comma);
            tb.append(t.substring(start, end));

        } else if (c.contains("维护具体结束时间")) {
            //维护公告
            int start = c.indexOf("计划于") + 3;
            int end = c.indexOf("进行");
            tb.append("维护公告： ").append(c, start, end);
        } else if (c.contains("维护延长")) {
            //维护延长公告
            int start = c.indexOf("原定于");
            int end = c.indexOf("开服。") + 3;
            tb.append("维护延长公告：").append(c, start, end);
        } else if (c.contains("服装商城")) {
            int start = c.indexOf("指挥官");
            int end = c.indexOf(comma);
            tb.append(c, start, end).append(c.substring(c.indexOf("将在"), start));
        } else if (c.contains(exclamation) || c.contains(period)) {
            //有感叹号或句号
            int e1 = c.indexOf(exclamation);
            int e2 = c.indexOf(period);
            e1 = e1 == -1 ? 100 : e1;
            e2 = e2 == -1 ? 100 : e2;
            tb.append(c, 0, Math.min(e1, e2) + 1);
        } else {
            tb.append(c, 0, 20);
        }
        if (tb.length() >= 65) {
            tb.subSequence(0, 65);
        }
        if (tb.length() < 50) {
            tb.insert(0, "[微博搬运] ");
        }
        this.title = tb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WeiboCard card = (WeiboCard) o;

        return id.equals(card.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
