package com.getapi.user.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.user.domain.Users;

public interface UserRepository extends JpaRepository<Users, Long> {
	boolean existsByEmail(String email);
	boolean existsByProviderId(String providerId);
	Users findByProviderId(String providerId);
	Users findByUserUuid(UUID uuid);
	Optional<Users> findByName(String name);
	void deleteByProfileDeletedAt(LocalDateTime limit);
	// deleted_at 시간 기준으로 지난 날짜 사용자 일괄 삭제
	@Transactional
	void deleteByProfileDeletedAtBefore(LocalDateTime limit);
}
