package com.getapi.payments.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.getapi.payments.client.PaymentsAuthInterceptor;

@Configuration
public class PaymentConfig {
    @Bean
    public PaymentsAuthInterceptor paymentAuthInterceptor(PaymentProperties paymentProperties) {
        return new PaymentsAuthInterceptor(paymentProperties);
    }
}