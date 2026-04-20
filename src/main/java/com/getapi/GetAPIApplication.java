package com.getapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
//JPA 리포지토리가 있는 패키지 경로를 정확히 적어줍니다.
@EnableJpaRepositories(basePackages = "com.getapi")
public class GetAPIApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetAPIApplication.class, args);
	}

}
