package com.DevLib.repository;

import com.DevLib.domain.SmsAuth;
import org.springframework.data.repository.CrudRepository;

public interface SmsAuthRepository extends CrudRepository<SmsAuth, String> {
}
