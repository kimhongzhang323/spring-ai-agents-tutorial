package com.masterclass.research;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.masterclass.research", "com.masterclass.shared"})
public class ResearchAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResearchAgentApplication.class, args);
    }
}
