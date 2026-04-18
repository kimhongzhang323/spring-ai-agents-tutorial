package com.masterclass.deployment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.masterclass.deployment", "com.masterclass.shared"})
public class DeploymentDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeploymentDemoApplication.class, args);
    }
}
