package com.getapi.global.advice;

import com.getapi.auth.repository.ApiAuthRepository;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;
import com.getapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ApiAuthRepository apiAuthRepository;

    // 모든 컨트롤러의 메서드가 실행되기 전에 이 메서드가 먼저 실행되어 Model에 값을 담습니다.
    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String sub = (String) authentication.getPrincipal();
            Users user = userRepository.findByProviderId(sub);

            if (user != null) {
                model.addAttribute("loginUser", user);
                userProfileRepository.findByUser(user).ifPresent(profile ->
                    model.addAttribute("loginProfile", profile));
                apiAuthRepository.findByUser(user).ifPresent(apiAuth ->
                    model.addAttribute("loginApiAuth", apiAuth));
                model.addAttribute("loggedIn", true);
            } else {
                model.addAttribute("loggedIn", false);
            }
        } else {
            model.addAttribute("loggedIn", false);
        }
    }
}