package com.getapi.payments.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.getapi.payments.config.PaymentProperties;

public class PaymentsAuthInterceptor implements RequestInterceptor {
    private static final String AUTH_HEADER_PREFIX = "Basic ";
    private final PaymentProperties paymentProperties;

    public PaymentsAuthInterceptor(final PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
    }

    @Override
    public void apply(final RequestTemplate template) {
        final String authHeader = createPaymentAuthorizationHeader();
        template.header("Authorization", authHeader);
    }

    private String createPaymentAuthorizationHeader() {
        // secretKey 뒤에 콜론(:)을 붙여서 인코딩해야 토스 API가 인식합니다.
        final String auth = paymentProperties.getSecretKey() + ":";
        final byte[] encodedBytes = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        return AUTH_HEADER_PREFIX + new String(encodedBytes);
    }
}