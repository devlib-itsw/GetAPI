package com.getapi.auth.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiKeyResponse {

    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("expired_date")
    private LocalDate expiredDate;

    @JsonProperty("rotation_token")
    private String rotationToken;
}
