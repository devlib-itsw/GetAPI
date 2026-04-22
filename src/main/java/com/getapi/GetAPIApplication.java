package com.getapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// <<<<<<< siwoo
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
// =======
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
// >>>>>>> develop
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableFeignClients	// 결제 할떄 넣은거@EnableFeignClients를 붙여야 스프링이 프로젝트 내부의 @FeignClient가 붙은 인터페이스들을 다 찾아내서 실제 구현체로 만들어줍니다.
@EnableScheduling
@EnableJpaRepositories(basePackages = {
	    "com.getapi.payments",
	    "com.getapi.library.repository"
	})
@SpringBootApplication
@EnableJpaRepositories(
    basePackages = {
        "com.getapi.post.repository",
        "com.getapi.tag.repository",
        "com.getapi.comment.repository",
        "com.getapi.user.repository",
        "com.getapi.auth.repository",
        "com.getapi.api.repository"
    }
)
@EnableRedisRepositories(basePackages = "com.getapi.redis")
public class GetAPIApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetAPIApplication.class, args);
	}

}
