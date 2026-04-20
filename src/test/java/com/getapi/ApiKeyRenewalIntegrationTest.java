package com.getapi;

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
import org.springframework.http.MediaType;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * POST /auth/api-key — API 키 갱신 엔드포인트 통합 테스트
 *
 * Go 클라이언트의 fetchAPIKeyWithToken() 이 호출하는 서버 측 로직을 검증한다.
 *
 * 테스트 시나리오:
 *   1. rotation_token 없음                   → 400
 *   2. 존재하지 않는 rotation_token          → 404
 *   3. 올바른 rotation_token + 정상 서명     → 200 + 새 api_key 반환
 *   4. 올바른 rotation_token + 잘못된 서명   → 401/500
 *   5. 올바른 rotation_token + 만료 타임스탬프 → 401/500
 *   6. 갱신 후 기존 rotation_token 는 무효화 → 404
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiKeyRenewalIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ApiAuthRepository apiAuthRepository;
    @Autowired UserRepository userRepository;

    @MockBean ImapIdleChannelAdapter mailAdapter;

    private static final String SECRET_KEY      = "renewal-test-secret-32-characters!";
    private static final String ROTATION_TOKEN  = "renewal-test-rotation-token-64chars-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String CURRENT_API_KEY = "renewal-test-api-key-64chars-yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy";

    private Users testUser;
    private ApiAuth testApiAuth;

    @BeforeEach
    void setUp() {
        testUser = new Users(null, UUID.randomUUID(), "provider-renewal-test",
                "renewal-test@example.com", "01011112222", "ROLE_USER", LocalDateTime.now(), null);
        userRepository.save(testUser);

        testApiAuth = new ApiAuth();
        testApiAuth.setUser(testUser);
        testApiAuth.setApiKey(CURRENT_API_KEY);
        testApiAuth.setSecretKey(SECRET_KEY);
        testApiAuth.setRotationToken(ROTATION_TOKEN);
        testApiAuth.setExpiredDate(LocalDate.now().plusDays(7));
        testApiAuth.setApiUpdatedAt(LocalDateTime.now());
        testApiAuth.setRotationTokenUpdatedAt(LocalDateTime.now());
        apiAuthRepository.save(testApiAuth);
    }

    @AfterEach
    void tearDown() {
        apiAuthRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ──────────────────────────────────────────
    // 입력 유효성 검사
    // ──────────────────────────────────────────

    @Test
    @DisplayName("rotation_token 없이 요청 → 400")
    void renewApiKey_missingRotationToken_returns400() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, body, timestamp);

        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("빈 rotation_token → 400")
    void renewApiKey_emptyRotationToken_returns400() throws Exception {
        byte[] body = "{\"rotation_token\":\"\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, body, timestamp);

        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 rotation_token → 404")
    void renewApiKey_invalidRotationToken_returns404() throws Exception {
        byte[] body = "{\"rotation_token\":\"nonexistent-token\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, body, timestamp);

        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isNotFound());
    }

    // ──────────────────────────────────────────
    // 정상 갱신 — 핵심 시나리오
    // ──────────────────────────────────────────

    @Test
    @DisplayName("유효한 rotation_token + 올바른 HMAC 서명 → 200 + 새 api_key 반환")
    void renewApiKey_validTokenAndSignature_returns200() throws Exception {
        byte[] body = ("{\"rotation_token\":\"" + ROTATION_TOKEN + "\"}").getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, body, timestamp);

        MvcResult result = mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.api_key").isNotEmpty())
                .andExpect(jsonPath("$.expired_date").isNotEmpty())
                .andExpect(jsonPath("$.rotation_token").isNotEmpty())
                .andReturn();

        // 새 api_key 는 기존과 달라야 함
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain(CURRENT_API_KEY);
    }

    @Test
    @DisplayName("갱신 후 DB 의 api_key 가 실제로 변경되어야 한다")
    void renewApiKey_updatesApiKeyInDatabase() throws Exception {
        byte[] body = ("{\"rotation_token\":\"" + ROTATION_TOKEN + "\"}").getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, body, timestamp);

        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isOk());

        ApiAuth updated = apiAuthRepository.findByUser(testUser).orElseThrow();
        assertThat(updated.getApiKey())
                .as("api_key must change after renewal")
                .isNotEqualTo(CURRENT_API_KEY);
        assertThat(updated.getExpiredDate())
                .as("expired_date must be in the future")
                .isAfter(LocalDate.now());
    }

    @Test
    @DisplayName("갱신 후 새 rotation_token 도 함께 교체되어야 한다")
    void renewApiKey_rotatesRotationToken() throws Exception {
        byte[] body = ("{\"rotation_token\":\"" + ROTATION_TOKEN + "\"}").getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();
        String sig = computeSignature(SECRET_KEY, body, timestamp);

        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isOk());

        ApiAuth updated = apiAuthRepository.findByUser(testUser).orElseThrow();
        assertThat(updated.getRotationToken())
                .as("rotation_token must be rotated after renewal")
                .isNotEqualTo(ROTATION_TOKEN);
    }

    @Test
    @DisplayName("갱신 성공 후 기존 rotation_token 으로 다시 갱신 시도 → 404 (rotation_token 무효화)")
    void renewApiKey_oldTokenInvalidatedAfterRenewal() throws Exception {
        byte[] body = ("{\"rotation_token\":\"" + ROTATION_TOKEN + "\"}").getBytes(StandardCharsets.UTF_8);
        String ts1 = Instant.now().toString();
        String sig1 = computeSignature(SECRET_KEY, body, ts1);

        // 첫 번째 갱신 성공
        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", ts1)
                        .header("X-GetAPI-Signature", sig1))
                .andExpect(status().isOk());

        // 두 번째 — 같은 rotation_token 재사용 → 404
        String ts2 = Instant.now().toString();
        String sig2 = computeSignature(SECRET_KEY, body, ts2);

        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", ts2)
                        .header("X-GetAPI-Signature", sig2))
                .andExpect(status().isNotFound());
    }

    // ──────────────────────────────────────────
    // 서명 검증 실패 케이스
    // ──────────────────────────────────────────

    @Test
    @DisplayName("유효한 rotation_token + 잘못된 서명 → 401 (GlobalControllerAdvice가 SecurityException을 401로 변환)")
    void renewApiKey_validToken_wrongSignature_fails() throws Exception {
        byte[] body = ("{\"rotation_token\":\"" + ROTATION_TOKEN + "\"}").getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();

        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", timestamp)
                        .header("X-GetAPI-Signature", "0".repeat(64)))
                .andExpect(status().isUnauthorized()); // GlobalControllerAdvice → 401
    }

    @Test
    @DisplayName("유효한 rotation_token + 만료된 타임스탬프 → 401 (Replay Attack 방지)")
    void renewApiKey_expiredTimestamp_fails() throws Exception {
        byte[] body = ("{\"rotation_token\":\"" + ROTATION_TOKEN + "\"}").getBytes(StandardCharsets.UTF_8);
        String oldTimestamp = Instant.now().minusSeconds(400).toString(); // 6분 이상 전
        String sig = computeSignature(SECRET_KEY, body, oldTimestamp);

        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Timestamp", oldTimestamp)
                        .header("X-GetAPI-Signature", sig))
                .andExpect(status().isUnauthorized()); // GlobalControllerAdvice → 401
    }

    @Test
    @DisplayName("rotation_token 은 있지만 X-GetAPI-Timestamp 헤더 없음 → 500")
    void renewApiKey_missingTimestampHeader_fails() throws Exception {
        byte[] body = ("{\"rotation_token\":\"" + ROTATION_TOKEN + "\"}").getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/auth/api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("X-GetAPI-Signature", "0".repeat(64)))
                .andExpect(status().is4xxClientError());
    }

    // ──────────────────────────────────────────
    // helper
    // ──────────────────────────────────────────

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
