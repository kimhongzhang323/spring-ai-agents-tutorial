package com.masterclass.tools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.masterclass.tools", "com.masterclass.shared"})
public class ToolCallingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ToolCallingApplication.class, args);
    }
}
