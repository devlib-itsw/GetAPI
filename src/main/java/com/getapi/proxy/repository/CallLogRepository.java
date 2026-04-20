package com.getapi.proxy.repository;

import com.getapi.api.domain.Api;
import com.getapi.proxy.domain.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    List<CallLog> findByApiOrderByCalledAtDesc(Api api, Pageable pageable);

    long countByApi(Api api);

    long countByApiAndCalledAtBetween(Api api, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(c.pointsCharged), 0) FROM CallLog c WHERE c.api = :api")
    long sumPointsChargedByApi(@Param("api") Api api);
}
