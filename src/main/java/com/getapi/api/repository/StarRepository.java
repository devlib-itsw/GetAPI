package com.getapi.api.repository;

import com.getapi.api.domain.Api;
import com.getapi.api.domain.Star;
import com.getapi.user.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface StarRepository extends JpaRepository<Star, Long> {
    long countByApi(Api api);
    boolean existsByApiAndUserProfile(Api api, UserProfile userProfile);
    @Transactional
    void deleteByApiAndUserProfile(Api api, UserProfile userProfile);
    @Transactional
    void deleteByUserProfile(UserProfile userProfile);
    @Transactional
    void deleteByApi(Api api);
}
