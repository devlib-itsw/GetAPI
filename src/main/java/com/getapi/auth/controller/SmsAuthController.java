package com.getapi.auth.controller;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.getapi.auth.domain.RefreshToken;
import com.getapi.auth.service.RefreshTokenService;
import com.getapi.auth.service.SmsAuthService;
import com.getapi.auth.util.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/phoneVerify")
public class SmsAuthController {
	private final SmsAuthService smsAuthService;
	private final RefreshTokenService refreshTokenService;
	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
	private final Map<String, String> pendingJwts = new ConcurrentHashMap<>();
	private final JwtUtil jwtUtil;

	@GetMapping("/{token}")
	public String returnHtml(@PathVariable("token") String token, Model model) {
		if (token == null || token.isEmpty()) {
			model.addAttribute("errorMessage", "유효하지 않은 접근입니다. 토큰이 없습니다.");
			return "redirect:/";
		}

		return "phone-verify";
	}
	
	@ResponseBody
	@PostMapping("")
	public ResponseEntity<?> handleVerification(@RequestBody Map<String, Object> data) {
		String token = (String) data.get("key");
		String phone = (String) data.get("phone");

		if (token == null || phone == null) {
			return ResponseEntity.badRequest().body(Map.of("message", "알맞은 입력값이 아닙니다."));
		}

		// 숫자만 추출
		String digits = phone.replaceAll("[^0-9]", "");

		// 010으로 시작하는 11자리 번호 패턴
		Pattern pattern = Pattern.compile("010[0-9]{8}");
		Matcher matcher = pattern.matcher(digits);
		String clear;

		if (matcher.find()) {
			clear = matcher.group();
		} else {
			return ResponseEntity.badRequest().body(Map.of("message", "알맞은 입력값이 아닙니다."));
		}

		try {
			smsAuthService.updateToken(token, clear);
		} catch (ResponseStatusException e) {
			return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
		}

		return ResponseEntity.ok().build();
	}

	// SSE 연결 엔드포인트
	@ResponseBody
	@GetMapping("/stream/{token}")
	public SseEmitter stream(@PathVariable("token") String token) {
		SseEmitter emitter = new SseEmitter(330_000L);

		emitters.put(token, emitter);

		emitter.onCompletion(() -> emitters.remove(token));
		emitter.onTimeout(() -> emitters.remove(token));

		return emitter;
	}

	// 인증 완료 시 JWT 생성 + SSE 알림
	public void notifyVerified(String token, String sub) {
		// JWT 생성 후 임시 저장 (프론트에서 /smsVerify/complete로 요청 시 쿠키로 설정)
		String jwt = jwtUtil.generateToken(sub);
		pendingJwts.put(token, jwt);

		SseEmitter emitter = emitters.get(token);
		if (emitter != null) {
			try {
				emitter.send(SseEmitter.event().name("verified").data("ok"));
				emitter.complete();
			} catch (IOException e) {
				emitter.completeWithError(e);
			}
			emitters.remove(token);
		}
	}

	// SSE "verified" 수신 후 프론트에서 리다이렉트 → httpOnly 쿠키 설정
	@GetMapping("/complete/{token}")
	public void completeVerification(@PathVariable("token") String token,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		String jwt = pendingJwts.remove(token);

		if (jwt == null) {
			response.sendRedirect("/");
			return;
		}

		// 액세스 토큰 쿠키
		Cookie cookie = new Cookie("JWT-TOKEN", jwt);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setMaxAge(1800); // 30분
		response.addCookie(cookie);

		// 리프레시 토큰 발급
		String sub = jwtUtil.getSubFromToken(jwt);
		RefreshToken rt = refreshTokenService.save(sub, request.getRemoteAddr(), request.getHeader("User-Agent"));
		Cookie refreshCookie = new Cookie("REFRESH-TOKEN", rt.getToken());
		refreshCookie.setHttpOnly(true);
		refreshCookie.setPath("/");
		refreshCookie.setMaxAge(2592000); // 30일
		response.addCookie(refreshCookie);

		response.sendRedirect("/");
	}
}
