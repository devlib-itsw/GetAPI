package com.getapi.auth.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getapi.auth.domain.ApiAuth;
import com.getapi.auth.dto.ApiKeyResponse;
import com.getapi.auth.service.ApiAuthService;
import com.getapi.auth.util.SecureUtil;
import com.getapi.user.domain.Users;
import com.getapi.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ApiAuthController {

    private final ApiAuthService apiAuthService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @PostMapping("/api-key")
    public ResponseEntity<?> issueApiKey(
            @RequestHeader("X-GetAPI-Timestamp") String timestamp,
            @RequestHeader("X-GetAPI-Signature") String signature,
            HttpServletRequest request) throws IOException {

        byte[] body = request.getInputStream().readAllBytes();

        // 1. rotation_token 파싱
        Map<String, String> parsed = objectMapper.readValue(body, Map.class);
        String rotationToken = parsed.get("rotation_token");

        if (rotationToken == null || rotationToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("rotation_token is required");
        }

        // 2. rotation_token으로 ApiAuth 조회
        ApiAuth apiAuth = apiAuthService.findByRotationToken(rotationToken);

        // 3. 타임스탬프 + 서명 검증
        SecureUtil.validateRequest(apiAuth.getSecretKey(), body, timestamp, signature);

        // 4. api_key + rotation_token 갱신
        ApiKeyResponse response = apiAuthService.renewByRotationToken(apiAuth);
        return ResponseEntity.ok(response);
    }

    // API Key 발급/재발급
    @PostMapping("/apiKey/{id}")
    @ResponseBody
    public void issueApiKey(@PathVariable("id") UUID uuid) {
        Users user = userService.getUserByUUID(uuid);
        List<Users> users = new ArrayList<>();
        users.add(user);
        apiAuthService.issueApiKey(users);
    }

    // Secret Key 발급/재발급
    @PostMapping("/secretKey/{id}")
    @ResponseBody
    public String issueSecretKey(@PathVariable("id") UUID uuid) {
        Users user = userService.getUserByUUID(uuid);
        String key = SecureUtil.generate64Token();
        apiAuthService.issueSecretKey(user, key);
        return key;
    }

    // API Key 만료일 조회
    @GetMapping("/apiUpdatedAt/{id}")
    @ResponseBody
    public LocalDateTime getApiExpiryDate(@PathVariable("id") UUID uuid) {
        Users user = userService.getUserByUUID(uuid);
        return apiAuthService.getApiExpiryDate(user);
    }

    // Rotation Token 만료일 조회
    @GetMapping("/rotationTokenExpiry/{id}")
    @ResponseBody
    public LocalDateTime getRotationTokenExpiryDate(@PathVariable("id") UUID uuid) {
        Users user = userService.getUserByUUID(uuid);
        return apiAuthService.getRotationTokenExpiryDate(user);
    }
}
