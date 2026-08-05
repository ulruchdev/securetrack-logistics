package com.cbs.logistics.security_checkpoint_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SecurityCheckpointServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecurityCheckpointServiceApplication.class, args);
    }
}