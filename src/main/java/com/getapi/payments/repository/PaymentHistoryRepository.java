package com.getapi.payments.repository;

import com.getapi.payments.domain.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {// 4월 11일 
    List<PaymentHistory> findByUserEmailOrderByPaidAtDesc(String userEmail);
}