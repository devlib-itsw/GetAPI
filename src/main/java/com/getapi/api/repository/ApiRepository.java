package com.getapi.api.repository;

import com.getapi.api.domain.Api;
import com.getapi.user.domain.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiRepository extends JpaRepository<Api, Long>, JpaSpecificationExecutor<Api> {
    List<Api> findByUserAndIsCensoredFalse(Users user);
    Optional<Api> findByApiUuid(UUID uuid);
    boolean existsByProxyUrl(String proxyUrl);
    boolean existsByOriginalUrl(String originalUrl);
    Optional<Api> findByProxyUrl(String proxyUrl);
    Page<Api> findByIsCensoredTrue(Pageable pageable);
    void deleteByApiUuidAndIsCensoredTrue(UUID uuid);

    @Query("SELECT COALESCE(SUM(a.viewCount), 0) FROM Api a WHERE a.isCensored = false")
    long sumViewCount();

    @Query(value = "SELECT a.* FROM api a LEFT JOIN star s ON a.api_id = s.api_id WHERE a.is_censored = false GROUP BY a.api_id ORDER BY COUNT(s.star_id) DESC, a.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM api WHERE is_censored = false",
           nativeQuery = true)
    Page<Api> findAllOrderByStars(Pageable pageable);
}
