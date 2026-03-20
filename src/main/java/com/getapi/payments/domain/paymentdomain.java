package com.getapi.payments.domain;

import org.springframework.data.annotation.Id; // 주의: JPA @Id가 아닌 Spring Data @Id
import org.springframework.data.redis.core.RedisHash;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "payment", timeToLive = 330) // 5분 30초 후 자동 삭제
public class paymentdomain {
    @Id 
    private String orderId;    // Redis의 Key가 됩니다.
    private String userEmail;
    private Long amount;
    private boolean isPaid = false;
}