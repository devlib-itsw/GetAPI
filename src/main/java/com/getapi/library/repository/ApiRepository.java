package com.getapi.library.repository;

import com.getapi.api.domain.Api;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApiRepository extends JpaRepository<Api, Long>, JpaSpecificationExecutor<Api> {
}