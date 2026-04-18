package com.masterclass.hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.masterclass.hello", "com.masterclass.shared"})
public class HelloAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelloAgentApplication.class, args);
    }
}
