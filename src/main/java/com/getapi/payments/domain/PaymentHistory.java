package com.getapi.payments.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "payment_history")
public class PaymentHistory {// 4월 11일 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId;
    private String userEmail;
    private Long amount;
    private String status;       // DONE, FAILED 등
    private String orderName;    // 포인트 충전 (5000P)
    
    private LocalDateTime paidAt; // 결제 시각
}