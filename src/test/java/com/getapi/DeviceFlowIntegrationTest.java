package com.getapi;

import com.getapi.auth.domain.DeviceCode;
import com.getapi.auth.repository.DeviceCodeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Device Flow (RFC 8628) 엔드포인트 통합 테스트
 *
 * 테스트 대상:
 *   POST /auth/device/code   — device_code + user_code 발급
 *   GET  /auth/device/poll/{deviceCode} — 폴링 (pending / expired)
 *   POST /auth/device/approve — 승인 (로그인 필요)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired DeviceCodeRepository deviceCodeRepository;

    // IMAP 어댑터가 실제로 Gmail 에 연결하지 않도록 Mock 처리
    @MockBean ImapIdleChannelAdapter mailAdapter;

    @AfterEach
    void cleanup() {
        deviceCodeRepository.deleteAll();
    }

    // ──────────────────────────────────────────
    // POST /auth/device/code
    // ──────────────────────────────────────────

    @Test
    @DisplayName("device_code 발급 — 200 + 필수 필드 반환")
    void issueDeviceCode_returns200WithRequiredFields() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/device/code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.device_code").isNotEmpty())
                .andExpect(jsonPath("$.user_code").isNotEmpty())
                .andExpect(jsonPath("$.verification_url").value("/auth/device/activate"))
                .andExpect(jsonPath("$.expires_in").value(600))
                .andExpect(jsonPath("$.interval").value(5))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("null");
    }

    @Test
    @DisplayName("발급된 device_code 는 Redis 에 저장되어야 한다")
    void issueDeviceCode_savedToRedis() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/device/code"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // JSON 에서 device_code 추출 (간단 파싱)
        String deviceCode = extractJsonValue(responseBody, "device_code");
        assertThat(deviceCode).isNotBlank();

        assertThat(deviceCodeRepository.existsById(deviceCode))
                .as("device_code must be persisted in Redis")
                .isTrue();
    }

    @Test
    @DisplayName("발급된 device_code 의 초기 상태는 pending 이어야 한다")
    void issueDeviceCode_initialStatusIsPending() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/device/code"))
                .andExpect(status().isOk())
                .andReturn();

        String deviceCode = extractJsonValue(result.getResponse().getContentAsString(), "device_code");
        DeviceCode dc = deviceCodeRepository.findById(deviceCode).orElseThrow();
        assertThat(dc.getStatus()).isEqualTo("pending");
    }

    @Test
    @DisplayName("user_code 는 XXXX-XXXX 형식 8자 (구분자 제외) 이어야 한다")
    void issueDeviceCode_userCodeFormat() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/device/code"))
                .andExpect(status().isOk())
                .andReturn();

        String userCode = extractJsonValue(result.getResponse().getContentAsString(), "user_code");
        // 예: ABCD-EFGH
        assertThat(userCode).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");
    }

    @Test
    @DisplayName("device_code 는 호출마다 고유해야 한다")
    void issueDeviceCode_isUnique() throws Exception {
        MvcResult r1 = mockMvc.perform(post("/auth/device/code")).andReturn();
        MvcResult r2 = mockMvc.perform(post("/auth/device/code")).andReturn();

        String dc1 = extractJsonValue(r1.getResponse().getContentAsString(), "device_code");
        String dc2 = extractJsonValue(r2.getResponse().getContentAsString(), "device_code");
        assertThat(dc1).isNotEqualTo(dc2);
    }

    // ──────────────────────────────────────────
    // GET /auth/device/poll/{deviceCode}
    // ──────────────────────────────────────────

    @Test
    @DisplayName("pending 상태 폴링 → status=pending 반환")
    void pollDeviceCode_pending_returnsPendingStatus() throws Exception {
        // device_code 발급
        MvcResult issueResult = mockMvc.perform(post("/auth/device/code")).andReturn();
        String deviceCode = extractJsonValue(issueResult.getResponse().getContentAsString(), "device_code");

        // 폴링
        mockMvc.perform(get("/auth/device/poll/" + deviceCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    @DisplayName("존재하지 않는 device_code 폴링 → status=expired 반환")
    void pollDeviceCode_notFound_returnsExpired() throws Exception {
        mockMvc.perform(get("/auth/device/poll/nonexistent-device-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("expired"));
    }

    @Test
    @DisplayName("승인된 device_code 폴링 — 사용자 없음 → error 반환")
    void pollDeviceCode_approved_noUser_returnsError() throws Exception {
        // approved 상태의 DeviceCode 를 직접 저장 (userId=9999 가 존재하지 않음)
        DeviceCode dc = new DeviceCode("test-dev-code", "TEST-CODE", "approved", 9999L);
        deviceCodeRepository.save(dc);

        mockMvc.perform(get("/auth/device/poll/test-dev-code"))
                .andExpect(status().is5xxServerError());
    }

    // ──────────────────────────────────────────
    // POST /auth/device/approve
    // ──────────────────────────────────────────

    @Test
    @DisplayName("로그인하지 않은 상태에서 approve 요청 → 401")
    void approveDeviceCode_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/auth/device/approve")
                        .param("userCode", "ABCD-EFGH"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────
    // GET /auth/device/activate
    // ──────────────────────────────────────────

    @Test
    @DisplayName("activate 페이지 — 200 반환")
    void activatePage_returns200() throws Exception {
        mockMvc.perform(get("/auth/device/activate"))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────
    // helper
    // ──────────────────────────────────────────

    private String extractJsonValue(String json, String key) {
        // 간단 파싱: "key":"value" 또는 "key":value
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start >= 0) {
            start += search.length();
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        }
        return null;
    }
}
