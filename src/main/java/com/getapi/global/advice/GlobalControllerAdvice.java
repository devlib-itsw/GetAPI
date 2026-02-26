package com.getapi.global.advice;

import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserRepository userRepository;

    // 모든 컨트롤러의 메서드가 실행되기 전에 이 메서드가 먼저 실행되어 Model에 값을 담습니다.
    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String sub = (String) authentication.getPrincipal();
            Users user = userRepository.findByProviderId(sub);

            if (user != null) {
                model.addAttribute("loginUser", user); // 유저 객체 통째로 담기
                model.addAttribute("loggedIn", true);
            } else {
                model.addAttribute("loggedIn", false);
            }
        } else {
            model.addAttribute("loggedIn", false);
        }
    }
}