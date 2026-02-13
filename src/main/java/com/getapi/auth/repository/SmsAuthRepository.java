package com.getapi.auth.repository;

import com.getapi.auth.domain.SmsAuth;
import org.springframework.data.repository.CrudRepository;

public interface SmsAuthRepository extends CrudRepository<SmsAuth, String> {
}
