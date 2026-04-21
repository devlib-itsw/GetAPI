package com.getapi.auth.filter;

import com.getapi.auth.domain.RefreshToken;
import com.getapi.auth.repository.RefreshTokenRepository;
import com.getapi.auth.service.RefreshTokenService;
import com.getapi.auth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository, RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String jwtToken = getCookie(request, "JWT-TOKEN");
        String refreshToken = getCookie(request, "REFRESH-TOKEN");

        if (jwtToken != null && jwtUtil.validateToken(jwtToken) && !jwtUtil.isTokenExpired(jwtToken)) {
            String sub = jwtUtil.getSubFromToken(jwtToken);
            String role = jwtUtil.getRoleFromToken(jwtToken);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(sub, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (refreshToken != null) {
            RefreshToken old = refreshTokenRepository.findById(refreshToken).orElse(null);
            if (old == null) {
                filterChain.doFilter(request, response);
                return;
            }
            RefreshToken rt = refreshTokenService.rotate(refreshToken, request.getRemoteAddr(), request.getHeader("User-Agent"), old.getRole());

            // 새 액세스 토큰 쿠키
            Cookie accessCookie = new Cookie("JWT-TOKEN", jwtUtil.generateToken(rt.getId(), rt.getRole()));
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(1800);
            response.addCookie(accessCookie);

            // 새 리프레시 토큰 쿠키
            Cookie refreshCookie = new Cookie("REFRESH-TOKEN", rt.getToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(2592000);
            response.addCookie(refreshCookie);

            // 인증
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rt.getRole()));
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(rt.getId(), null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }

    private String getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
