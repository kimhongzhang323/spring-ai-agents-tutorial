package com.masterclass.microservices.orchestrator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRequest(
        @NotBlank @Size(max = 4000) String message,
        String integrationHint  // optional: "kafka", "rabbitmq", "postgres", etc.
) {}
