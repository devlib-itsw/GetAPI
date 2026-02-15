package com.getapi.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private static final long EXPIRATION_TIME = 1800000; // 30분 (밀리초)

    private SecretKey key;

    @jakarta.annotation.PostConstruct
    private void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // JWT 토큰 생성
    public String generateToken(String sub) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", sub);

        return Jwts.builder()
                .claims(claims)
                .subject(sub)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    // JWT 토큰에서 이메일 추출
    public String getSubFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // JWT 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // JWT 토큰에서 Claims 추출
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 토큰 만료 확인
    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }
}