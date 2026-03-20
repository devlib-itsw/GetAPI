//package com.getapi.payments;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//@Entity
//@Getter
//@NoArgsConstructor
//public class Payment { // 결제 정보를 저장할 장부
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//   
//    private String orderId;   // 상점에서 만든 주문번호
//    private String userEmail; // 이 주문을 한 유저 이메일
//    private Long amount;      // 결제할 금액
//    private boolean isPaid = false; // 결제 완료 여부
//
//    public Payment(String orderId, String userEmail, Long amount) {
//        this.orderId = orderId;
//        this.userEmail = userEmail;
//        this.amount = amount;
//    }
//}