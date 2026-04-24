package com.masterclass.capstone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.masterclass.capstone", "com.masterclass.shared"})
public class CapstoneApplication {
    public static void main(String[] args) {
        SpringApplication.run(CapstoneApplication.class, args);
    }
}
