package com.getapi.user.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
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
	@Transactional // 데이터 수정을 위해 필수! 이시우
	public void updateUserInfo(UUID uuid, String nickname, String introduction, String webUrl) {
	    // 1. 기존 유저 조회
	    Users user = this.userRepository.findByUserUuid(uuid);
	    
	    if (user != null) {
	        // 2. 엔티티 내부 메서드로 값 변경
	        user.updateMyPage(nickname, introduction, webUrl);
	        
	        // 3. JPA의 Dirty Checking 덕분에 save()를 명시적으로 안 써도 되지만, 
	        // 기존 스타일을 유지하신다면 아래 코드를 남겨두셔도 됩니다.
	        this.userRepository.save(user);
	    }
	}


}
