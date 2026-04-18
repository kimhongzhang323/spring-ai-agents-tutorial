package com.masterclass.deployment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/deployment")
@Tag(name = "Deployment", description = "Runtime info and smoke-test endpoint")
public class HealthController {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.profiles.active:default}")
    private String profile;

    @GetMapping("/info")
    @Operation(summary = "Returns runtime profile and version — useful for smoke tests after deploy")
    public Map<String, String> info() {
        return Map.of(
                "app", appName,
                "profile", profile,
                "status", "UP"
        );
    }
}
