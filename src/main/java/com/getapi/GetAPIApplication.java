package com.getapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.getapi.user.repository")
public class GetAPIApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetAPIApplication.class, args);
	}

}
