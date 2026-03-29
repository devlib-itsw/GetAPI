package com.getapi.payments.service;

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

@Service
@RequiredArgsConstructor
public class PaymentsService {
    private final PaymentClient paymentClient;
    private final UserRepository userRepository;
    
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
                } else {
                    System.out.println("에러: DB에서 유저를 찾을 수 없습니다. (Email: " + email + ")");
                }
            } else {
                System.out.println("에러: 로그인 정보를 찾을 수 없습니다.");
            }
        }
        return response;
    }
}
