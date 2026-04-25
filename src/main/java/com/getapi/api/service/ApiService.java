package com.getapi.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getapi.admin.domain.AdminCensoredResponse;
import com.getapi.ai.service.AiCensorAsyncService;
import com.getapi.api.domain.Api;
import com.getapi.api.domain.ApiTagMapping;
import com.getapi.api.repository.ApiRepository;
import com.getapi.api.repository.ApiTagMappingRepository;
import com.getapi.api.repository.StarRepository;
import com.getapi.auth.util.SecureUtil;
import com.getapi.comment.repository.ApiCommentRepository;
import com.getapi.proxy.repository.CallLogRepository;
import com.getapi.tag.domain.Tag;
import com.getapi.tag.repository.TagRepository;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ApiService {

    private final ApiRepository apiRepository;
    private final StringRedisTemplate redisTemplate;
    private final AiCensorAsyncService aiCensorAsyncService;
    private final TagRepository tagRepository;
    private final ApiTagMappingRepository apiTagMappingRepository;
    private final StarRepository starRepository;
    private final ApiCommentRepository apiCommentRepository;
    private final CallLogRepository callLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final String VERIFY_PREFIX = "getapi:verify:";
    private static final String PENDING_PREFIX = "getapi:pending:";
    public static final String UPLOAD_DIR = "uploads/api-docs/";

    // ── API 생성 (Redis에 임시 저장) ──────────────────────────────────────────

    public UUID createApi(Users user, String name, String method, String slug, String originUri,
                          String description, Long points, MultipartFile docFile,
                          String tags) throws IOException {

        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String fileName = UUID.randomUUID() + ".md";
        Files.write(Paths.get(UPLOAD_DIR + fileName), docFile.getBytes());

        String proxyUrl = (slug == null || slug.isBlank()) ? SecureUtil.generate64Token() : slug;

        validateOriginUrl(originUri);

        if (apiRepository.existsByProxyUrl(proxyUrl))
            throw new IllegalArgumentException("이미 사용 중인 URL Slug입니다.");
        if (apiRepository.existsByOriginalUrl(originUri))
            throw new IllegalArgumentException("이미 사용 중인 원 API URL입니다.");

        UUID uuid = UUID.randomUUID();

        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("userId", user.getUserId());
        pending.put("userProviderId", user.getProviderId());
        pending.put("name", name);
        pending.put("method", method);
        pending.put("originalUrl", originUri);
        pending.put("proxyUrl", proxyUrl);
        pending.put("description", description);
        pending.put("docUrl", fileName);
        pending.put("price", points);
        List<String> tagList = (tags != null && !tags.isBlank())
                ? Arrays.stream(tags.split(",")).map(String::trim).filter(t -> !t.isEmpty()).collect(java.util.stream.Collectors.toList())
                : List.of();
        pending.put("tags", tagList);

        try {
            redisTemplate.opsForValue().set(PENDING_PREFIX + uuid,
                    objectMapper.writeValueAsString(pending), 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new IOException("Redis 저장 실패: " + e.getMessage(), e);
        }

        String token = "getapi_verify_" + SecureUtil.generate64Token();
        redisTemplate.opsForValue().set(VERIFY_PREFIX + uuid, token, 10, TimeUnit.MINUTES);

        return uuid;
    }

    public Map<String, Object> getPendingApiData(UUID uuid) {
        String json = redisTemplate.opsForValue().get(PENDING_PREFIX + uuid);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void activate(UUID uuid) {
        Map<String, Object> pending = getPendingApiData(uuid);
        if (pending == null) throw new IllegalStateException("등록 정보가 만료되었습니다.");

        Users user = userRepository.findById(Long.valueOf(pending.get("userId").toString()))
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Api api = new Api();
        api.setApiUuid(uuid);
        api.setCensored(false);
        api.setUser(user);
        api.setName((String) pending.get("name"));
        api.setMethod((String) pending.get("method"));
        api.setOriginalUrl((String) pending.get("originalUrl"));
        api.setProxyUrl((String) pending.get("proxyUrl"));
        api.setDescription((String) pending.get("description"));
        api.setDocUrl((String) pending.get("docUrl"));
        api.setPrice(Long.valueOf(pending.get("price").toString()));
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());

        Api saved = apiRepository.save(api);
        aiCensorAsyncService.checkApi(saved.getApiId(),
                api.getName() + " " + (api.getDescription() != null ? api.getDescription() : ""));

        Object tagsObj = pending.get("tags");
        if (tagsObj instanceof List<?> tagList) {
            for (Object t : tagList) {
                String tagName = t.toString().trim();
                if (tagName.isBlank()) continue;
                Tag tag = new Tag();
                tag.setTagUuid(UUID.randomUUID());
                tag.setTag(tagName);
                tag.setCensored(false);
                tagRepository.save(tag);
                aiCensorAsyncService.checkTag(tag.getTagId(), tagName);
                ApiTagMapping mapping = new ApiTagMapping();
                mapping.setApi(saved);
                mapping.setTag(tag);
                apiTagMappingRepository.save(mapping);
            }
        }

        redisTemplate.delete(PENDING_PREFIX + uuid);
        redisTemplate.delete(VERIFY_PREFIX + uuid);
    }

    // ── 문서 업데이트 ─────────────────────────────────────────────────────────

    @Transactional
    public void updateDoc(Api api, MultipartFile docFile) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String fileName = UUID.randomUUID() + ".md";
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        Files.write(filePath, docFile.getBytes());
        api.setDocUrl(fileName);
        api.setUpdatedAt(LocalDateTime.now());
    }

    // ── 수정 / 삭제 ───────────────────────────────────────────────────────────

    @Transactional
    public void update(Api api, String name, String description, Long price, String slug) {
        api.setName(name);
        api.setDescription(description);
        api.setPrice(price);
        if (slug != null && !slug.isBlank() && !slug.equals(api.getProxyUrl())) {
            if (apiRepository.existsByProxyUrl(slug)) throw new IllegalArgumentException("이미 사용 중인 URL Slug입니다.");
            api.setProxyUrl(slug);
        }
        api.setUpdatedAt(LocalDateTime.now());
        aiCensorAsyncService.checkApi(api.getApiId(), name + " " + (description != null ? description : ""));
    }

    @Transactional
    public void deleteByUuid(UUID uuid, Users owner) {
        apiRepository.findByApiUuid(uuid).ifPresent(api -> {
            if (!api.getUser().getUserId().equals(owner.getUserId()))
                throw new SecurityException("삭제 권한이 없습니다.");
            callLogRepository.deleteByApi(api);
            apiCommentRepository.deleteByApi(api);
            starRepository.deleteByApi(api);
            apiTagMappingRepository.deleteByApi(api);
            apiRepository.delete(api);
        });
    }

    // ── 조회 ─────────────────────────────────────────────────────────────────

    public List<Api> findByUser(Users user) {
        return apiRepository.findByUserAndIsCensoredFalse(user);
    }

    public Optional<Api> findByUuid(UUID uuid) {
        return apiRepository.findByApiUuid(uuid);
    }

    @Transactional
    public void incrementViewCount(Api api) {
        api.setViewCount(api.getViewCount() == null ? 1L : api.getViewCount() + 1);
    }

    // ── 검증 토큰 ─────────────────────────────────────────────────────────────

    public String getVerifyToken(UUID uuid) {
        return redisTemplate.opsForValue().get(VERIFY_PREFIX + uuid);
    }

    public void deleteVerifyToken(UUID uuid) {
        redisTemplate.delete(VERIFY_PREFIX + uuid);
    }

    public boolean isOriginalUrlJson(Api api) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(api.getOriginalUrl()).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod(api.getMethod());
        conn.connect();
        String contentType = conn.getContentType();
        conn.disconnect();
        return contentType != null && contentType.contains("application/json");
    }

    public boolean verifyWellKnown(Map<String, Object> pending, String token) throws IOException, URISyntaxException {
        String originalUrl = (String) pending.get("originalUrl");
        URI uri = new URI(originalUrl);
        String base = uri.getScheme() + "://" + uri.getHost()
                + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
        String wellKnownUrl = base + "/.well-known/getapi-verify";

        HttpURLConnection conn = (HttpURLConnection) new URL(wellKnownUrl).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != 200) return false;

        String body = new String(conn.getInputStream().readAllBytes()).trim();
        return token.equals(body);
    }

    // ── HATEOAS ────────────────────────────────────────────────────────────────

    public void setHateoasEnabled(Api api, boolean enabled) {
        api.setHateoasEnabled(enabled);
        api.setUpdatedAt(LocalDateTime.now());
        apiRepository.save(api);
    }

    public void saveHateoas(Api api, boolean enabled, String linksJson) {
        api.setHateoasEnabled(enabled);
        api.setHateoasLinks(linksJson);
        api.setUpdatedAt(LocalDateTime.now());
        apiRepository.save(api);
    }

    // ── 어드민 검열 ───────────────────────────────────────────────────────────

    public Page<AdminCensoredResponse> getApisByIsCensoredPage(int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("apiId").descending());
        return apiRepository.findByIsCensoredTrue(pageable)
                .map(api -> new AdminCensoredResponse(
                        api.getApiId(),
                        api.getName(),
                        api.getDescription(),
                        api.getApiUuid().toString(),
                        api.getUpdatedAt(),
                        api.getUser()
                ));
    }

    @Transactional
    public void ignore(UUID uuid) {
        apiRepository.findByApiUuid(uuid).ifPresent(api -> api.setCensored(false));
    }

    @Transactional
    public void delete(UUID uuid) {
        apiRepository.deleteByApiUuidAndIsCensoredTrue(uuid);
    }

    public long totalApis() {
        return apiRepository.count();
    }

    public long totalApiCalls() {
        return apiRepository.sumViewCount();
    }

    // ── SSRF 방지: 내부 주소 차단 ────────────────────────────────────────────

    private void validateOriginUrl(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("잘못된 URL 형식입니다.");
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
            throw new IllegalArgumentException("http 또는 https URL만 허용됩니다.");

        String host = uri.getHost();
        if (host == null || host.isBlank())
            throw new IllegalArgumentException("유효하지 않은 호스트입니다.");

        try {
            InetAddress addr = InetAddress.getByName(host);
            if (isBlockedAddress(addr))
                throw new IllegalArgumentException("내부 네트워크 주소는 등록할 수 없습니다.");
        } catch (UnknownHostException ignored) {
            // 현재 resolve 불가 → Well-Known 검증에서 차단됨
        }
    }

    private boolean isBlockedAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()   // 127.x.x.x
            || addr.isSiteLocalAddress()  // 10.x, 172.16-31.x, 192.168.x
            || addr.isLinkLocalAddress()  // 169.254.x.x
            || addr.isAnyLocalAddress()   // 0.0.0.0
            || addr.isMulticastAddress()) // 224.x ~
            return true;

        byte[] b = addr.getAddress();
        if (b.length != 4) return false; // IPv6는 위 체크로 충분

        int b0 = b[0] & 0xFF;
        int b1 = b[1] & 0xFF;
        int b2 = b[2] & 0xFF;

        // 100.64.0.0/10 — CGNAT
        if (b0 == 100 && b1 >= 64 && b1 <= 127) return true;
        // 192.0.2.0/24, 198.51.100.0/24, 203.0.113.0/24 — 문서용 예약 대역
        if (b0 == 192 && b1 == 0   && b2 == 2)   return true;
        if (b0 == 198 && b1 == 51  && b2 == 100) return true;
        if (b0 == 203 && b1 == 0   && b2 == 113) return true;
        // 198.18.0.0/15 — 벤치마크 대역
        if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;
        // 240.0.0.0/4 — 예약
        if (b0 >= 240) return true;

        return false;
    }
}
