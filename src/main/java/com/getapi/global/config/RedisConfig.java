package com.getapi.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
//아래와 같이 스캔 범위를 확장하세요
@EnableRedisRepositories(basePackages = {
 "com.getapi.auth.repository", 
 "com.getapi.payments"  // PaymentsRepository가 있는 위치 추가
})
@EnableJpaRepositories(basePackages = "com.getapi.user.repository")
public class RedisConfig {

 @Bean
 public RedisConnectionFactory redisConnectionFactory() {
     return new LettuceConnectionFactory();
 }
}