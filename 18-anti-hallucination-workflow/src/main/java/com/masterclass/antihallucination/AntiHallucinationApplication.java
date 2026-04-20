package com.masterclass.antihallucination;

import com.masterclass.antihallucination.config.AntiHallucinationConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AntiHallucinationConfig.class)
public class AntiHallucinationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AntiHallucinationApplication.class, args);
    }
}
