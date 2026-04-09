package com.vkr.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.vkr.auth.repository")
public class VkrApplication {
    public static void main(String[] args) {
        SpringApplication.run(VkrApplication.class, args);
    }
}