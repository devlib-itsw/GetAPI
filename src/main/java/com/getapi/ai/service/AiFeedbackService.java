package com.getapi.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.getapi.ai.domain.AiFeedback;
import com.getapi.ai.repository.AiFeedbackRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final AiFeedbackRepository aiFeedbackRepository;

    /**
     * 관리자가 AI 판정을 무시(ignore)했을 때 호출 — false positive 기록.
     * adminLabel = "clean"
     */
    public void recordFalsePositive(String text, String contentType, String contentUuid, String detectedLabels) {
        aiFeedbackRepository.save(new AiFeedback(text, contentType, contentUuid, detectedLabels, "clean"));
    }

    /**
     * 관리자가 AI 판정에 동의(delete)했을 때 호출 — true positive 기록.
     * adminLabel = "confirmed"
     */
    public void recordConfirmed(String text, String contentType, String contentUuid, String detectedLabels) {
        aiFeedbackRepository.save(new AiFeedback(text, contentType, contentUuid, detectedLabels, "confirmed"));
    }

    public List<AiFeedback> getAll() {
        return aiFeedbackRepository.findAll();
    }
}
