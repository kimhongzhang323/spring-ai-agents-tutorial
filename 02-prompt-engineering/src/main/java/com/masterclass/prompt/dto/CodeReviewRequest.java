package com.masterclass.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeReviewRequest(
        @NotBlank @Size(max = 8000) String code,
        @NotBlank @Size(max = 50)   String language
) {}
