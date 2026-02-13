package com.getapi.user.repository;

import com.getapi.user.domain.Users;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {
	boolean existsByEmail(String email);
	boolean existsByProviderId(String providerId);
	Users findByProviderId(String providerId);
}
