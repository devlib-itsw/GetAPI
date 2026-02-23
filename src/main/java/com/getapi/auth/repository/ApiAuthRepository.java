package com.getapi.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.getapi.auth.domain.ApiAuth;
import com.getapi.user.domain.Users;

public interface ApiAuthRepository extends JpaRepository<ApiAuth, Long> {
    Optional<ApiAuth> findByRotationToken(String rotationToken);
    Optional<ApiAuth> findByUser(Users user);
    List<ApiAuth> findByApiUpdatedAtBefore(LocalDateTime limit);
}
