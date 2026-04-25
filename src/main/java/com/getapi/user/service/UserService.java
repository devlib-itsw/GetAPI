package com.getapi.user.service;

import java.time.LocalDateTime;
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
import com.getapi.ai.service.AiCensorAsyncService;
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
	private final AiCensorAsyncService aiCensorAsyncService;

	public List<Users> getUsers() {
		return this.userRepository.findAll();
	}

	public Users getUserByUUID(UUID uuid) {
		return this.userRepository.findByUserUuid(uuid);
	}

	public Users getProviderId(String providerId) {
		return this.userRepository.findByProviderId(providerId);
	}

	public Users getByName(String name) {
		Optional<UserProfile> profile = this.userProfileRepository.findByName(name);
		UserProfile userProfile = profile.orElseThrow(() -> new RuntimeException("프로필을 찾을 수 없습니다."));
		return userProfile.getUser();
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

	// 유저 프로필 정보 수정 (UserProfile 기반)
	public void updateUser(Users user, UserUpdateDTO dto) {
		UserProfile profile = userProfileRepository.findByUser(user)
				.orElseThrow(() -> new RuntimeException("UserProfile not found"));

		profile.setNickname(dto.getNickname().isEmpty() ? null : dto.getNickname());
		profile.setIntroduction(dto.getIntroduction().isEmpty() ? null : dto.getIntroduction());
		profile.setWebUrl(dto.getWebsite().isEmpty() ? null : dto.getWebsite());
		userProfileRepository.save(profile);

		String checkText = (dto.getNickname() != null ? dto.getNickname() : "") + " "
				+ (dto.getIntroduction() != null ? dto.getIntroduction() : "");
		aiCensorAsyncService.checkUserProfile(profile.getProfileId(), checkText.trim());
	}

	// 관리자 페이지 - 유저 검색 페이지네이션
	public Page<Users> getUsersBySearchPage(int page, String keyword) {
		Pageable pageable = PageRequest.of(page, 10, Sort.by("userId").descending());
		if (keyword == null || keyword.trim().isEmpty()) {
			return this.userRepository.findAll(pageable);
		}
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

		return list.map(user -> {
			UserProfile profile = profileMap.get(user.getUserId());
			String displayName = "이름없음";
			if (profile != null) {
				displayName = Optional.ofNullable(profile.getNickname()).orElse(profile.getName());
			}
			return new AdminCensoredResponse(user.getUserId(), displayName,
					user.getEmail(), user.getUserUuid().toString(), user.getProfileCreatedAt(), user);
		});
	}

	public UserProfile getProfileByUserUuid(UUID uuid) {
		Users user = this.userRepository.findByUserUuid(uuid);
		return this.userProfileRepository.findByUser(user)
				.orElseThrow(() -> new RuntimeException("해당 유저의 프로필을 찾을 수 없습니다."));
	}

	@Transactional
	public void ignore(UUID uuid) {
		Users user = this.userRepository.findByUserUuid(uuid);
		UserProfile userProfile = this.userProfileRepository.findByUser(user)
				.orElseThrow(() -> new RuntimeException("해당 유저의 프로필을 찾을 수 없습니다."));
		userProfile.setCensored(false);
		this.userProfileRepository.save(userProfile);
	}

	@Transactional
	public void delete(UUID uuid) {
		Users user = this.userRepository.findByUserUuid(uuid);
		UserProfile profile = this.userProfileRepository.findByUser(user)
				.orElseThrow(() -> new RuntimeException("해당 유저의 프로필을 찾을 수 없습니다."));
		profile.setNickname("검열됨");
		profile.setIntroduction("검열됨");
		profile.setWebUrl(null);
		this.userProfileRepository.save(profile);
	}
}
