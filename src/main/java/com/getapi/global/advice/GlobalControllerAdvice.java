package com.getapi.global.advice;

import com.getapi.auth.repository.ApiAuthRepository;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;
import com.getapi.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ApiAuthRepository apiAuthRepository;

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(SecurityException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e, HttpServletRequest request, Model model) {
        // REST 요청(Accept: application/json)은 기존 방식 유지
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            ModelAndView mav = new ModelAndView();
            mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            return mav;
        }
        log.error("Unhandled exception [{}] {}", request.getMethod(), request.getRequestURI(), e);
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", 500);
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        try {
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
        } catch (Exception e) {
            log.warn("addGlobalAttributes failed: {}", e.getMessage());
            model.addAttribute("loggedIn", false);
        }
    }
}