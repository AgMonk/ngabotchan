package com.gin.ngabotchan.controller;

import com.gin.ngabotchan.service.ConfigService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @RequestMapping("/get")
    public Object get() {
        Map<String, Object> map = new HashMap<>();

        map.put("cookie", ConfigService.COOKIE_MAP.keySet());
        map.put("fid", ConfigService.FID_MAP.keySet());
        map.put("tid", ConfigService.TID_MAP.keySet());

        return map;
    }
}
