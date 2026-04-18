package com.masterclass.multiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.masterclass.multiagent", "com.masterclass.shared"})
public class MultiAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiAgentApplication.class, args);
    }
}
