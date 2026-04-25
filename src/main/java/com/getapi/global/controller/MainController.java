package com.getapi.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("totalApis",     parseLong(redisTemplate.opsForValue().get("stats:totalApis")));
        model.addAttribute("totalUsers",    parseLong(redisTemplate.opsForValue().get("stats:totalUsers")));
        model.addAttribute("totalApiCalls", parseLong(redisTemplate.opsForValue().get("stats:totalApiCalls")));
        model.addAttribute("totalPoints",   parseLong(redisTemplate.opsForValue().get("stats:totalPoints")));
        return "index";
    }

    private long parseLong(String value) {
        if (value == null || value.equals("null")) return 0L;
        return Long.parseLong(value);
    }

    @GetMapping("/guide")
    public String guide() {
        return "guide";
    }

    @GetMapping("/download")
    public String download() {
        return "download";
    }
}
