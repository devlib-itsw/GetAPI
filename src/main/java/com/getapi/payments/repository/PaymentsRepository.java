package com.getapi.payments.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.getapi.payments.domain.paymentdomain;

import java.util.Optional;

@Repository
public interface PaymentsRepository extends CrudRepository<paymentdomain, String> {
    // 기본 save, findById 등을 제공합니다.
    Optional<paymentdomain> findByOrderId(String orderId);
}