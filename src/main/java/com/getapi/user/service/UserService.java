package com.getapi.user.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.admin.domain.AdminCensoredResponse;

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

	public List<Users> getUsers() {
		return this.userRepository.findAll();
	}

	private final UserProfileRepository userProfileRepository;
	private final ApiAuthRepository apiAuthRepository;

	public Users getUserByUUID(UUID uuid) {
		Users user = this.userRepository.findByUserUuid(uuid);

		// null일 상황에 맞춰 에러페이지 제작
		return user;
	}

	public List<Users> getUsersBeforeApiUpdatedAt() {
		LocalDateTime limit = LocalDateTime.now().minusDays(90).with(LocalTime.MAX);
		return this.apiAuthRepository.findByApiUpdatedAtBefore(limit).stream().map(ApiAuth::getUser)
				.collect(Collectors.toList());
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
		LocalDateTime limit = LocalDateTime.now().minusDays(90);
		
		this.userProfileRepository.deleteByUserProfileDeletedAtBefore(limit);
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
		for (Users user : users) {
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
		return apiAuthRepository.findByUser(user).map(a -> a.getApiUpdatedAt().plusDays(90)).orElse(null);
	}

	// 관리자 페이지 페이지네이션
	/*
	 * public Page<Users> getUsersByPage(int page){ Pageable
	 * pageable=PageRequest.of(page, 10, Sort.by("userId").descending());
	 * 
	 * return this.userRepository.findAll(pageable); }
	 */

	public Page<Users> getUsersBySearchPage(int page, String keyword) {
		Pageable pageable = PageRequest.of(page, 10, Sort.by("userId").descending());

		if (keyword == null || keyword.trim().isEmpty()) {
			return this.userRepository.findAll(pageable);
		}

		/*
		 * return
		 * this.userRepository.findByNameContainingOrNicknameContainingOrEmailContaining
		 * (keyword, keyword, keyword, pageable);
		 */
		return this.userRepository.findByKeyword(keyword, pageable);
	}

	public long totalUsers() {
		return this.userRepository.count();
	}

	public Page<AdminCensoredResponse> getUsersByIsCensoredPage(int page) {
		Pageable pageable = PageRequest.of(page, 10, Sort.by("userId").descending());
		Page<Users> list = this.userRepository.findByIsCensoredTrue(pageable);
		List<Users> users = list.getContent();
		List<UserProfile> profileList = userProfileRepository.findByUserIn(users);
		Map<Long, UserProfile> profileMap = profileList.stream()
				.collect(Collectors.toMap(p -> p.getUser().getUserId(), p -> p));

		Page<AdminCensoredResponse> dtolist = list.map(user -> {
			// 1. 미리 만들어둔 Map에서 이 유저의 프로필을 찾습니다.
			UserProfile profile = profileMap.get(user.getUserId());

			// 2. 프로필이 없을 경우를 대비해 안전하게 처리합니다.
			String displayName = "이름없음"; // 기본값
			if (profile != null) {
				// 프로필이 있다면 닉네임 -> 이름 순으로 가져옵니다.
				displayName = Optional.ofNullable(profile.getNickname()).orElse(profile.getName());
			}

			// 3. 반드시 'return'을 써서 객체를 반환해야 합니다.
			return new AdminCensoredResponse(user.getUserId(), displayName, // 위에서 추출한 이름을 넣습니다.
					user.getEmail(), user.getUserUuid().toString(), user.getProfileCreatedAt(), user);
		});

		return dtolist;
	}

	@Transactional
	public void ignore(UUID uuid) {
		Users user = this.userRepository.findByUserUuid(uuid);
		UserProfile userProfile = this.userProfileRepository.findByUser(user).orElseThrow(() -> new RuntimeException("해당 유저의 프로필을 찾을 수 없습니다."));
		userProfile.setCensored(false);
	}

	@Transactional
	public void delete(UUID uuid) {
		this.userProfileRepository.deleteByUserUuidAndIsCensoredTrue(uuid);
		this.userRepository.deleteByUserUuid(uuid);
	}
}
