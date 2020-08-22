package com.gin.ngabotchan.service;


import com.gin.ngabotchan.util.RequestUtil;

public interface NgaService {
    String NBSP = RequestUtil.decode("%0D%0A");

    /**
     * 发表主题
     *
     * @param title   标题
     * @param content 正文
     * @param fid     版面id
     * @param cookie  cookie
     * @return 返回
     */
    String newTheme(String title, String content, String fid, String cookie);

    /**
     * 发表回复
     *
     * @param content 正文
     * @param fid     版面id
     * @param tid     帖子id
     * @param cookie  cookie
     * @return 返回
     */
    String reply(String content, String title, String fid, String tid, String cookie);


}
