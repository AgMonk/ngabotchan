package com.gin.ngabotchan.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 一条微博
 *
 * @author bx002
 */
@Data
public class WeiboCard {
    static String pattern = "EEE MMM dd HH:mm:ss Z yyyy";
    static String pattern2 = "yyyy-MM-dd HH:mm:ss";
    static SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
    static SimpleDateFormat format = new SimpleDateFormat(pattern2);

    String sourceUrl, id, createdAt, rawText, createdStr, content, title;
    Long createdTime;
    List<String> pics;


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

    public void setCreatedTime(String createdTime) {
        try {
            this.createdTime = sdf.parse(createdTime).getTime();

            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CTT"));

            createdStr = format.format(new Date(this.createdTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
