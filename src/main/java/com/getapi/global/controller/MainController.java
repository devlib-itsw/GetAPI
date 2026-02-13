package com.getapi.global.controller;

import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private final UserRepository userRepository;

    public MainController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String sub = (String) authentication.getPrincipal();
            Users user = userRepository.findByProviderId(sub);

            if (user != null) {
                model.addAttribute("email", user.getEmail());
                model.addAttribute("name", user.getName());
                model.addAttribute("picture", user.getProfileImage());
                model.addAttribute("loggedIn", true);
            } else {
                // 인증은 됐으나 DB에 없는 경우 (보통 로그아웃 시키거나 추가 정보 입력 페이지로 이동)
                model.addAttribute("loggedIn", false);
                // 필요하다면 여기서 SecurityContextHolder.clearContext() 후 리다이렉트
            }
        } else {
            model.addAttribute("loggedIn", false);
        }

        return "index";
    }
}
