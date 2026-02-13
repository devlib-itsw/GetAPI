package com.getapi.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.getapi.auth.repository")
@EnableJpaRepositories(basePackages = "com.getapi.user.repository")
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 기본값은 localhost:6379입니다. 
        // 외부 서버를 쓴다면 여기에 설정을 넣습니다.
        return new LettuceConnectionFactory();
    }
}