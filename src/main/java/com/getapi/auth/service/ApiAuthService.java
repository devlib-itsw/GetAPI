package com.getapi.auth.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.getapi.auth.domain.ApiAuth;
import com.getapi.auth.dto.ApiKeyResponse;
import com.getapi.auth.repository.ApiAuthRepository;
import com.getapi.auth.util.SecureUtil;
import com.getapi.user.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiAuthService {

    private final ApiAuthRepository apiAuthRepository;

    public ApiAuth findByRotationToken(String rotationToken) {
        return apiAuthRepository.findByRotationToken(rotationToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 rotation_token입니다."));
    }

    public ApiKeyResponse toResponse(ApiAuth apiAuth) {
        return new ApiKeyResponse(
                apiAuth.getApiKey(),
                apiAuth.getExpiredDate(),
                apiAuth.getRotationToken()
        );
    }

    // Go 클라이언트 자동 갱신용 — api_key + rotation_token 모두 교체
    public ApiKeyResponse renewByRotationToken(ApiAuth apiAuth) {
        apiAuth.setApiKey(SecureUtil.generate64Token());
        apiAuth.setRotationToken(SecureUtil.generate64Token());
        apiAuth.setApiUpdatedAt(LocalDateTime.now());
        apiAuth.setRotationTokenUpdatedAt(LocalDateTime.now());
        apiAuth.setExpiredDate(LocalDate.now().plusDays(7));
        apiAuthRepository.save(apiAuth);
        return toResponse(apiAuth);
    }

    // api_key만 갱신 (브라우저/스케줄러 재발급용 — rotation_token 변경 없음)
    public void issueApiKey(List<Users> users) {
        for (Users user : users) {
            ApiAuth apiAuth = apiAuthRepository.findByUser(user).orElse(new ApiAuth());
            apiAuth.setUser(user);
            apiAuth.setApiKey(SecureUtil.generate64Token());
            apiAuth.setApiUpdatedAt(LocalDateTime.now());
            apiAuth.setExpiredDate(LocalDate.now().plusDays(7));
            apiAuthRepository.save(apiAuth);
        }
    }

    public void issueSecretKey(Users user, String key) {
        ApiAuth apiAuth = apiAuthRepository.findByUser(user).orElse(new ApiAuth());
        apiAuth.setUser(user);
        apiAuth.setSecretKey(key);
        apiAuthRepository.save(apiAuth);
    }

    public LocalDateTime getApiExpiryDate(Users user) {
        return apiAuthRepository.findByUser(user)
                .map(a -> a.getApiUpdatedAt().plusDays(7))
                .orElse(null);
    }

    public LocalDateTime getRotationTokenExpiryDate(Users user) {
        return apiAuthRepository.findByUser(user)
                .map(a -> a.getRotationTokenUpdatedAt() != null
                        ? a.getRotationTokenUpdatedAt().plusDays(90)
                        : null)
                .orElse(null);
    }

    public List<Users> getUsersBeforeApiUpdatedAt() {
        LocalDateTime limit = LocalDateTime.now().minusDays(90).with(LocalTime.MAX);
        return apiAuthRepository.findByApiUpdatedAtBefore(limit)
                .stream().map(ApiAuth::getUser).collect(Collectors.toList());
    }
}
