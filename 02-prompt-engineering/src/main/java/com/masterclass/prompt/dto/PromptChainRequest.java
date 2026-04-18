package com.masterclass.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromptChainRequest(
        @NotBlank @Size(max = 4000) String input,
        @NotBlank @Size(max = 200)  String analysisContext,
        @NotBlank @Size(max = 200)  String audience
) {}
