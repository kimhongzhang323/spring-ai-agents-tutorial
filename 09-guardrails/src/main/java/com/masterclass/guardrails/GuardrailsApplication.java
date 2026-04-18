package com.masterclass.guardrails;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.masterclass.guardrails", "com.masterclass.shared"})
public class GuardrailsApplication {
    public static void main(String[] args) {
        SpringApplication.run(GuardrailsApplication.class, args);
    }
}
