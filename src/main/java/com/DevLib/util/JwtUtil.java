package com.DevLib.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    // application.properties에서 설정할 비밀키
    private static final String SECRET_KEY = "9mK2pL8sQ4vR7nF3jH6xW1yT5uE0aZ9bC8dG4hJ7kN2mP6qS3vX1wY5zA8cF0eH3";
    private static final long EXPIRATION_TIME = 86400000; // 24시간 (밀리초)

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // JWT 토큰 생성
    public String generateToken(String email, String name, String picture) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("name", name);
        claims.put("picture", picture);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    // JWT 토큰에서 이메일 추출
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // JWT 토큰에서 이름 추출
    public String getNameFromToken(String token) {
        return getClaims(token).get("name", String.class);
    }

    // JWT 토큰에서 프로필 이미지 추출
    public String getPictureFromToken(String token) {
        return getClaims(token).get("picture", String.class);
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