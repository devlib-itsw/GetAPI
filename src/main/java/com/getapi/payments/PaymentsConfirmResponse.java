package com.getapi.payments;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentsConfirmResponse {
    private String mId;            // 상점 아이디
    private String orderId;        // 주문 번호
    private String orderName;      // 주문 명
    private String paymentKey;     // 결제 고유 키
    private String status;         // 결제 상태 (DONE, CANCELED 등)
    private String requestedAt;    // 결제 요청 시간
    private String approvedAt;     // 결제 승인 시간
    private Long totalAmount;      // 총 결제 금액
}