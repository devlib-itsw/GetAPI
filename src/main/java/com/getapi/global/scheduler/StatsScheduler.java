package com.getapi.global.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.getapi.api.service.ApiService;
import com.getapi.payments.service.PaymentsService;
import com.getapi.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsScheduler {

    static final String KEY_TOTAL_APIS       = "stats:totalApis";
    static final String KEY_TOTAL_USERS      = "stats:totalUsers";
    static final String KEY_TOTAL_API_CALLS  = "stats:totalApiCalls";
    static final String KEY_TOTAL_POINTS     = "stats:totalPoints";

    private final ApiService apiService;
    private final UserService userService;
    private final PaymentsService paymentsService;
    private final StringRedisTemplate redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initStats() {
        updateStats();
    }

    @Scheduled(cron = "0 0 0 * * MON")
    public void weeklyUpdate() {
        updateStats();
    }

    private void updateStats() {
        try {
            long totalApis     = apiService.totalApis();
            long totalUsers    = userService.totalUsers();
            long totalApiCalls = apiService.totalApiCalls();
            Long points        = paymentsService.totalPoints();
            long totalPoints   = points != null ? points : 0L;

            redisTemplate.opsForValue().set(KEY_TOTAL_APIS,      String.valueOf(totalApis));
            redisTemplate.opsForValue().set(KEY_TOTAL_USERS,     String.valueOf(totalUsers));
            redisTemplate.opsForValue().set(KEY_TOTAL_API_CALLS, String.valueOf(totalApiCalls));
            redisTemplate.opsForValue().set(KEY_TOTAL_POINTS,    String.valueOf(totalPoints));

            log.info("[Stats] APIs={}, Users={}, Calls={}, Points={}", totalApis, totalUsers, totalApiCalls, totalPoints);
        } catch (Exception e) {
            log.error("[Stats] updateStats 실패", e);
        }
    }
}
