package com.getapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableFeignClients	// 결제 할떄 넣은거@EnableFeignClients를 붙여야 스프링이 프로젝트 내부의 @FeignClient가 붙은 인터페이스들을 다 찾아내서 실제 구현체로 만들어줍니다.
@EnableScheduling
@EnableJpaRepositories(basePackages = {
	    "com.getapi.payments",
	    "com.getapi.library.repository"
	})
@SpringBootApplication
public class GetAPIApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetAPIApplication.class, args);
	}

}
