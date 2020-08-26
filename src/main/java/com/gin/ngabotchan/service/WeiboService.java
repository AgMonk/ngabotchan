package com.gin.ngabotchan.service;

import com.gin.ngabotchan.entity.WeiboCard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface WeiboService {

    Map<String, List<WeiboCard>> CARD_MAP = new HashMap<>();
    Map<String, List<WeiboCard>> CARD_MAP_NEW = new HashMap<>();

    /**
     * 解析少前微博的发言格式，设置发帖的标题和正文
     *
     * @param card     微博
     * @param fullText 是否请求全文
     */
    void parseGirlsFrontLineCards(WeiboCard card, boolean fullText);


    /**
     * 更新第一页微博内容
     *
     * @param uid 微博用户id
     */
    void updateCards(String uid);

    /**
     * 转发到回复
     *
     * @param fid  版面id
     * @param tid  帖子id
     * @param card 微博
     * @return 响应结果
     */
    String repost(String fid, String tid, WeiboCard card, boolean testMode);

}
