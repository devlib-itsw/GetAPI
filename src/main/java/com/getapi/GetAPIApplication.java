package com.getapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GetAPIApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetAPIApplication.class, args);
	}

}
