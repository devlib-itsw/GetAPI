package com.getapi;

import com.getapi.auth.util.SecureUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.*;

/**
 * SecureUtil 단위 테스트
 * - Spring 컨텍스트 불필요 (순수 로직 검증)
 * - Go generateSignature() ↔ Java verifyHmac() 호환성 포함
 */
class SecureUtilTest {

    // ──────────────────────────────────────────
    // isTimestampValid
    // ──────────────────────────────────────────

    @Test
    @DisplayName("현재 시각 타임스탬프는 유효해야 한다")
    void timestamp_current_isValid() {
        String now = Instant.now().toString(); // RFC3339
        assertThat(SecureUtil.isTimestampValid(now)).isTrue();
    }

    @Test
    @DisplayName("4분 전 타임스탬프는 유효해야 한다 (±5분 허용)")
    void timestamp_4minAgo_isValid() {
        String ts = Instant.now().minusSeconds(240).toString();
        assertThat(SecureUtil.isTimestampValid(ts)).isTrue();
    }

    @Test
    @DisplayName("6분 전 타임스탬프는 만료로 처리되어야 한다")
    void timestamp_6minAgo_isExpired() {
        String ts = Instant.now().minusSeconds(360).toString();
        assertThat(SecureUtil.isTimestampValid(ts)).isFalse();
    }

    @Test
    @DisplayName("6분 미래 타임스탬프는 만료로 처리되어야 한다")
    void timestamp_6minFuture_isInvalid() {
        String ts = Instant.now().plusSeconds(360).toString();
        assertThat(SecureUtil.isTimestampValid(ts)).isFalse();
    }

    @Test
    @DisplayName("잘못된 형식의 타임스탬프는 false 반환")
    void timestamp_invalidFormat_returnsFalse() {
        assertThat(SecureUtil.isTimestampValid("not-a-timestamp")).isFalse();
        assertThat(SecureUtil.isTimestampValid("")).isFalse();
        assertThat(SecureUtil.isTimestampValid(null)).isFalse();
    }

    // ──────────────────────────────────────────
    // verifyHmac — Go generateSignature() 와 호환성
    // ──────────────────────────────────────────

    /**
     * Go 코드와 동일한 방법으로 HMAC-SHA256 서명 계산
     * Go: h := hmac.New(sha256.New, []byte(secret)); h.Write(body+timestamp)
     */
    private String computeGoSignature(String secretKey, byte[] body, String timestamp) throws Exception {
        byte[] tsBytes = timestamp.getBytes(StandardCharsets.UTF_8);
        byte[] message = new byte[body.length + tsBytes.length];
        System.arraycopy(body, 0, message, 0, body.length);
        System.arraycopy(tsBytes, 0, message, body.length, tsBytes.length);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(message));
    }

    @Test
    @DisplayName("Go 방식으로 생성한 서명을 Java verifyHmac 이 검증해야 한다 (핵심 호환성)")
    void hmac_goSignature_isValidatedByJava() throws Exception {
        String secret = "shared-secret-key";
        byte[] body = "{\"rotation_token\":\"abc123\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();

        String signature = computeGoSignature(secret, body, timestamp);

        assertThat(SecureUtil.verifyHmac(secret, body, timestamp, signature))
                .as("Go-generated HMAC must be verifiable by Java SecureUtil.verifyHmac()")
                .isTrue();
    }

    @Test
    @DisplayName("body 가 변조되면 서명 검증 실패해야 한다")
    void hmac_tamperedBody_fails() throws Exception {
        String secret = "secret";
        byte[] originalBody = "{\"rotation_token\":\"token\"}".getBytes(StandardCharsets.UTF_8);
        byte[] tamperedBody = "{\"rotation_token\":\"TAMPERED\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();

        String signature = computeGoSignature(secret, originalBody, timestamp);

        assertThat(SecureUtil.verifyHmac(secret, tamperedBody, timestamp, signature)).isFalse();
    }

    @Test
    @DisplayName("타임스탬프가 변조되면 서명 검증 실패해야 한다")
    void hmac_tamperedTimestamp_fails() throws Exception {
        String secret = "secret";
        byte[] body = "{\"rotation_token\":\"token\"}".getBytes(StandardCharsets.UTF_8);
        String originalTs = Instant.now().toString();
        String tamperedTs = Instant.now().minusSeconds(1).toString();

        String signature = computeGoSignature(secret, body, originalTs);

        assertThat(SecureUtil.verifyHmac(secret, body, tamperedTs, signature)).isFalse();
    }

    @Test
    @DisplayName("secret 이 다르면 서명 검증 실패해야 한다")
    void hmac_wrongSecret_fails() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();

        String signature = computeGoSignature("correct-secret", body, timestamp);

        assertThat(SecureUtil.verifyHmac("wrong-secret", body, timestamp, signature)).isFalse();
    }

    @Test
    @DisplayName("빈 body + 타임스탬프로도 서명 생성·검증이 가능해야 한다")
    void hmac_emptyBody_works() throws Exception {
        String secret = "secret";
        byte[] body = new byte[0];
        String timestamp = Instant.now().toString();

        String signature = computeGoSignature(secret, body, timestamp);
        assertThat(SecureUtil.verifyHmac(secret, body, timestamp, signature)).isTrue();
    }

    // ──────────────────────────────────────────
    // validateRequest (통합 검증)
    // ──────────────────────────────────────────

    @Test
    @DisplayName("유효한 요청은 예외 없이 통과해야 한다")
    void validateRequest_valid_noException() throws Exception {
        String secret = "valid-secret";
        byte[] body = "{\"rotation_token\":\"rt\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();
        String sig = computeGoSignature(secret, body, timestamp);

        assertThatNoException().isThrownBy(
                () -> SecureUtil.validateRequest(secret, body, timestamp, sig));
    }

    @Test
    @DisplayName("만료된 타임스탬프는 SecurityException 발생해야 한다")
    void validateRequest_expiredTimestamp_throws() throws Exception {
        String secret = "secret";
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String oldTs = Instant.now().minusSeconds(400).toString(); // 6분 초과
        String sig = computeGoSignature(secret, body, oldTs);

        assertThatThrownBy(() -> SecureUtil.validateRequest(secret, body, oldTs, sig))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Timestamp");
    }

    @Test
    @DisplayName("서명이 올바르지 않으면 SecurityException 발생해야 한다")
    void validateRequest_invalidSignature_throws() {
        String secret = "secret";
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Instant.now().toString();

        assertThatThrownBy(() -> SecureUtil.validateRequest(secret, body, timestamp, "badhex0000"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("signature");
    }

    // ──────────────────────────────────────────
    // generate64Token
    // ──────────────────────────────────────────

    @Test
    @DisplayName("generate64Token 은 64자 소문자 hex 를 반환해야 한다")
    void generate64Token_format() {
        String token = SecureUtil.generate64Token();
        assertThat(token).hasSize(64);
        assertThat(token).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("generate64Token 은 매 호출마다 다른 값을 반환해야 한다 (랜덤성)")
    void generate64Token_isRandom() {
        String t1 = SecureUtil.generate64Token();
        String t2 = SecureUtil.generate64Token();
        assertThat(t1).isNotEqualTo(t2);
    }
}
