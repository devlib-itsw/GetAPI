package com.getapi.api.controller;

import com.getapi.api.domain.Api;
import com.getapi.api.domain.ApiTagMapping;
import com.getapi.api.service.ApiService;
import com.getapi.api.service.StarService;
import com.getapi.comment.domain.ApiComment;
import com.getapi.comment.service.ApiCommentService;
import com.getapi.proxy.service.CallLogService;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final ApiService apiService;
    private final UserRepository userRepository;
    private final CallLogService callLogService;
    private final StarService starService;
    private final ApiCommentService apiCommentService;

    private final Map<String, SseEmitter> verifyEmitters = new ConcurrentHashMap<>();

    // ─── 페이지 ───────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByProviderId(sub);
        if (user == null) return "redirect:/login";

        List<Api> apis = apiService.findByUser(user);
        long totalCalls = 0, todayCalls = 0, totalRevenue = 0;
        List<Map<String, Object>> apiStats = new ArrayList<>();

        for (Api api : apis) {
            long tc  = callLogService.getTotalCalls(api);
            long tdc = callLogService.getTodayCalls(api);
            long tr  = callLogService.getTotalRevenue(api);
            totalCalls  += tc;
            todayCalls  += tdc;
            totalRevenue += tr;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("api", api);
            m.put("totalCalls", tc);
            m.put("todayCalls", tdc);
            m.put("totalRevenue", tr);
            apiStats.add(m);
        }

        model.addAttribute("apiStats", apiStats);
        model.addAttribute("totalCalls", totalCalls);
        model.addAttribute("todayCalls", todayCalls);
        model.addAttribute("totalRevenue", totalRevenue);
        return "dashboard";
    }

    @PostMapping("/dashboard/list")
    @ResponseBody
    public ResponseEntity<?> dashboardList(@RequestBody Map<String, Object> body) {
        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByProviderId(sub);
        if (user == null) return ResponseEntity.status(401).build();

        String keyword = body.getOrDefault("keyword", "").toString().toLowerCase();
        String sort    = body.getOrDefault("sort", "revenue").toString();
        int page       = Integer.parseInt(body.getOrDefault("page", 0).toString());
        int size       = 8;

        List<Api> apis = apiService.findByUser(user);

        List<Map<String, Object>> stats = apis.stream().map(api -> {
            long tc  = callLogService.getTotalCalls(api);
            long tdc = callLogService.getTodayCalls(api);
            long tr  = callLogService.getTotalRevenue(api);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("apiUuid",      api.getApiUuid());
            m.put("name",         api.getName());
            m.put("price",        api.getPrice());
            m.put("totalCalls",   tc);
            m.put("todayCalls",   tdc);
            m.put("totalRevenue", tr);
            return m;
        }).collect(Collectors.toList());

        if (!keyword.isBlank()) {
            stats = stats.stream()
                .filter(m -> m.get("name").toString().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        Comparator<Map<String, Object>> cmp = switch (sort) {
            case "calls"   -> Comparator.comparingLong(m -> -((Long) m.get("totalCalls")));
            case "name"    -> Comparator.comparing(m  -> m.get("name").toString());
            default        -> Comparator.comparingLong(m -> -((Long) m.get("totalRevenue")));
        };
        stats.sort(cmp);

        int total    = stats.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int from     = Math.min(page * size, total);
        int to       = Math.min(from + size, total);
        List<Map<String, Object>> content = stats.subList(from, to);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content",    content);
        result.put("totalPages", totalPages);
        result.put("number",     page);
        result.put("totalElements", total);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dashboard/create")
    public String create(Model model) {
        return "dashboard-create";
    }

    @GetMapping("/dashboard/view/{uuid}")
    public String view(@PathVariable("uuid") String uuid, Model model) {
        Optional<Api> api = apiService.findByUuid(UUID.fromString(uuid));
        if (api.isEmpty()) return "redirect:/api/dashboard";

        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users owner = userRepository.findByProviderId(sub);
        if (owner == null || !api.get().getUser().getUserId().equals(owner.getUserId()))
            return "redirect:/api/dashboard";

        model.addAttribute("api", api.get());
        model.addAttribute("totalCalls",  callLogService.getTotalCalls(api.get()));
        model.addAttribute("todayCalls",  callLogService.getTodayCalls(api.get()));
        model.addAttribute("totalRevenue", callLogService.getTotalRevenue(api.get()));
        model.addAttribute("callLogs",    callLogService.getRecentLogs(api.get(), 50));
        return "dashboard-view";
    }

    // ── 문서 서빙 ────────────────────────────────────────────────────────────

    @GetMapping("/doc/{uuid}")
    @ResponseBody
    public ResponseEntity<String> getDoc(@PathVariable("uuid") String uuid) throws IOException {
        Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
        if (apiOpt.isEmpty()) return ResponseEntity.notFound().build();

        String docFileName = apiOpt.get().getDocUrl();
        if (docFileName == null || docFileName.isBlank())
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("# 문서 없음\n\n아직 문서가 등록되지 않았습니다.");

        java.nio.file.Path filePath = Paths.get(ApiService.UPLOAD_DIR + docFileName);
        if (!Files.exists(filePath))
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("# 문서를 찾을 수 없습니다.");

        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(Files.readString(filePath));
    }

    @PostMapping("/dashboard/view/{uuid}/docs")
    @ResponseBody
    public ResponseEntity<?> uploadDoc(@PathVariable("uuid") String uuid,
                                       @RequestParam("docFile") MultipartFile docFile) throws IOException {
        Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
        if (apiOpt.isEmpty()) return ResponseEntity.notFound().build();

        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByProviderId(sub);
        if (user == null || !apiOpt.get().getUser().getUserId().equals(user.getUserId()))
            return ResponseEntity.status(403).build();

        apiService.updateDoc(apiOpt.get(), docFile);
        return ResponseEntity.ok().build();
    }

    // ── API 수정/삭제 ────────────────────────────────────────────────────────

    @PatchMapping("/dashboard/view/{uuid}")
    @ResponseBody
    public ResponseEntity<?> updateApi(@PathVariable("uuid") String uuid,
                                       @RequestBody Map<String, Object> body) {
        Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
        if (apiOpt.isEmpty()) return ResponseEntity.notFound().build();

        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByProviderId(sub);
        if (user == null || !apiOpt.get().getUser().getUserId().equals(user.getUserId()))
            return ResponseEntity.status(403).build();

        try {
            String name        = (String) body.get("name");
            String description = (String) body.get("description");
            Long   price       = Long.parseLong(body.get("price").toString());
            String slug        = (String) body.get("slug");
            apiService.update(apiOpt.get(), name, description, price, slug);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/dashboard/view/{uuid}")
    @ResponseBody
    public ResponseEntity<?> deleteApi(@PathVariable("uuid") String uuid) {
        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByProviderId(sub);
        if (user == null) return ResponseEntity.status(401).build();

        try {
            apiService.deleteByUuid(UUID.fromString(uuid), user);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }
    }

    // ── HATEOAS / 헬스체크 ────────────────────────────────────────────────────

    @PostMapping("/dashboard/view/{uuid}/hateoas/toggle")
    @ResponseBody
    public ResponseEntity<?> toggleHateoas(@PathVariable("uuid") String uuid,
                                           @RequestBody Map<String, Object> body) {
        Optional<Api> api = apiService.findByUuid(UUID.fromString(uuid));
        if (api.isEmpty()) return ResponseEntity.notFound().build();
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        if (enabled) {
            try {
                if (!apiService.isOriginalUrlJson(api.get()))
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "원본 API가 JSON을 반환하지 않아 HATEOAS를 활성화할 수 없습니다."));
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "원본 서버에 연결할 수 없습니다: " + e.getMessage()));
            }
        }
        apiService.setHateoasEnabled(api.get(), enabled);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/dashboard/view/{uuid}/hateoas")
    @ResponseBody
    public ResponseEntity<?> saveHateoas(@PathVariable("uuid") String uuid,
                                         @RequestBody Map<String, Object> body) {
        Optional<Api> api = apiService.findByUuid(UUID.fromString(uuid));
        if (api.isEmpty()) return ResponseEntity.notFound().build();
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        String linksJson = body.get("links") != null ? body.get("links").toString() : "[]";
        apiService.saveHateoas(api.get(), enabled, linksJson);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dashboard/view/{uuid}/logs")
    @ResponseBody
    public ResponseEntity<?> getLogs(@PathVariable("uuid") String uuid,
                                     @RequestParam(name = "page", defaultValue = "0") int page) {
        Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
        if (apiOpt.isEmpty()) return ResponseEntity.notFound().build();

        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByProviderId(sub);
        if (user == null || !apiOpt.get().getUser().getUserId().equals(user.getUserId()))
            return ResponseEntity.status(403).build();

        return ResponseEntity.ok(callLogService.getLogPage(apiOpt.get(), page, 20));
    }

    @RequestMapping(value = "/dashboard/view/{uuid}/health", method = RequestMethod.HEAD)
    @ResponseBody
    public ResponseEntity<?> healthCheck(@PathVariable("uuid") String uuid) {
        Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
        if (apiOpt.isEmpty()) return ResponseEntity.notFound().build();
        try {
            java.net.HttpURLConnection conn =
                    (java.net.HttpURLConnection) new java.net.URL(apiOpt.get().getOriginalUrl()).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("HEAD");
            int code = conn.getResponseCode();
            return code < 500 ? ResponseEntity.ok().build() : ResponseEntity.status(code).build();
        } catch (Exception e) {
            return ResponseEntity.status(502).build();
        }
    }


    // ── Library Comments ──────────────────────────────────────────────────────

    @PostMapping("/library/{uuid}/comments")
    @ResponseBody
    public ResponseEntity<?> createComment(@PathVariable("uuid") String uuid,
                                           @RequestBody Map<String, String> body) {
        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByProviderId(sub);
        if (user == null) return ResponseEntity.status(401).build();

        Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
        if (apiOpt.isEmpty()) return ResponseEntity.notFound().build();

        String content = body.get("content");
        if (content == null || content.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "내용을 입력해주세요."));

        ApiComment c = apiCommentService.create(content, user, apiOpt.get());
        return ResponseEntity.ok(Map.of(
                "commentUuid", c.getCommentUuid().toString(),
                "content", c.getContent(),
                "createdAt", c.getCreatedAt().toString().substring(0, 10)
        ));
    }

    @PutMapping("/library/{uuid}/comments/{commentUuid}")
    @ResponseBody
    public ResponseEntity<?> updateComment(@PathVariable("uuid") String uuid,
                                           @PathVariable("commentUuid") String commentUuid,
                                           @RequestBody Map<String, String> body) {
        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByProviderId(sub);
        if (user == null) return ResponseEntity.status(401).build();

        String content = body.get("content");
        if (content == null || content.isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "내용을 입력해주세요."));

        apiCommentService.update(UUID.fromString(commentUuid), content, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/library/{uuid}/comments/{commentUuid}")
    @ResponseBody
    public ResponseEntity<?> deleteComment(@PathVariable("uuid") String uuid,
                                           @PathVariable("commentUuid") String commentUuid) {
        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        Users user = userRepository.findByProviderId(sub);
        if (user == null) return ResponseEntity.status(401).build();

        apiCommentService.deleteByUuid(UUID.fromString(commentUuid), user);
        return ResponseEntity.ok().build();
    }

    // ── Step1: API 등록 ───────────────────────────────────────────────────────

    @PostMapping("")
    public String createApi(@RequestParam("name") String name,
                            @RequestParam("method") String method,
                            @RequestParam(name = "slug", required = false) String slug,
                            @RequestParam("originUri") String originUri,
                            @RequestParam("description") String description,
                            @RequestParam("points") Long points,
                            @RequestParam("apiDoc") MultipartFile apiDoc,
                            @RequestParam(name = "tags", required = false) String tags,
                            RedirectAttributes redirectAttributes) {
        try {
            String sub = SecurityContextHolder.getContext().getAuthentication().getName();
            Users user = userRepository.findByProviderId(sub);
            if (user == null) return "redirect:/login";

            UUID uuid = apiService.createApi(user, name, method, slug,
                    originUri.replaceAll("/+$", ""), description, points, apiDoc, tags);
            return "redirect:/api/dashboard/verify/" + uuid;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/api/dashboard/create";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "등록 중 오류가 발생했습니다.");
            return "redirect:/api/dashboard/create";
        }
    }

    // ── Step2: 검증 ───────────────────────────────────────────────────────────

    @GetMapping("/dashboard/verify/{uuid}")
    public String verifyPage(@PathVariable("uuid") String uuid, Model model) {
        model.addAttribute("uuid", uuid);
        return "dashboard-verify";
    }

    @GetMapping("/verify/stream/{uuid}")
    @ResponseBody
    public SseEmitter stream(@PathVariable("uuid") String uuid) {
        SseEmitter emitter = new SseEmitter(600_000L);
        verifyEmitters.put(uuid, emitter);
        emitter.onCompletion(() -> verifyEmitters.remove(uuid));
        emitter.onTimeout(() -> verifyEmitters.remove(uuid));
        return emitter;
    }

    @GetMapping("/verify/info/{uuid}")
    @ResponseBody
    public ResponseEntity<?> verifyInfo(@PathVariable("uuid") String uuid) throws URISyntaxException {
        Map<String, Object> pending = apiService.getPendingApiData(UUID.fromString(uuid));
        if (pending == null)
            return ResponseEntity.status(410).body(Map.of("message", "검증 토큰이 만료되었습니다. API를 다시 등록해주세요."));

        String token = apiService.getVerifyToken(UUID.fromString(uuid));
        if (token == null)
            return ResponseEntity.status(410).body(Map.of("message", "검증 토큰이 만료되었습니다. API를 다시 등록해주세요."));

        String originUrl = (String) pending.get("originalUrl");
        URI uri = new URI(originUrl);
        String base = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
        return ResponseEntity.ok(Map.of(
                "token", token,
                "wellKnownUrl", base + "/.well-known/getapi-verify",
                "originUrl", originUrl
        ));
    }

    @PostMapping("/verify/wellknown/{uuid}")
    @ResponseBody
    public ResponseEntity<?> wellKnownVerify(@PathVariable("uuid") String uuid) throws IOException {
        String sub = SecurityContextHolder.getContext().getAuthentication().getName();

        Map<String, Object> pending = apiService.getPendingApiData(UUID.fromString(uuid));
        if (pending == null)
            return ResponseEntity.status(410).body(Map.of("message", "검증 토큰이 만료되었습니다. API를 다시 등록해주세요."));

        if (!sub.equals(pending.get("userProviderId")))
            return ResponseEntity.status(403).body(Map.of("message", "소유자가 아닙니다."));

        String token = apiService.getVerifyToken(UUID.fromString(uuid));
        if (token == null)
            return ResponseEntity.status(410).body(Map.of("message", "검증 토큰이 만료되었습니다. API를 다시 등록해주세요."));

        boolean ok;
        try {
            ok = apiService.verifyWellKnown(pending, token);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "서버에 연결할 수 없습니다: " + e.getMessage()));
        }

        if (!ok) return ResponseEntity.badRequest().body(Map.of("message", "토큰이 일치하지 않습니다. 설정을 확인하세요."));

        try {
            apiService.activate(UUID.fromString(uuid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "등록 중 오류: " + e.getMessage()));
        }

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
