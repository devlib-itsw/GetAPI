package com.DevLib.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.DevLib.repository") // 👈 Repository 위치 지정 (필수!)
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 기본값은 localhost:6379입니다. 
        // 외부 서버를 쓴다면 여기에 설정을 넣습니다.
        return new LettuceConnectionFactory();
    }
}