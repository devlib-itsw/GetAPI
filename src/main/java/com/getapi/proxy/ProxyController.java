package com.getapi.proxy;

import com.getapi.api.domain.Api;
import com.getapi.api.repository.ApiRepository;
import com.getapi.auth.domain.ApiAuth;
import com.getapi.auth.repository.ApiAuthRepository;
import com.getapi.auth.util.SecureUtil;
import com.getapi.proxy.service.CallLogService;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class ProxyController {

    private final ApiRepository apiRepository;
    private final ApiAuthRepository apiAuthRepository;
    private final UserRepository userRepository;
    private final CallLogService callLogService;

    // 리다이렉트 미추적(SSRF 방지), 쿠키 저장 없음, 커넥션 타임아웃
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 업스트림으로 전송 시 제거할 헤더 — 서버 신원·클라이언트 IP 노출 방지
    private static final Set<String> EXCLUDED_REQUEST_HEADERS = Set.of(
            // hop-by-hop
            "host", "connection", "transfer-encoding", "te",
            "trailer", "upgrade", "proxy-authorization", "proxy-authenticate",
            "content-length",
            // GetAPI 인증 헤더 (업스트림에 노출 불필요)
            "x-getapi-key", "x-getapi-timestamp", "x-getapi-signature",
            // 클라이언트/프록시 신원 노출 헤더
            "x-forwarded-for", "x-forwarded-host", "x-forwarded-proto", "x-forwarded-port",
            "x-real-ip", "forwarded", "via",
            "x-cluster-client-ip", "x-original-forwarded-for",
            "x-original-url", "x-rewrite-url"
    );

    // 업스트림 응답에서 클라이언트에게 전달 시 제거할 헤더 — 원본 서버 정보 은닉
    private static final Set<String> EXCLUDED_RESPONSE_HEADERS = Set.of(
            "server", "x-powered-by", "via",
            "x-aspnet-version", "x-aspnetmvc-version",
            "x-generator", "x-drupal-cache",
            "x-varnish", "x-cache", "x-cache-hits",
            "x-served-by", "x-timer", "x-runtime"
    );

    @RequestMapping("/lib/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {

        // 1. API Key 인증
        String apiKey = request.getHeader("X-GetAPI-Key");
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("X-GetAPI-Key 헤더가 필요합니다.".getBytes());
        }

        Optional<ApiAuth> authOpt = apiAuthRepository.findByApiKey(apiKey);
        if (authOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("유효하지 않은 API Key입니다.".getBytes());
        }

        ApiAuth apiAuth = authOpt.get();
        if (apiAuth.getExpiredDate() != null && apiAuth.getExpiredDate().isBefore(LocalDate.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("만료된 API Key입니다.".getBytes());
        }

        // 2. 서명 검증
        String timestamp = request.getHeader("X-GetAPI-Timestamp");
        String signature = request.getHeader("X-GetAPI-Signature");
        if (timestamp == null || signature == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("서명 헤더가 필요합니다.".getBytes());
        }

        byte[] body = request.getInputStream().readAllBytes();

        try {
            SecureUtil.validateRequest(apiAuth.getSecretKey(), body, timestamp, signature);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage().getBytes());
        }

        // 4. slug 추출
        String fullPath = request.getRequestURI();
        String afterProxy = fullPath.substring("/lib/".length());
        int slashIdx = afterProxy.indexOf('/');
        String slug = slashIdx == -1 ? afterProxy : afterProxy.substring(0, slashIdx);

        // 5. API 조회
        Optional<Api> apiOpt = apiRepository.findByProxyUrl(slug);
        if (apiOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("등록된 API를 찾을 수 없습니다.".getBytes());
        }

        Api api = apiOpt.get();
        if (api.isCensored()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("검열된 API입니다.".getBytes());
        }

        // 6. 포인트 차감
        Users user = apiAuth.getUser();
        long price = api.getPrice();
        if (user.getPoint() < price) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body("포인트가 부족합니다.".getBytes());
         }
        user.setPoint(user.getPoint() - price);
        userRepository.save(user);

        // 7. 대상 URL 조합 및 스킴 검증
        String queryString = request.getQueryString();
        String targetUrl = api.getOriginalUrl() + (queryString != null ? "?" + queryString : "");
        URI targetUri = URI.create(targetUrl);
        String scheme = targetUri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("http 또는 https 프로토콜만 허용됩니다.".getBytes());
        }

        // 8. HttpRequest 빌드 — 메서드 · 헤더 · 바디 그대로 전달
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(targetUri)
                .method(request.getMethod(),
                        body.length > 0
                                ? HttpRequest.BodyPublishers.ofByteArray(body)
                                : HttpRequest.BodyPublishers.noBody());

        Collections.list(request.getHeaderNames()).forEach(name -> {
            if (!EXCLUDED_REQUEST_HEADERS.contains(name.toLowerCase())) {
                Collections.list(request.getHeaders(name))
                        .forEach(value -> builder.header(name, value));
            }
        });

        HttpRequest httpRequest = builder.build();

        // 8. 포워딩
        HttpResponse<byte[]> upstream;
        long startTime = System.currentTimeMillis();
        try {
            upstream = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("업스트림 연결 실패".getBytes());
        }
        long responseTimeMs = System.currentTimeMillis() - startTime;

        // 9. 호출 로그 저장
        callLogService.record(api, user, upstream.statusCode(), request.getMethod(), price, responseTimeMs);

        // 10. 응답 헤더 복사 (hop-by-hop 제외)
        HttpHeaders responseHeaders = new HttpHeaders();
        upstream.headers().map().forEach((name, values) -> {
            if (!EXCLUDED_RESPONSE_HEADERS.contains(name.toLowerCase())) {
                responseHeaders.addAll(name, values);
            }
        });

        // 11. HATEOAS _links 주입
        byte[] responseBody = upstream.body();
        String contentType = upstream.headers().firstValue("content-type").orElse("");
        if (api.isHateoasEnabled()
                && contentType.contains("application/json")
                && api.getHateoasLinks() != null
                && !api.getHateoasLinks().isBlank()) {
            try {
                Map<String, Object> json = objectMapper.readValue(responseBody, Map.class);
                List<Map<String, String>> linkList = objectMapper.readValue(api.getHateoasLinks(), List.class);
                Map<String, Object> linksMap = new LinkedHashMap<>();
                for (Map<String, String> link : linkList) {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("href", link.get("uri"));
                    entry.put("method", link.get("method"));
                    linksMap.put(link.get("rel"), entry);
                }
                json.put("_links", linksMap);
                responseBody = objectMapper.writeValueAsBytes(json);
                responseHeaders.setContentType(MediaType.valueOf("application/hal+json"));
            } catch (Exception ignored) {
                // JSON 파싱 실패 시 원본 그대로
            }
        }

        return ResponseEntity.status(upstream.statusCode())
                .headers(responseHeaders)
                .body(responseBody);
    }
}
