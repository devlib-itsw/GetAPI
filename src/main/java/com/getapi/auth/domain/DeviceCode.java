package com.getapi.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.data.redis.core.RedisHash;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@RedisHash(value = "deviceCode", timeToLive = 600) // 10분
public class DeviceCode {

    @Id
    private String deviceCode; // Redis Key (Go 클라이언트 보유)

    @Indexed
    private String userCode;   // 사용자가 브라우저에 입력하는 코드 (XXXX-XXXX)

    private String status;     // pending / approved

    private Long userId;       // 승인 후 설정
}
