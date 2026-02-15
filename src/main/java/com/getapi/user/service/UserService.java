package com.getapi.user.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	
	public Users getUserByUUID(UUID uuid) {
		Users user=this.userRepository.findByUserUuid(uuid);
		
		// null일 상황에 맞춰 에러페이지 제작
		return user;
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
}
