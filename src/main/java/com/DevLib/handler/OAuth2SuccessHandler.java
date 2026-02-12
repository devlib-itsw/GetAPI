package com.DevLib.handler;

import com.DevLib.service.SmsAuthService;
import com.DevLib.util.JwtUtil;
import com.DevLib.util.SecureUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final SmsAuthService smsAuthService;

    public OAuth2SuccessHandler(JwtUtil jwtUtil, SmsAuthService smsAuthService) {
        this.jwtUtil = jwtUtil;
		this.smsAuthService = smsAuthService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture"); 
        String SecureToken = SecureUtil.generate64Token();

        if (smsAuthService.existsByEmail(email)) {
        	// 기존 유저: 바로 JWT 로그인
        	String token = jwtUtil.generateToken(email, name, picture);

        	Cookie cookie = new Cookie("JWT-TOKEN", token);
        	cookie.setHttpOnly(true);
        	cookie.setPath("/");
        	cookie.setMaxAge(86400);
        	response.addCookie(cookie);

        	getRedirectStrategy().sendRedirect(request, response, "/");
        } else {
        	// 신규 유저: 번호인증 진행
        	smsAuthService.saveToken(SecureToken, email, name, picture);
        	getRedirectStrategy().sendRedirect(request, response, "/smsVerify?token=" + SecureToken);
        }
    }
}