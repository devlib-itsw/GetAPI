package com.getapi.payments;

import com.getapi.payments.PaymentConfig;
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
