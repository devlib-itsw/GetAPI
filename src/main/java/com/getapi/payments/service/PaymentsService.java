package com.getapi.payments.service;

import org.springframework.stereotype.Service;

import com.getapi.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentsService {
	private final UserRepository userRepository;
	
	public Long totalPoints() {
		return this.userRepository.sumPointsByProfileDeletedAtIsNotNull();
	}
}
