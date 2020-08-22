package com.gin.ngabotchan.service;

import com.gin.ngabotchan.entity.WeiboCard;

public interface WeiboService {
    /**
     * 解析少前微博的发言格式，设置发帖的标题和正文
     *
     * @param card     微博
     * @param fullText 是否请求全文
     */
    void parseGirlsFrontLineCards(WeiboCard card, boolean fullText);
}
