package com.masterclass.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.masterclass.support", "com.masterclass.shared"})
public class CustomerSupportApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerSupportApplication.class, args);
    }
}
