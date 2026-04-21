package com.getapi.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(Users user);
    List<UserProfile> findByUserIn(List<Users> users);
    
    Optional<UserProfile> findByName(String name);
    
    Optional<UserProfile> findByUser_UserId(Long userId);
    
    Page<UserProfile> findByIsCensoredTrue(Pageable pageable);
    
    // Users의 userUuid를 통해 delete
    @Modifying
    @Transactional
    @Query("DELETE FROM UserProfile p WHERE p.user.userUuid=:uuid AND p.isCensored=true")
    void deleteByUserUuidAndIsCensoredTrue(@Param("uuid") UUID uuid);
    
    // Users의 ProfileDeletedAt를 통해 delete
    @Modifying
    @Transactional
    @Query("DELETE FROM UserProfile p WHERE p.user.profileDeletedAt<:limit")
    void deleteByUserProfileDeletedAtBefore(@Param("limit") LocalDateTime limit);
}
