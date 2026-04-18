package com.masterclass.apimgmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.masterclass.apimgmt", "com.masterclass.shared"})
public class ApiManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiManagementApplication.class, args);
    }
}
