package com.getapi.user.repository;

import com.getapi.user.domain.Users;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {
	boolean existsByEmail(String email);
	boolean existsByProviderId(String providerId);
	Users findByProviderId(String providerId);
	Users findByUserUuid(UUID uuid);
	// deleted_at 시간 기준으로 지난 날짜 사용자 일괄 삭제
	void deleteByProfileDeletedAt(LocalDateTime limit);
}
