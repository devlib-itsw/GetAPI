package com.getapi.user.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.getapi.auth.util.SecureUtil;
import com.getapi.user.domain.UserUpdateDTO;
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
	
	public List<Users> getUsersBeforeApiUpdatedAt(){
		LocalDateTime limit=LocalDateTime.now().minusDays(90).with(LocalTime.MAX);
		return this.userRepository.findByApiUpdatedAtBefore(limit);
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
		user.setNickname(dto.getNickname().isEmpty() ? null : dto.getNickname());
		user.setIntroduction(dto.getIntroduction().isEmpty() ? null : dto.getIntroduction());
		user.setWebUrl(dto.getWebsite().isEmpty() ? null : dto.getWebsite());
		this.userRepository.save(user);
	}
	
	// 유저 api key 할당
	public void setApiKey(List<Users> users) {
		for(Users user : users) {			
			user.setApiKey(SecureUtil.generate64Token());
			user.setRotationToken(SecureUtil.generate64Token());
			user.setApiUpdatedAt(LocalDateTime.now());
			this.userRepository.save(user);
		}
	}
	
	// 유저 secret key 할당
	public void setSecretKey(Users user, String key) {	
		user.setSecretKey(key);
		this.userRepository.save(user);
	}
}
