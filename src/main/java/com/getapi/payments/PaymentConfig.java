package com.getapi.payments;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfig {
    @Bean
    public PaymentsAuthInterceptor paymentAuthInterceptor(PaymentProperties paymentProperties) {
        return new PaymentsAuthInterceptor(paymentProperties);
    }
}