package com.getapi.api.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 2592000) // 30일
public class ApiAuthToken {
 @Id
 private String token; // Redis Key
 
 private String id;
 private String ip;
 private String userAgent;
}