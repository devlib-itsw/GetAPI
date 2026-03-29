package com.getapi.payments.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentsConfirmRequest {
    private String paymentKey; // 토스에서 발급한 결제 고유 키
    private String orderId;    // 상점에서 발급한 주문 번호
    private Long amount;       // 결제 금액
    
}