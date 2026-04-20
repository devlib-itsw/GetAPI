package com.getapi.api.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.getapi.api.domain.Api;

public interface ApiRepository extends JpaRepository<Api, Long> {
	Api findByApiUuid(UUID uuid);
	
	Page<Api> findByIsCensoredTrue(Pageable page);
	
	void deleteByApiUuidAndIsCensoredTrue(UUID uuid);
}
