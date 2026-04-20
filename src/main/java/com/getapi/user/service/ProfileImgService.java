package com.getapi.user.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;
import com.getapi.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileImgService {
	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;
	private final RedisTemplate<String, byte[]> byteRedisTemplate;
	private final RestTemplate restTemplate;

	public byte[] getOrFetch(Long id) {
		String key = "avatar:" + id;
		String url = "https://lh3.googleusercontent.com/a/" + id;

		// 1. Redis 캐시 확인
		byte[] cached = byteRedisTemplate.opsForValue().get(key);
		if (cached != null) return cached;

		// 2. DB에서 프로필 이미지 URL 조회
//		Users user = userRepository.findById(userId)
//				.orElseThrow(() -> new RuntimeException("User not found"));
//		UserProfile profile = userProfileRepository.findByUser(user)
//				.orElseThrow(() -> new RuntimeException("UserProfile not found"));

		// 3. 구글에서 이미지 fetch
		HttpHeaders headers = new HttpHeaders();
		headers.set("Referer", "https://accounts.google.com");
		headers.set("User-Agent", "Mozilla/5.0");
		HttpEntity<Void> entity = new HttpEntity<>(headers);

		ResponseEntity<byte[]> response = restTemplate.exchange(
				url, HttpMethod.GET, entity, byte[].class);
		byte[] image = response.getBody();

		// 4. Redis에 캐싱 (1일)
		byteRedisTemplate.opsForValue().set(key, image, 7, TimeUnit.DAYS);

		return image;
	}
}
