package com.getapi.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.getapi.ai.dto.AiCheckRequest;
import com.getapi.ai.dto.AiCheckResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiFilterService {

    private static final Logger log = LoggerFactory.getLogger(AiFilterService.class);

    private final RestTemplate restTemplate;

    @Value("${ai.server.url:http://localhost:8888}")
    private String aiServerUrl;

    /**
     * AI 서버에 텍스트를 전송하여 혐오 여부를 판정합니다.
     * AI 서버가 다운되었을 경우 false(검열 안 함)를 반환합니다.
     */
    public AiCheckResponse check(String text) {
        try {
            return restTemplate.postForObject(
                aiServerUrl + "/check",
                new AiCheckRequest(text),
                AiCheckResponse.class
            );
        } catch (Exception e) {
            log.warn("AI 서버 호출 실패 — 검열 건너뜀: {}", e.getMessage());
            AiCheckResponse fallback = new AiCheckResponse();
            fallback.setCensored(false);
            return fallback;
        }
    }
}
