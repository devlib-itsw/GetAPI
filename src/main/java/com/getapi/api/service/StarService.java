package com.getapi.api.service;

import com.getapi.api.domain.Api;
import com.getapi.api.domain.Star;
import com.getapi.api.repository.StarRepository;
import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StarService {

    private final StarRepository starRepository;
    private final UserProfileRepository userProfileRepository;

    public long countByApi(Api api) {
        return starRepository.countByApi(api);
    }

    public boolean isStarred(Api api, Users user) {
        return userProfileRepository.findByUser(user)
                .map(p -> starRepository.existsByApiAndUserProfile(api, p))
                .orElse(false);
    }

    @Transactional
    public boolean toggle(Api api, Users user) {
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("프로필이 없습니다."));
        if (starRepository.existsByApiAndUserProfile(api, profile)) {
            starRepository.deleteByApiAndUserProfile(api, profile);
            return false;
        }
        Star star = new Star();
        star.setApi(api);
        star.setUserProfile(profile);
        starRepository.save(star);
        return true;
    }
}
