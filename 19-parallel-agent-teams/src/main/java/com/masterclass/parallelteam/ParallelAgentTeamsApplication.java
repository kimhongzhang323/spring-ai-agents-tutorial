package com.masterclass.parallelteam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ParallelAgentTeamsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParallelAgentTeamsApplication.class, args);
    }
}
