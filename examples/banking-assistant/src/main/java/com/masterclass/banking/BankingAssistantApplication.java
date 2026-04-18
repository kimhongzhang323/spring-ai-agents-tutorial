package com.masterclass.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.masterclass.banking", "com.masterclass.shared"})
public class BankingAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingAssistantApplication.class, args);
    }
}
