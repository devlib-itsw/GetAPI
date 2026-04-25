package com.getapi.proxy.service;

import com.getapi.api.domain.Api;
import com.getapi.proxy.domain.CallLog;
import com.getapi.proxy.repository.CallLogRepository;
import com.getapi.user.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CallLogService {

    private final CallLogRepository callLogRepository;
    private final StringRedisTemplate redisTemplate;

    public void record(Api api, Users user, int statusCode, String method, long pointsCharged, long responseTimeMs) {
        // DB 저장
        CallLog log = new CallLog();
        log.setApi(api);
        log.setCalledUser(user);
        log.setStatusCode(statusCode);
        log.setHttpMethod(method);
        log.setPointsCharged(pointsCharged);
        log.setResponseTimeMs(responseTimeMs);
        log.setCalledAt(LocalDateTime.now());
        callLogRepository.save(log);

        // Redis 카운터 증가
        String apiId = api.getApiId().toString();
        String today = LocalDate.now().toString();

        redisTemplate.opsForValue().increment("calls:total:" + apiId);

        String todayKey = "calls:today:" + apiId + ":" + today;
        redisTemplate.opsForValue().increment(todayKey);
        // 오늘 자정까지 TTL
        redisTemplate.expire(todayKey, Duration.between(LocalDateTime.now(), LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)));

        redisTemplate.opsForValue().increment("revenue:total:" + apiId, pointsCharged);
    }

    public long getTotalCalls(Api api) {
        String key = "calls:total:" + api.getApiId();
        String val = redisTemplate.opsForValue().get(key);
        if (val != null) return Long.parseLong(val);

        // Redis miss → DB에서 복구
        long count = callLogRepository.countByApi(api);
        redisTemplate.opsForValue().set(key, String.valueOf(count));
        return count;
    }

    public long getTodayCalls(Api api) {
        String today = LocalDate.now().toString();
        String key = "calls:today:" + api.getApiId() + ":" + today;
        String val = redisTemplate.opsForValue().get(key);
        if (val != null) return Long.parseLong(val);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long count = callLogRepository.countByApiAndCalledAtBetween(api, startOfDay, endOfDay);
        redisTemplate.opsForValue().set(key, String.valueOf(count),
                Duration.between(LocalDateTime.now(), LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT)));
        return count;
    }

    public List<CallLog> getRecentLogs(Api api, int limit) {
        return callLogRepository.findByApiOrderByCalledAtDesc(api, PageRequest.of(0, limit)).getContent();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, Object> getLogPage(Api api, int page, int size) {
        Page<CallLog> p = callLogRepository.findByApiOrderByCalledAtDesc(api, PageRequest.of(page, size));
        List<Map<String, Object>> content = p.getContent().stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("email", log.getCalledUser().getEmail());
            m.put("statusCode", log.getStatusCode());
            m.put("method", log.getHttpMethod());
            m.put("responseTimeMs", log.getResponseTimeMs());
            m.put("pointsCharged", log.getPointsCharged());
            m.put("calledAt", log.getCalledAt().toString().replace("T", " ").substring(0, 19));
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalPages", p.getTotalPages());
        result.put("totalElements", p.getTotalElements());
        result.put("number", p.getNumber());
        return result;
    }

    public long getTotalRevenue(Api api) {
        String key = "revenue:total:" + api.getApiId();
        String val = redisTemplate.opsForValue().get(key);
        if (val != null) return Long.parseLong(val);

        long revenue = callLogRepository.sumPointsChargedByApi(api);
        redisTemplate.opsForValue().set(key, String.valueOf(revenue));
        return revenue;
    }
}
