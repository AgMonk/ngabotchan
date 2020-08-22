package com.gin.ngabotchan.controller;

import com.gin.ngabotchan.service.NgaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nga")
public class NgaController {

    final NgaService ngaService;

    public NgaController(NgaService ngaService) {
        this.ngaService = ngaService;
    }

    @RequestMapping("/newTheme")
    public String newTheme(String title, String content, String fid, String cookie) {
        return ngaService.newTheme(title, content, fid, cookie);
    }

    @RequestMapping("/reply")
    public String reply(String content, String title, String fid, String tid, String cookie) {
        return ngaService.reply(content, title, fid, tid, cookie);
    }
}
