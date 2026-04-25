package com.getapi.user.service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;
import com.getapi.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImgService {
	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;
	private final RedisTemplate<String, byte[]> byteRedisTemplate;
	private final RestTemplate restTemplate;

	public byte[] getOrFetch(UUID uuid) {
		String key = "avatar:" + uuid;

		// 1. Redis 캐시 확인
		byte[] cached = byteRedisTemplate.opsForValue().get(key);
		if (cached != null) return cached;

		// 2. DB에서 프로필 이미지 URL 조회
		Users user = userRepository.findByUserUuid(uuid);
		if (user == null) return null;
		UserProfile profile = userProfileRepository.findByUser(user)
				.orElse(null);
		if (profile == null) return null;
		String url = profile.getProfileImage();

		if (url == null || url.isBlank()) return null;

		// 3. 구글에서 이미지 fetch
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("User-Agent", "Mozilla/5.0");
			HttpEntity<Void> entity = new HttpEntity<>(headers);

			ResponseEntity<byte[]> response = restTemplate.exchange(
					url, HttpMethod.GET, entity, byte[].class);
			byte[] image = response.getBody();
			if (image != null) {
				byteRedisTemplate.opsForValue().set(key, image, 6, TimeUnit.HOURS);
			}
			return image;
		} catch (RestClientException e) {
			log.warn("프로필 이미지 fetch 실패 (uuid={}): {}", uuid, e.getMessage());
			return null;
		}
	}
}
