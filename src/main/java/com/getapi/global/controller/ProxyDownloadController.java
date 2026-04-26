package com.getapi.global.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/download")
public class ProxyDownloadController {

    private static final String GITHUB_BASE =
            "https://github.com/devlib-itsw/getapi-client/releases/latest/download/";

    private static final Map<String, String> FILE_MAP = Map.of(
            "linux_amd64",   "getapi-proxy_linux_amd64.tar.gz",
            "linux_arm64",   "getapi-proxy_linux_arm64.tar.gz",
            "darwin_amd64",  "getapi-proxy_darwin_amd64.tar.gz",
            "darwin_arm64",  "getapi-proxy_darwin_arm64.tar.gz",
            "windows_amd64", "getapi-proxy_windows_amd64.zip"
    );

    private static final Set<String> ZIP_KEYS = Set.of("windows_amd64");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> download(
            @RequestParam("os") String os,
            @RequestParam(value = "arch", defaultValue = "amd64") String arch) {

        String key = os.toLowerCase() + "_" + arch.toLowerCase();
        String filename = FILE_MAP.get(key);
        if (filename == null) {
            return ResponseEntity.badRequest()
                    .body(("지원하지 않는 OS/아키텍처입니다: " + key).getBytes());
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_BASE + filename))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<byte[]> resp;
        try {
            resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(502)
                    .body("GitHub에서 파일을 가져올 수 없습니다.".getBytes());
        }

        if (resp.statusCode() != 200) {
            return ResponseEntity.status(502)
                    .body(("GitHub에서 파일을 가져올 수 없습니다. 상태 코드: " + resp.statusCode()).getBytes());
        }

        MediaType contentType = ZIP_KEYS.contains(key)
                ? MediaType.parseMediaType("application/zip")
                : MediaType.parseMediaType("application/gzip");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(contentType)
                .body(resp.body());
    }
}
