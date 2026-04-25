package com.getapi.payments.client;

import com.getapi.payments.config.PaymentConfig;
import com.getapi.payments.dto.PaymentsConfirmRequest;
import com.getapi.payments.dto.PaymentsConfirmResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "tossPaymentClient", 
    url = "https://api.tosspayments.com/v1/payments", 
    configuration = PaymentConfig.class
)
public interface PaymentClient {
    @PostMapping("/confirm")
    PaymentsConfirmResponse confirmPayment(@RequestBody PaymentsConfirmRequest request);
}
