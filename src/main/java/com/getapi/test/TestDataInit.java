package com.getapi.test;

import com.getapi.api.domain.Api;
import com.getapi.library.repository.ApiRepository;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TestDataInit implements CommandLineRunner {

    private final ApiRepository apiRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
    	
  
        if (apiRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        Users testUser = new Users();
        testUser.setUserUuid(UUID.randomUUID());
        testUser.setProviderId("test-provider-" + UUID.randomUUID());
        testUser.setEmail("admin@test.com");
        testUser.setPhone("01012345678");
        testUser.setName("testUser");
        testUser.setNickname("관리자");
        testUser.setIntroduction("테스트 관리자 계정");
        testUser.setWebUrl("https://example.com");
        testUser.setPoint(0L);
        testUser.setProfileImage("default-profile.png");
        testUser.setRole("USER");
        testUser.setProfileCreatedAt(now);

        userRepository.save(testUser);

        for (int i = 1; i <= 20; i++) {
            Api api = new Api();
            api.setApiUuid(UUID.randomUUID());
            api.setUser(testUser);
            api.setName("테스트 API " + i);
            api.setOriginalUrl("https://original-api.com/" + i);
            api.setProxyUrl("proxy-url-" + UUID.randomUUID().toString().substring(0, 8));
            api.setDescription("이 API는 " + i + "번째 테스트용입니다. 날씨 검색 가능.");
            api.setDocUrl("https://docs.api.com/" + i);
            api.setPrice(1000L * i);
            api.setViewCount((long) (Math.random() * 100));
            api.setStatus("active");
            api.setCensored(false);
            api.setCreatedAt(now);
            api.setUpdatedAt(now);

            apiRepository.save(api);
        }

        System.out.println("✅ 테스트 데이터 20개가 정상 등록되었습니다.");
    }
}