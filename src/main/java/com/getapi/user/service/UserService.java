package com.getapi.user.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.UserUpdateDTO;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;
import com.getapi.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;

	public Users getUserByUUID(UUID uuid) {
		return this.userRepository.findByUserUuid(uuid);
	}

	// 사용자 요청으로 통한 임시 삭제
	public void softDelete(Users user) {
		user.setProfileDeletedAt(LocalDateTime.now());
		this.userRepository.save(user);
	}

	// scheduler를 통한 일괄 삭제
	public void hardDelete() {
		LocalDateTime limit=LocalDateTime.now().minusDays(90);
		this.userRepository.deleteByProfileDeletedAtBefore(limit);
	}

	// 유저 정보 수정
	public void updateUser(Users user, UserUpdateDTO dto) {
		UserProfile profile = userProfileRepository.findByUser(user)
				.orElseThrow(() -> new RuntimeException("UserProfile not found"));
		profile.setNickname(dto.getNickname().isEmpty() ? null : dto.getNickname());
		profile.setIntroduction(dto.getIntroduction().isEmpty() ? null : dto.getIntroduction());
		profile.setWebUrl(dto.getWebsite().isEmpty() ? null : dto.getWebsite());
		userProfileRepository.save(profile);
	}
}
