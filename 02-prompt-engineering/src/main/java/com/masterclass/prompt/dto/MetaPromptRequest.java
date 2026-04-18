package com.masterclass.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MetaPromptRequest(
        @NotBlank @Size(max = 1000) String useCase,
        @NotBlank @Size(max = 200)  String targetAudience
) {}
