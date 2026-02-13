package com.getapi.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//domain/SmsAuth.java
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@RedisHash(value = "smsAuth", timeToLive = 330) // 5분 30초 (SMS 전송 지연 버퍼 30초 포함)
public class SmsAuth {
 @Id
 private String token; // Redis Key
 
 private String userId;
 private String userEmail;
 private String userName;
 private String userImg;
 private String phone;
}