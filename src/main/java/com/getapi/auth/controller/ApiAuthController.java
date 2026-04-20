package com.getapi.auth.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getapi.auth.domain.ApiAuth;
import com.getapi.auth.dto.ApiKeyResponse;
import com.getapi.auth.service.ApiAuthService;
import com.getapi.auth.util.SecureUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ApiAuthController {

    private final ApiAuthService apiAuthService;
    private final ObjectMapper objectMapper;

    @PostMapping("/api-key")
    public ResponseEntity<?> issueApiKey(
            @RequestHeader("X-GetAPI-Timestamp") String timestamp,
            @RequestHeader("X-GetAPI-Signature") String signature,
            HttpServletRequest request) throws IOException {

        // raw body 읽기 (HMAC 검증에 원본 bytes 필요)
        byte[] body = request.getInputStream().readAllBytes();

        // 1. 타임스탬프 검증
        if (!SecureUtil.isTimestampValid(timestamp)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Timestamp expired");
        }

        // 2. rotation_token 파싱
        Map<String, String> parsed = objectMapper.readValue(body, Map.class);
        String rotationToken = parsed.get("rotation_token");

        if (rotationToken == null || rotationToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("rotation_token is required");
        }

        // 3. rotation_token으로 ApiAuth 조회
        ApiAuth apiAuth = apiAuthService.findByRotationToken(rotationToken);

        // 4. HMAC 서명 검증
        if (!SecureUtil.verifyHmac(apiAuth.getSecretKey(), body, timestamp, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        // 5. 응답 반환
        ApiKeyResponse response = apiAuthService.toResponse(apiAuth);
        return ResponseEntity.ok(response);
    }
}
