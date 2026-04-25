package com.getapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "com.getapi.user.repository",
    "com.getapi.auth.repository",
    "com.getapi.api.repository",
    "com.getapi.comment.repository",
    "com.getapi.library.repository",
    "com.getapi.payments.repository",
    "com.getapi.post.repository",
    "com.getapi.proxy.repository",
    "com.getapi.tag.repository",
    "com.getapi.ai.repository"
})
public class GetAPIApplication {

    public static void main(String[] args) {
        SpringApplication.run(GetAPIApplication.class, args);
    }
}
