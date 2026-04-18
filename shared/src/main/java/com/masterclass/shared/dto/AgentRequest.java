package com.masterclass.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRequest(
        @NotBlank(message = "message must not be blank")
        @Size(max = 4000, message = "message must not exceed 4000 characters")
        String message,

        String conversationId
) {
    public AgentRequest(String message) {
        this(message, null);
    }
}
