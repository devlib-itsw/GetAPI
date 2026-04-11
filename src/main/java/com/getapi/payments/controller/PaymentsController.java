package com.getapi.payments.controller;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.apache.catalina.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import com.getapi.auth.util.JwtUtil; // JwtUtil이 있는 실제 패키지 경로
import com.getapi.payments.domain.paymentdomain;
import com.getapi.payments.dto.PaymentsConfirmRequest;
import com.getapi.payments.dto.PaymentsConfirmResponse;
import com.getapi.payments.repository.PaymentHistoryRepository;
import com.getapi.payments.repository.PaymentsRepository;
import com.getapi.payments.service.PaymentsService;
import com.getapi.payments.domain.PaymentHistory;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentsController {
	private final PaymentsRepository paymentsRepository;
    private final PaymentsService paymentsService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PaymentHistoryRepository paymentHistoryRepository; //4월 11일
    
    @GetMapping("/confirm")
    public String confirm(PaymentsConfirmRequest request, Model model) {
        // [기존 로직] 토스 서버에 승인 요청을 보내서 결제를 확정함
        PaymentsConfirmResponse response = paymentsService.confirmPayment(request);
        
        
        // [데이터 전달] 마이페이지에서 보여줄 정보가 있다면 담아줌
        model.addAttribute("totalAmount", response.getTotalAmount());
        
        
        
        // [화면 이동] 이제 JSON 데이터가 아닌 "mypage.html" 파일을 보여줌
        return "redirect:/user/mypage?amount=" + response.getTotalAmount();
    }
    
    @ResponseBody
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody paymentdomain payment, HttpServletRequest request) {
        try {
        	// 1. 쿠키에서 JWT 추출 및 유저 정보 확인
        	Cookie[] cookies = request.getCookies();
            String userEmail = null;

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("JWT-TOKEN".equals(cookie.getName())) {
                        userEmail = jwtUtil.getSubFromToken(cookie.getValue());
                        break;
                    }
                }
            }

            if (userEmail == null) {
                return ResponseEntity.status(401).body(Map.of("message", "로그인이 만료되었습니다."));
            }

            // 2. 전달받은 객체에 유저 정보 세팅
            payment.setUserEmail(userEmail);
            payment.setPaid(false);

            // 3. Redis에 저장 (설정한 TTL 330초 작동)
            paymentsRepository.save(payment);
            System.out.println("성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공성공");

		} catch (Exception e) {
			System.out.println("에러!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1에러!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!빼애액!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println(e);
		}
        return ResponseEntity.ok(Map.of("status", "success", "orderId", payment.getOrderId()));
    }
    
    @ResponseBody
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(HttpServletRequest request) { //충전 내역 4월 11일 
        // JWT에서 이메일 추출
        Cookie[] cookies = request.getCookies();
        String userEmail = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT-TOKEN".equals(cookie.getName())) {
                    userEmail = jwtUtil.getSubFromToken(cookie.getValue());
                    break;
                }
            }
        }
        if (userEmail == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 만료되었습니다."));
        }

        List<PaymentHistory> histories = paymentHistoryRepository.findByUserEmailOrderByPaidAtDesc(userEmail);
        return ResponseEntity.ok(histories);
    }
    
    @ResponseBody
    @PostMapping("/use-api")
    public ResponseEntity<?> useApi(HttpServletRequest request) { // 차감 4월 11일
        // 1. 쿠키에서 JWT 추출 (기존 save, history 메서드와 동일한 로직)
        Cookie[] cookies = request.getCookies();
        String userEmail = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT-TOKEN".equals(cookie.getName())) {
                    userEmail = jwtUtil.getSubFromToken(cookie.getValue());
                    break;
                }
            }
        }

        // 2. 로그인 여부 확인
        if (userEmail == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 만료되었습니다."));
        }

        // 3. PaymentsService에 작성한 useApi 호출
        // 서비스에서 ResponseEntity를 리턴하므로 그대로 반환하면 됩니다.
        return paymentsService.useApi(userEmail);
    }
}





