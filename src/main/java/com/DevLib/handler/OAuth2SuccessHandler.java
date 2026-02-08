package com.DevLib.handler;

import com.DevLib.util.JwtUtil;
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

    public OAuth2SuccessHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // JWT 토큰 생성
        String token = jwtUtil.generateToken(email, name, picture);

        // 쿠키에 JWT 저장
        Cookie cookie = new Cookie("JWT-TOKEN", token);
        cookie.setHttpOnly(true);  // JavaScript에서 접근 불가 (보안)
        cookie.setPath("/");
        cookie.setMaxAge(86400);   // 24시간
        response.addCookie(cookie);

        // 홈으로 리디렉션
        getRedirectStrategy().sendRedirect(request, response, "/");
    }
}