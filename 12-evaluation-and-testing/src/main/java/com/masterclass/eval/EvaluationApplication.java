package com.masterclass.eval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.masterclass.eval", "com.masterclass.shared"})
public class EvaluationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvaluationApplication.class, args);
    }
}
