package com.masterclass.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TranslateRequest(
        @NotBlank
        @Size(max = 8000)
        String text,

        @NotBlank String sourceLanguage,
        @NotBlank String targetLanguage
) {}
