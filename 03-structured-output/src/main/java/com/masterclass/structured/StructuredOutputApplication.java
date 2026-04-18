package com.masterclass.structured;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.masterclass.structured", "com.masterclass.shared"})
public class StructuredOutputApplication {
    public static void main(String[] args) {
        SpringApplication.run(StructuredOutputApplication.class, args);
    }
}
