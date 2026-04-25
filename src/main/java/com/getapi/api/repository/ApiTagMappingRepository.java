package com.getapi.api.repository;

import com.getapi.api.domain.Api;
import com.getapi.api.domain.ApiTagMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTagMappingRepository extends JpaRepository<ApiTagMapping, Long> {
    void deleteByApi(Api api);
}
