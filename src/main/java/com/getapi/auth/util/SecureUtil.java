package com.getapi.auth.util;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HexFormat;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class SecureUtil {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generate64Token() {
    	byte[] values = new byte[32]; // 256비트
        secureRandom.nextBytes(values);
        // 16진수 문자열로 변환 (좌측 0 패딩 처리)
        String hex = new BigInteger(1, values).toString(16);
        return String.format("%64s", hex).replace(' ', '0');
    }
    
    // 쿠키 삭제 편의 메서드
    public static void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
