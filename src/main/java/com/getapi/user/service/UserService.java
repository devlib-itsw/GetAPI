package com.getapi.user.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.admin.domain.AdminCensoredResponse;
import com.getapi.auth.util.SecureUtil;
import com.getapi.user.domain.UserUpdateDTO;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;
	
	public List<Users> getUsers(){
		return this.userRepository.findAll();
	}
	
	public Users getUserByUUID(UUID uuid) {
		Users user=this.userRepository.findByUserUuid(uuid);
		
		// null일 상황에 맞춰 에러페이지 제작
		return user;
	}
	
	public List<Users> getUsersBeforeApiUpdatedAt(){
		LocalDateTime limit=LocalDateTime.now().minusDays(90).with(LocalTime.MAX);
		return this.userRepository.findByApiUpdatedAtBefore(limit);
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
	
	// 관리자 페이지 페이지네이션
	/*
	 * public Page<Users> getUsersByPage(int page){ Pageable
	 * pageable=PageRequest.of(page, 10, Sort.by("userId").descending());
	 * 
	 * return this.userRepository.findAll(pageable); }
	 */
	
	public Page<Users> getUsersBySearchPage(int page, String keyword){
		Pageable pageable=PageRequest.of(page, 10, Sort.by("userId").descending());
		
		if(keyword==null || keyword.trim().isEmpty()) {
			return this.userRepository.findAll(pageable);
		}
		
		return this.userRepository.findByNameContainingOrNicknameContainingOrEmailContaining(keyword, keyword, keyword, pageable);
	}
	
	public long totalUsers() {
		return this.userRepository.count();
	}
	
	public Page<AdminCensoredResponse> getUsersByIsCensoredPage(int page){
		Pageable pageable=PageRequest.of(page, 10, Sort.by("userId").descending());
		
		Page<Users> list=this.userRepository.findByIsCensoredTrue(pageable);
		
		Page<AdminCensoredResponse> dtolist=list.map(user->new AdminCensoredResponse(
			user.getUserId(),
			Optional.ofNullable(user.getNickname()).orElse(user.getName()),
			user.getEmail(),
			user.getUserUuid().toString(),
			user.getProfileCreatedAt(),
			user
		));
		
		return dtolist;
	}
	
	@Transactional
	public void ignore(UUID uuid) {
		Users user=this.userRepository.findByUserUuid(uuid);
		if(user!=null) {
			user.setCensored(false);
		}
	}
	
	@Transactional
	public void delete(UUID uuid) {
		this.userRepository.deleteByUserUuidAndIsCensoredTrue(uuid);
	}
}
