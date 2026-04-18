package com.masterclass.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExpertChatRequest(
        @NotBlank String domain,
        @NotBlank String audienceLevel,
        int yearsExperience,

        @NotBlank
        @Size(max = 4000)
        String question
) {
    public ExpertChatRequest(String domain, String question) {
        this(domain, "beginner", 10, question);
    }
}
