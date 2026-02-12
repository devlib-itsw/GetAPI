package com.DevLib.controller;

import com.DevLib.service.SmsAuthService;
import com.DevLib.util.JwtUtil;
import com.DevLib.util.SecureUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private final JwtUtil jwtUtil;

    public MainController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/")
    public String index(Model model, HttpServletRequest request) {
        String token = getJwtFromCookie(request);
        
        if (token != null && jwtUtil.validateToken(token) && !jwtUtil.isTokenExpired(token)) {
            // JWT에서 사용자 정보 추출
            String email = jwtUtil.getEmailFromToken(token);
            String name = jwtUtil.getNameFromToken(token);
            String picture = jwtUtil.getPictureFromToken(token);
            
            model.addAttribute("email", email);
            model.addAttribute("name", name);
            model.addAttribute("picture", picture);
            model.addAttribute("loggedIn", true);
        } else {
            model.addAttribute("loggedIn", false);
        }
        
        return "index";
    }
 
    private String getJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT-TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}