package com.masterclass.memory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.masterclass.memory", "com.masterclass.shared"})
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemoryApplication.class, args);
    }
}
