package com.getapi.auth.controller;

import com.getapi.auth.domain.ApiAuth;
import com.getapi.auth.domain.DeviceCode;
import com.getapi.auth.repository.ApiAuthRepository;
import com.getapi.auth.repository.DeviceCodeRepository;
import com.getapi.auth.util.SecureUtil;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/auth/device")
@RequiredArgsConstructor
public class DeviceAuthController {

    private final DeviceCodeRepository deviceCodeRepository;
    private final ApiAuthRepository apiAuthRepository;
    private final UserRepository userRepository;

    // Go 클라이언트 → device_code + user_code 발급
    @PostMapping("/code")
    @ResponseBody
    public ResponseEntity<?> issueDeviceCode() {
        String deviceCode = SecureUtil.generate64Token();
        String userCode = generateUserCode();

        DeviceCode dc = new DeviceCode(deviceCode, userCode, "pending", null);
        deviceCodeRepository.save(dc);

        return ResponseEntity.ok(Map.of(
                "device_code", deviceCode,
                "user_code", userCode,
                "verification_url", "/auth/device/activate",
                "expires_in", 600,
                "interval", 5
        ));
    }

    // 사용자 브라우저 → 활성화 페이지
    @GetMapping("/activate")
    public String activatePage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
        model.addAttribute("loggedIn", loggedIn);
        return "device-activate";
    }

    // OAuth2 로그인 시작 → 로그인 후 /auth/device/activate로 돌아오도록 쿠키 설정
    @GetMapping("/oauth-start")
    public String oauthStart(jakarta.servlet.http.HttpServletResponse response) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("_device_redirect", "/auth/device/activate");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(300); // 5분
        response.addCookie(cookie);
        return "redirect:/oauth2/authorization/google";
    }

    // 사용자 브라우저 → user_code 제출 (승인)
    @PostMapping("/approve")
    @ResponseBody
    public ResponseEntity<?> approve(@RequestParam("userCode") String userCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
        }

        String sub = (String) auth.getPrincipal();
        Users user = userRepository.findByProviderId(sub);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "세션이 만료되었습니다. 다시 로그인해 주세요.", "redirect", "/auth/device/activate"));
        }

        Optional<DeviceCode> dcOpt = deviceCodeRepository.findByUserCode(userCode.toUpperCase());
        if (dcOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "유효하지 않거나 만료된 코드입니다."));
        }

        DeviceCode dc = dcOpt.get();
        if (!"pending".equals(dc.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 사용된 코드입니다."));
        }

        dc.setStatus("approved");
        dc.setUserId(user.getUserId());
        deviceCodeRepository.save(dc);

        return ResponseEntity.ok(Map.of("message", "승인되었습니다."));
    }

    // Go 클라이언트 → 폴링
    @GetMapping("/poll/{deviceCode}")
    @ResponseBody
    public ResponseEntity<?> poll(@PathVariable("deviceCode") String deviceCode) {
        Optional<DeviceCode> dcOpt = deviceCodeRepository.findById(deviceCode);

        if (dcOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "expired"));
        }

        DeviceCode dc = dcOpt.get();

        if ("pending".equals(dc.getStatus())) {
            return ResponseEntity.ok(Map.of("status", "pending"));
        }

        // 승인됨 → 새 크리덴셜 발급
        Users user = userRepository.findById(dc.getUserId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "사용자를 찾을 수 없습니다."));
        }

        ApiAuth apiAuth = apiAuthRepository.findByUser(user).orElse(new ApiAuth());

        if (apiAuth.getSecretKey() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", "시크릿 키를 먼저 설정해주세요."));
        }

        apiAuth.setUser(user);
        apiAuth.setApiKey(SecureUtil.generate64Token());
        apiAuth.setRotationToken(SecureUtil.generate64Token());
        apiAuth.setApiUpdatedAt(LocalDateTime.now());
        apiAuth.setRotationTokenUpdatedAt(LocalDateTime.now());
        apiAuth.setExpiredDate(LocalDate.now().plusDays(7));
        apiAuthRepository.save(apiAuth);

        // 사용된 device_code 삭제
        deviceCodeRepository.delete(dc);

        return ResponseEntity.ok(Map.of(
                "status", "approved",
                "api_key", apiAuth.getApiKey(),
                "rotation_token", apiAuth.getRotationToken(),
                "secret_key", apiAuth.getSecretKey(),
                "expired_date", apiAuth.getExpiredDate().toString()
        ));
    }

    private String generateUserCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rng = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append('-');
            sb.append(chars.charAt(rng.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
