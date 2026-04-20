package com.getapi.api.controller;

import com.getapi.api.domain.Api;
import com.getapi.user.domain.Users;
import com.getapi.api.service.ApiService;
import com.getapi.proxy.service.CallLogService;
import com.getapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

	private final ApiService apiService;
	private final UserRepository userRepository;
	private final CallLogService callLogService;

	// uuid → SSE emitter (브라우저 대기)
	private final Map<String, SseEmitter> verifyEmitters = new ConcurrentHashMap<>();

	// ─── 페이지 ───────────────────────────────────────────────

	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		return "dashboard";
	}

	@GetMapping("/dashboard/create")
	public String create(Model model) {
		return "dashboard-create";
	}

	@GetMapping("/dashboard/view/{uuid}")
	public String view(@PathVariable("uuid") String uuid, Model model) {
		Optional<Api> api = apiService.findByUuid(UUID.fromString(uuid));

		if (api.isEmpty()) {
			return "redirect:/api/dashboard";
		}

		model.addAttribute("api", api.get());
		model.addAttribute("totalCalls", callLogService.getTotalCalls(api.get()));
		model.addAttribute("todayCalls", callLogService.getTodayCalls(api.get()));
		model.addAttribute("totalRevenue", callLogService.getTotalRevenue(api.get()));
		model.addAttribute("callLogs", callLogService.getRecentLogs(api.get(), 50));
		return "dashboard-view";
	}

	// HATEOAS 활성화 토글
	@PostMapping("/dashboard/view/{uuid}/hateoas/toggle")
	@ResponseBody
	public ResponseEntity<?> toggleHateoas(@PathVariable("uuid") String uuid, @RequestBody Map<String, Object> body) {
		Optional<Api> api = apiService.findByUuid(UUID.fromString(uuid));
		if (api.isEmpty())
			return ResponseEntity.notFound().build();
		boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
		if (enabled) {
			try {
				if (!apiService.isOriginalUrlJson(api.get())) {
					return ResponseEntity.badRequest()
							.body(Map.of("message", "원본 API가 JSON을 반환하지 않아 HATEOAS를 활성화할 수 없습니다."));
				}
			} catch (Exception e) {
				return ResponseEntity.badRequest().body(Map.of("message", "원본 서버에 연결할 수 없습니다: " + e.getMessage()));
			}
		}
		apiService.setHateoasEnabled(api.get(), enabled);
		return ResponseEntity.ok().build();
	}

	// HATEOAS 설정 저장 (링크 포함)
	@PostMapping("/dashboard/view/{uuid}/hateoas")
	@ResponseBody
	public ResponseEntity<?> saveHateoas(@PathVariable("uuid") String uuid, @RequestBody Map<String, Object> body) {
		Optional<Api> api = apiService.findByUuid(UUID.fromString(uuid));
		if (api.isEmpty())
			return ResponseEntity.notFound().build();
		boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
		String linksJson = body.get("links") != null ? body.get("links").toString() : "[]";
		apiService.saveHateoas(api.get(), enabled, linksJson);
		return ResponseEntity.ok().build();
	}

	// 서버 헬스체크
	@RequestMapping(value = "/dashboard/view/{uuid}/health", method = RequestMethod.HEAD)
	@ResponseBody
	public ResponseEntity<?> healthCheck(@PathVariable("uuid") String uuid) {
		Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
		if (apiOpt.isEmpty())
			return ResponseEntity.notFound().build();

		Api api = apiOpt.get();
		try {
			java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(api.getOriginalUrl())
					.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setRequestMethod("HEAD");
			int code = conn.getResponseCode();
			return code < 500 ? ResponseEntity.ok().build() : ResponseEntity.status(code).build();
		} catch (Exception e) {
			return ResponseEntity.status(502).build();
		}
	}

	@GetMapping("/library")
	public String library(Model model) {
		return "library";
	}

	@GetMapping("/library/view/{uuid}")
	public String libraryview(@PathVariable("uuid") String uuid, Model model) {
		model.addAttribute("uuid", uuid);
		return "library-view";
	}

	@GetMapping("/dashboard/verify/{uuid}")
	public String verifyPage(@PathVariable("uuid") String uuid, Model model) {
		model.addAttribute("uuid", uuid);
		return "dashboard-verify";
	}

	// ─── Step1: API 등록 ──────────────────────────────────────

	@PostMapping("")
	public String createApi(@RequestParam("name") String name, @RequestParam("method") String method,
			@RequestParam(name = "slug", required = false) String slug, @RequestParam("originUri") String originUri,
			@RequestParam("description") String description, @RequestParam("points") Long points,
			@RequestParam("apiDoc") MultipartFile apiDoc, @RequestParam(name = "tags", required = false) String[] tags,
			RedirectAttributes redirectAttributes) {

		try {
			String sub = SecurityContextHolder.getContext().getAuthentication().getName();
			Users user = userRepository.findByProviderId(sub);
			if (user == null) {
				return "redirect:/login";
			}

			Api api = apiService.createApi(user, name, method, slug, originUri.replaceAll("/+$", ""), description, points,
					apiDoc);
			return "redirect:/api/dashboard/verify/" + api.getApiUuid();

		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/api/dashboard/create";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "등록 중 오류가 발생했습니다.");
			return "redirect:/api/dashboard/create";
		}
	}

	// ─── Step2: 검증 ─────────────────────────────────────────

	// 브라우저: SSE 연결 (검증 완료 대기)
	@GetMapping("/verify/stream/{uuid}")
	@ResponseBody
	public SseEmitter stream(@PathVariable("uuid") String uuid) {
		SseEmitter emitter = new SseEmitter(600_000L); // 10분
		verifyEmitters.put(uuid, emitter);
		emitter.onCompletion(() -> verifyEmitters.remove(uuid));
		emitter.onTimeout(() -> verifyEmitters.remove(uuid));
		return emitter;
	}

	// 토큰 정보 조회 (페이지 로드 시)
	@GetMapping("/verify/info/{uuid}")
	@ResponseBody
	public ResponseEntity<?> verifyInfo(@PathVariable("uuid") String uuid) throws URISyntaxException {
		Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
		if (apiOpt.isEmpty())
			return ResponseEntity.notFound().build();

		Api api = apiOpt.get();
		String token = apiService.getVerifyToken(UUID.fromString(uuid));
		if (token == null)
			return ResponseEntity.status(410).body(Map.of("message", "검증 토큰이 만료되었습니다. API를 다시 등록해주세요."));

		URI uri = new URI(api.getOriginalUrl());
		String base = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
		return ResponseEntity.ok(Map.of("token", token, "wellKnownUrl", base + "/.well-known/getapi-verify",
				"originUrl", api.getOriginalUrl()));
	}

	// .well-known 검증
	@PostMapping("/verify/wellknown/{uuid}")
	@ResponseBody
	public ResponseEntity<?> wellKnownVerify(@PathVariable("uuid") String uuid) throws IOException {
		String sub = SecurityContextHolder.getContext().getAuthentication().getName();

		Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
		if (apiOpt.isEmpty())
			return ResponseEntity.notFound().build();

		Api api = apiOpt.get();

		if (!api.getUser().getProviderId().equals(sub)) {
			return ResponseEntity.status(403).body(Map.of("message", "소유자가 아닙니다."));
		}

		String token = apiService.getVerifyToken(UUID.fromString(uuid));
		if (token == null)
			return ResponseEntity.status(410).body(Map.of("message", "검증 토큰이 만료되었습니다. API를 다시 등록해주세요."));

		boolean ok;
		try {
			ok = apiService.verifyWellKnown(api, token);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("message", "서버에 연결할 수 없습니다: " + e.getMessage()));
		}

		if (!ok) {
			return ResponseEntity.badRequest().body(Map.of("message", "토큰이 일치하지 않습니다. 설정을 확인하세요."));
		}

		apiService.deleteVerifyToken(UUID.fromString(uuid));
		apiService.activate(api);

		SseEmitter emitter = verifyEmitters.get(uuid);
		if (emitter != null) {
			try {
				emitter.send(SseEmitter.event().name("verified").data("ok"));
				emitter.complete();
			} catch (IOException e) {
				emitter.completeWithError(e);
			}
			verifyEmitters.remove(uuid);
		}

		return ResponseEntity.ok(Map.of("message", "verified"));
	}

}
