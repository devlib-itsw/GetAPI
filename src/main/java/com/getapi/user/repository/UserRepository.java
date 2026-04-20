package com.getapi.user.repository;

import com.getapi.user.domain.Users;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<Users, Long> {
	boolean existsByEmail(String email);

	boolean existsByProviderId(String providerId);

	Users findByProviderId(String providerId);

	Users findByUserUuid(UUID uuid);

	void deleteByProfileDeletedAt(LocalDateTime limit);

	// deleted_at 시간 기준으로 지난 날짜 사용자 일괄 삭제
	@Transactional
	void deleteByProfileDeletedAtBefore(LocalDateTime limit);

	// apiUpdatedAt 시간 기준으로 지난 날짜 사용자 선택
//	@Query("SELECT u FROM Users u JOIN UserProfile p ON u.userId=p.user.userId WHERE p.apiUpdatedAt<:limit")
	List<Users> findByApiUpdatedAtBefore(@Param("limit") LocalDateTime limit);

	Page<Users> findAll(Pageable pageable);

//	Page<Users> findByIsCensoredTrue(Pageable pageable);

	// 전체 포인트
	@Query("SELECT SUM(p.point) FROM UserProfile p JOIN Users u ON p.profileId=u.userId WHERE u.profileDeletedAt IS NULL")
	Long sumPointsByProfileDeletedAtIsNotNull();

	// uuid를 활요해서 delete
	@Modifying
	@Transactional
	void deleteByUserUuid(UUID uuid);
	
	// 수정 전 유저 검색
//	@Query("SELECT u FROM Users u JOIN UserProfile p ON u.userId = p.user.userId "
//	+ "WHERE p.name LIKE %:kw% OR p.nickname LIKE %:kw% OR u.email LIKE %:kw%")
//	Page<Users> findByKeyword(@Param("kw") String kw, Pageable pageable);
	
	// 수정 후 유저 검색
	@Query("SELECT u FROM Users u JOIN UserProfile p ON u.userId=p.user.userId WHERE p.name LIKE %:kw% OR p.nickname LIKE %:kw% OR u.email LIKE %:kw%")
	Page<Users> findByKeyword(@Param("kw") String kw, Pageable pageable);
	
	// 검열된 사용자들 반환
	@Query("SELECT u FROM Users u JOIN UserProfile p ON u.userId=p.user.userId WHERE p.isCensored=true")
	Page<Users> findByIsCensoredTrue(Pageable pageable);
}
