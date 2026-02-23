package com.getapi.user.repository;

import java.util.Optional;

import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(Users user);
}
