package com.getapi.auth.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.getapi.auth.domain.DeviceCode;

public interface DeviceCodeRepository extends CrudRepository<DeviceCode, String> {
    Optional<DeviceCode> findByUserCode(String userCode);
}
