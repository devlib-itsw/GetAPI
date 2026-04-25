package com.getapi.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.getapi.ai.domain.AiFeedback;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {
}
