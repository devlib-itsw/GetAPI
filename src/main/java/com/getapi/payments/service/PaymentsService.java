package com.getapi.payments.service;

// <<<<<<< siwoo
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.getapi.payments.client.PaymentClient;
import com.getapi.payments.domain.paymentdomain;
import com.getapi.payments.dto.PaymentsConfirmRequest;
import com.getapi.payments.dto.PaymentsConfirmResponse;
import com.getapi.payments.repository.PaymentsRepository;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import com.getapi.payments.domain.PaymentHistory;
import com.getapi.payments.repository.PaymentHistoryRepository;
import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class PaymentsService {
    private final PaymentClient paymentClient;
    private final UserRepository userRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PaymentsRepository paymentsRepository;
    
    
    
    @Transactional
    public PaymentsConfirmResponse confirmPayment(PaymentsConfirmRequest requestDto) {
        // 1. 토스 서버에 결제 승인 요청 (인터셉터가 인증 처리)
        PaymentsConfirmResponse response = paymentClient.confirmPayment(requestDto);
        
        // 2. 결제가 성공(DONE)했을 때만 포인트 업데이트
        if ("DONE".equals(response.getStatus())) {
            
            // [수정 핵심] OAuth2로 로그인한 유저 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (true) {
//                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                
                // 구글 로그인 시 보통 "email" 키값에 사용자 이메일이 들어있습니다.
            	paymentdomain payment = paymentsRepository.findByOrderId(requestDto.getOrderId())
                        .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));
                String email = payment.getUserEmail();
             
                
                // 3. DB에서 해당 이메일을 가진 유저 찾기
                Users user = userRepository.findByProviderId(email);

                if (user != null) {
                    // 4. 포인트 충전 (기존 포인트 + 결제 금액)
                	long amount = response.getTotalAmount();
                    long currentPoint = (user.getPoint() == null) ? 0L : user.getPoint();
                    
                    user.setPoint(currentPoint + amount);
                    
                    userRepository.save(user);
                    
                    System.out.println("트랜잭션 내 포인트 변경 완료: " + user.getPoint());
                    
                    PaymentHistory history = PaymentHistory.builder()  // 4월 11일 충전 내역
                    	    .orderId(requestDto.getOrderId())
                    	    .userEmail(email)
                    	    .amount(amount)
                    	    .status(response.getStatus())
                    	    .orderName(response.getOrderName()) // ← PaymentsConfirmResponse에 있으면
                    	    .paidAt(LocalDateTime.now())
                    	    .build();
                    	paymentHistoryRepository.save(history);
                } else {
                    System.out.println("에러: DB에서 유저를 찾을 수 없습니다. (Email: " + email + ")");
                }
            } else {
                System.out.println("에러: 로그인 정보를 찾을 수 없습니다.");
            }
        }
        return response;
    }
    @Transactional
    public org.springframework.http.ResponseEntity<?> useApi(String userEmail) { // 차감 4월 11일
        // 1. DB에서 유저 조회
        Users user = userRepository.findByProviderId(userEmail);
        
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
        }

        // 2. 포인트 부족 시 즉시 차단 (Guard Clause)
        if (user.getPoint() == null || user.getPoint() < 1) {
            return org.springframework.http.ResponseEntity.status(403).body("포인트가 부족합니다. 충전 후 이용해주세요.");
        }

        // 3. 포인트 차감 (1P씩)
        user.setPoint(user.getPoint() - 1);
        userRepository.save(user); // JPA Dirty Checking으로 자동 저장되지만 명시적으로 호출
        
        return org.springframework.http.ResponseEntity.ok("API 호출 성공! 남은 포인트: " + user.getPoint());
    }
    
    
   
// =======
import org.springframework.stereotype.Service;

import com.getapi.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentsService {
	private final UserRepository userRepository;
	
	public Long totalPoints() {
		return this.userRepository.sumPointsByProfileDeletedAtIsNotNull();
	}
// >>>>>>> develop
}
