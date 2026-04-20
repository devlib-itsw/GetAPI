package com.getapi.api.service;

import com.getapi.api.domain.Api;
import com.getapi.api.repository.ApiRepository;
import com.getapi.auth.util.SecureUtil;
import com.getapi.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ApiService {

    private final ApiRepository apiRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String VERIFY_PREFIX = "getapi:verify:";

    private static final String UPLOAD_DIR = "uploads/api-docs/";

    public Api createApi(Users user, String name, String method, String slug, String originUri,
                         String description, Long points, MultipartFile docFile) throws IOException {

        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String fileName = UUID.randomUUID() + "_" + docFile.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        Files.write(filePath, docFile.getBytes());

        String proxyUrl = (slug == null || slug.isBlank()) ? SecureUtil.generate64Token() : slug;

        if (apiRepository.existsByProxyUrl(proxyUrl)) {
            throw new IllegalArgumentException("이미 사용 중인 URL Slug입니다.");
          }
        
        if (apiRepository.existsByOriginalUrl(originUri)) {
        	throw new IllegalArgumentException("이미 사용 중인 원 API URL입니다.");
        }

        Api api = new Api();
        api.setApiUuid(UUID.randomUUID());
        api.setUser(user);
        api.setName(name);
        api.setMethod(method);
        api.setOriginalUrl(originUri);
        api.setProxyUrl(proxyUrl);
        api.setDescription(description);
        api.setDocUrl(filePath.toString());
        api.setPrice(points);
        api.setStatus("pending");
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());

        Api saved = apiRepository.save(api);

        String token = "getapi_verify_" + SecureUtil.generate64Token();
        redisTemplate.opsForValue().set(VERIFY_PREFIX + saved.getApiUuid(), token, 10, TimeUnit.MINUTES);

        return saved;
    }

    public Optional<Api> findByUuid(UUID uuid) {
        return apiRepository.findByApiUuid(uuid);
    }

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

    public void activate(Api api) {
        api.setStatus("active");
        api.setUpdatedAt(LocalDateTime.now());
        apiRepository.save(api);
    }

    public boolean verifyWellKnown(Api api, String token) throws IOException, URISyntaxException {
        URI uri = new URI(api.getOriginalUrl());
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
}
