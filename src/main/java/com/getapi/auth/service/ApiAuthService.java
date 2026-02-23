package com.getapi.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.getapi.auth.domain.ApiAuth;
import com.getapi.auth.dto.ApiKeyResponse;
import com.getapi.auth.repository.ApiAuthRepository;

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
}
