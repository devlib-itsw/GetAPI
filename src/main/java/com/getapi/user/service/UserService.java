package com.getapi.user.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.getapi.auth.domain.ApiAuth;
import com.getapi.auth.repository.ApiAuthRepository;
import com.getapi.auth.util.SecureUtil;
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
	private final ApiAuthRepository apiAuthRepository;

	public Users getUserByUUID(UUID uuid) {
		Users user=this.userRepository.findByUserUuid(uuid);

		// null일 상황에 맞춰 에러페이지 제작
		return user;
	}

	public List<Users> getUsersBeforeApiUpdatedAt(){
		LocalDateTime limit=LocalDateTime.now().minusDays(90).with(LocalTime.MAX);
		return this.apiAuthRepository.findByApiUpdatedAtBefore(limit)
				.stream().map(ApiAuth::getUser).collect(Collectors.toList());
	}

	public String get64Token() {
		return SecureUtil.generate64Token();
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

	// 유저 api key 할당
	public void setApiKey(List<Users> users) {
		for(Users user : users) {
			ApiAuth apiAuth = apiAuthRepository.findByUser(user).orElse(new ApiAuth());
			apiAuth.setUser(user);
			apiAuth.setApiKey(SecureUtil.generate64Token());
			apiAuth.setRotationToken(SecureUtil.generate64Token());
			apiAuth.setApiUpdatedAt(LocalDateTime.now());
			apiAuth.setExpiredDate(LocalDate.now().plusDays(90));
			apiAuthRepository.save(apiAuth);
		}
	}

	// 유저 secret key 할당
	public void setSecretKey(Users user, String key) {
		ApiAuth apiAuth = apiAuthRepository.findByUser(user)
				.orElseThrow(() -> new RuntimeException("ApiAuth not found"));
		apiAuth.setSecretKey(key);
		apiAuthRepository.save(apiAuth);
	}

	// api 만료일 조회
	public LocalDateTime getApiExpiryDate(Users user) {
		return apiAuthRepository.findByUser(user)
				.map(a -> a.getApiUpdatedAt().plusDays(90))
				.orElse(null);
	}
}
