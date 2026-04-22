//package com.getapi.test;
//
//import com.getapi.api.domain.Api;
//import com.getapi.library.repository.ApiRepository;
//import com.getapi.user.domain.Users;
//import com.getapi.user.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Component
//@RequiredArgsConstructor
//public class TestDataInit implements CommandLineRunner {
//
//    private final ApiRepository apiRepository;
//    private final UserRepository userRepository;
//
//    @Override
//    @Transactional
//    public void run(String... args) {
//        // 기존 데이터가 20개 정도 있을 테니, 100개 이상일 때만 중단하도록 변경
//        if (apiRepository.count() > 100) {
//            return;
//        }
//
//        LocalDateTime now = LocalDateTime.now();
//
//        // 기존 유저를 쓰거나 새로 생성 (여기서는 새로 생성)
//        Users testUser = new Users();
//        testUser.setUserUuid(UUID.randomUUID());
//        testUser.setProviderId("test-provider-" + UUID.randomUUID());
//        testUser.setEmail("dev@getapi.com");
//        testUser.setName("SiwooLee");
//        testUser.setNickname("개발자시우");
//        testUser.setPhone("01000000000");
//        testUser.setRole("USER");
//        testUser.setProfileCreatedAt(now);
//        testUser.setProfileImage("https://getapi.com/images/default-profile.png");
//        userRepository.save(testUser);
//
//        // 1. 날씨 관련 API (21~30번)
//        saveApiGroup(testUser, 21, 30, "날씨", "실시간 기상청 데이터 및 전세계 날씨 예보", 1500L);
//
//        // 2. 금융/주식 관련 API (31~40번)
//        saveApiGroup(testUser, 31, 40, "주식", "국내외 증시 시세 및 기업 재무 제표 조회", 5000L);
//
//        // 3. AI/이미지 관련 API (41~50번)
//        saveApiGroup(testUser, 41, 50, "AI 생성", "스테이블 디퓨전 기반 이미지 생성 및 텍스트 분석", 3000L);
//
//        // 4. 쇼핑/상품 관련 API (51~60번)
//        saveApiGroup(testUser, 51, 60, "쇼핑", "네이버/쿠팡 최저가 비교 및 상품 상세 정보", 2000L);
//
//        System.out.println("✅ 검색 테스트를 위한 다양한 카테고리의 데이터가 추가되었습니다.");
//    }
//
//    // 중복 코드를 줄이기 위한 헬퍼 메서드
//    private void saveApiGroup(Users user, int start, int end, String keyword, String descPrefix, Long price) {
//        for (int i = start; i <= end; i++) {
//            Api api = new Api();
//            api.setApiUuid(UUID.randomUUID());
//            api.setUser(user);
//            api.setName(keyword + " 정보 서비스 " + i);
//            api.setOriginalUrl("https://api.provider.com/" + keyword + "/" + i);
//            api.setProxyUrl("proxy-" + keyword + "-" + i);
//            api.setDescription(descPrefix + "를 제공하는 " + i + "번째 API입니다.");
//            api.setDocUrl("https://docs.getapi.com/" + i);
//            api.setPrice(price);
//            api.setViewCount((long) (Math.random() * 200));
//            api.setStatus("active");
//            api.setCreatedAt(LocalDateTime.now());
//            api.setUpdatedAt(LocalDateTime.now());
//            apiRepository.save(api);
//        }
//    }
//}