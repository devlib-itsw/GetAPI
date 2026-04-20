package com.getapi;

import com.getapi.api.domain.Api;
import com.getapi.api.repository.ApiRepository;
import com.getapi.auth.domain.ApiAuth;
import com.getapi.auth.repository.ApiAuthRepository;
import com.getapi.auth.util.SecureUtil;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProxyController 통합 테스트 — /lib/** 인증 및 라우팅 로직
 *
 * 테스트 시나리오:
 *   1. API Key 헤더 없음                → 401
 *   2. 유효하지 않은 API Key            → 401
 *   3. 서명 헤더 없음                   → 401
 *   4. 서명 불일치                      → 401
 *   5. 만료된 API Key                  → 401
 *   6. 정상 인증 + slug 없음            → 404
 *   7. 정상 인증 + API 비활성           → 503
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProxyIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ApiAuthRepository apiAuthRepository;
    @Autowired ApiRepository apiRepository;
    @Autowired UserRepository userRepository;

    @MockBean ImapIdleChannelAdapter mailAdapter;

    private static final String SECRET_KEY = "proxy-test-secret-key-32-chars!!";
    private static final String API_KEY    = "proxy-test-api-key-64chars-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    private Users testUser;
    private ApiAuth testApiAuth;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = new Users(null, UUID.randomUUID(), "provider-proxy-test",
                "proxy-test@example.com", "01012345678", "ROLE_USER", LocalDateTime.now(), null);
        userRepository.save(testUser);

        // 유효한 ApiAuth (만료일 = 내일)
        testApiAuth = new ApiAuth();
        testApiAuth.setUser(testUser);
        testApiAuth.setApiKey(API_KEY);
        testApiAuth.setSecretKey(SECRET_KEY);
        testApiAuth.setExpiredDate(LocalDate.now().plusDays(1));
        testApiAuth.setApiUpdatedAt(LocalDateTime.now());
        apiAuthRepository.save(testApiAuth);
    }

    @AfterEach
    void tearDown() {
        apiRepository.deleteAll();
        apiAuthRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ──────────────────────────────────────────
    // 인증 실패 케이스
    // ──────────────────────────────────────────

    @Test
    @DisplayName("X-GetAPI-Key 헤더 없음 → 401")
    void proxy_missingApiKey_returns401() throws Exception {
        mockMvc.perform(get("/lib/test-slug/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 API Key → 401")
    void proxy_invalidApiKey_returns401() throws Exception {
        mockMvc.perform(get("/lib/test-slug/v1/users")
                        .header("X-GetAPI-Key", "invalid-key-that-does-not-exist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API Key 는 유효하지만 서명 헤더 없음 → 401")
    void proxy_missingSignatureHeaders_returns401() throws Exception {
        mockMvc.perform(get("/lib/test-slug/v1/users")
                        .header("X-GetAPI-Key", API_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API Key 유효 + 타임스탬프만 있고 서명 없음 → 401")
    void proxy_missingSignature_returns401() throws Exception {
        mockMvc.perform(get("/lib/test-slug/v1/users")
                        .header("X-GetAPI-Key", API_KEY)
                        .header("X-GetAPI-Timestamp", Instant.now().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API Key 유효 + 서명 불일치 → 401")
    void proxy_invalidSignature_returns401() throws Exception {
        mockMvc.perform(get("/lib/test-slug/v1/users")
                        .header("X-GetAPI-Key", API_KEY)
                        .header("X-GetAPI-Timestamp", Instant.now().toString())
                        .header("X-GetAPI-Signature", "0".repeat(64)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 API Key → 401")
    void proxy_expiredApiKey_returns401() throws Exception {
        // 만료된 별도 ApiAuth 생성
        Users expiredUser = new Users(null, UUID.randomUUID(), "provider-expired",
                "expired@example.com", "01099998888", "ROLE_USER", LocalDateTime.now(), null);
        userRepository.save(expiredUser);

        String expiredKey = "expired-api-key-64chars-yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy";
        ApiAuth expiredAuth = new ApiAuth();
        expiredAuth.setUser(expiredUser);
        expiredAuth.setApiKey(expiredKey);
        expiredAuth.setSecretKey(SECRET_KEY);
        expiredAuth.setExpiredDate(LocalDate.now().minusDays(1)); // 어제 만료
        expiredAuth.setApiUpdatedAt(LocalDateTime.now().minusDays(2));
        apiAuthRepository.save(expiredAuth);

        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, new byte[0], timestamp);

        mockMvc.perform(get("/lib/test-slug/v1/users")
                        .header("X-GetAPI-Key", expiredKey)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("타임스탬프가 5분 이상 지난 경우 → 401 (Replay Attack 방지)")
    void proxy_expiredTimestamp_returns401() throws Exception {
        String oldTimestamp = Instant.now().minusSeconds(400).toString();
        String sig = computeSignature(SECRET_KEY, new byte[0], oldTimestamp);

        mockMvc.perform(get("/lib/test-slug/v1/users")
                        .header("X-GetAPI-Key", API_KEY)
                        .header("X-GetAPI-Timestamp", oldTimestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────
    // 인증 통과 후 라우팅 케이스
    // ──────────────────────────────────────────

    @Test
    @DisplayName("정상 인증 + 등록되지 않은 slug → 404")
    void proxy_validAuth_slugNotFound_returns404() throws Exception {
        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, new byte[0], timestamp);

        mockMvc.perform(get("/lib/nonexistent-slug/v1/users")
                        .header("X-GetAPI-Key", API_KEY)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("정상 인증 + API 비활성 → 503")
    void proxy_validAuth_inactiveApi_returns503() throws Exception {
        // inactive API 등록
        Api inactiveApi = buildApi(testUser, "inactive-slug", "http://example.com/api", "inactive");
        apiRepository.save(inactiveApi);

        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, new byte[0], timestamp);

        mockMvc.perform(get("/lib/inactive-slug/v1/users")
                        .header("X-GetAPI-Key", API_KEY)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("정상 인증 + active API + 포워딩 대상 없음 → 502/500 계열 에러")
    void proxy_validAuth_activeApi_forwardingFails() throws Exception {
        // 연결 불가 URL 로 등록된 active API
        Api activeApi = buildApi(testUser, "active-slug", "http://127.0.0.1:19979", "active");
        apiRepository.save(activeApi);

        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, new byte[0], timestamp);

        mockMvc.perform(get("/lib/active-slug/v1/resource")
                        .header("X-GetAPI-Key", API_KEY)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().is5xxServerError()); // 502 or 500 (connection refused)
    }

    @Test
    @DisplayName("POST 요청 body 포함 — 서명 검증 통과해야 한다")
    void proxy_postWithBody_signatureValid() throws Exception {
        byte[] body = "{\"test\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, body, timestamp);

        mockMvc.perform(post("/lib/nonexistent-slug/v1/data")
                        .header("X-GetAPI-Key", API_KEY)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound()); // 인증 통과, slug 없음 → 404
    }

    // ──────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────

    private Api buildApi(Users user, String slug, String url, String status) {
        Api api = new Api();
        api.setUser(user);
        api.setApiUuid(UUID.randomUUID());
        api.setName(slug + "-name");
        api.setOriginalUrl(url);
        api.setProxyUrl(slug);
        api.setDescription("test");
        api.setDocUrl("http://docs.example.com");
        api.setPrice(0L);
        api.setStatus(status);
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());
        return api;
    }

    private String computeSignature(String secretKey, byte[] body, String timestamp) throws Exception {
        byte[] tsBytes = timestamp.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[body.length + tsBytes.length];
        System.arraycopy(body, 0, message, 0, body.length);
        System.arraycopy(tsBytes, 0, message, body.length, tsBytes.length);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(message));
    }
}
