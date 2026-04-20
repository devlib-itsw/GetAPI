package com.getapi.api.repository;

import com.getapi.api.domain.Api;
import com.getapi.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiRepository extends JpaRepository<Api, Long> {
    List<Api> findByUser(Users user);
    Optional<Api> findByApiUuid(UUID uuid);
    boolean existsByProxyUrl(String proxyUrl);
    boolean existsByOriginalUrl(String originalUrl);
    Optional<Api> findByProxyUrl(String proxyUrl);
}
