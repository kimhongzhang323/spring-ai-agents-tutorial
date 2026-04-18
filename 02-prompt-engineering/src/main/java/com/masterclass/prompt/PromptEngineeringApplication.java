package com.masterclass.prompt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.masterclass.prompt", "com.masterclass.shared"})
public class PromptEngineeringApplication {
    public static void main(String[] args) {
        SpringApplication.run(PromptEngineeringApplication.class, args);
    }
}
