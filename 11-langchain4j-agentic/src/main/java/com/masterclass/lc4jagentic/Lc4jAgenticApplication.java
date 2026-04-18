package com.masterclass.lc4jagentic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.masterclass.lc4jagentic", "com.masterclass.shared"})
public class Lc4jAgenticApplication {
    public static void main(String[] args) {
        SpringApplication.run(Lc4jAgenticApplication.class, args);
    }
}
