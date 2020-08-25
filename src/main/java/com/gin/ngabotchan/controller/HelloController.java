package com.gin.ngabotchan.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class HelloController {
    @RequestMapping("/")
    public void hello(HttpServletResponse response) throws IOException {
        response.sendRedirect("/index.html");
    }
}
