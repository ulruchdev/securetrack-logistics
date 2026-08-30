package com.cbs.logistics.package_service;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.cbs.logistics.package_service", "com.cbs.logistics.common.security"})
public class PackageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PackageServiceApplication.class,args);


    }
}