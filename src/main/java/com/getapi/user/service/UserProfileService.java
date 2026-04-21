package com.getapi.user.service;

import org.springframework.stereotype.Service;

import com.getapi.user.domain.UserProfile;
import com.getapi.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserProfileService {
	private final UserProfileRepository userProfileRepository;
	
	public UserProfile findByUserId(Long userId) {
        return userProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }
}
