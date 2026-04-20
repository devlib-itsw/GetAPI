package com.getapi.auth.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SecureUtil {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final long ALLOWED_SKEW_SECONDS = 300; // 5분
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public static String generate64Token() {
        byte[] values = new byte[32]; // 256비트
        secureRandom.nextBytes(values);
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

    /**
     * RFC3339 타임스탬프가 현재 시각과 ±5분 이내인지 검증
     */
    public static boolean isTimestampValid(String timestamp) {
        try {
            Instant requestTime = Instant.parse(timestamp);
            long diffSeconds = Math.abs(Duration.between(Instant.now(), requestTime).getSeconds());
            return diffSeconds <= ALLOWED_SKEW_SECONDS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * HMAC-SHA256 서명 검증
     * message = body_bytes + timestamp_bytes (구분자 없이 이어붙임)
     */
    public static boolean verifyHmac(String secretKey, byte[] body, String timestamp, String signature) {
        try {
            byte[] timestampBytes = timestamp.getBytes(StandardCharsets.UTF_8);
            byte[] message = new byte[body.length + timestampBytes.length];
            System.arraycopy(body, 0, message, 0, body.length);
            System.arraycopy(timestampBytes, 0, message, body.length, timestampBytes.length);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            String expected = HexFormat.of().formatHex(mac.doFinal(message));

            return expected.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
