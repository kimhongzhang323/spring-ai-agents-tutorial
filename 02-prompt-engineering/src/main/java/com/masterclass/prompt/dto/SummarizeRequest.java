package com.masterclass.prompt.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SummarizeRequest(
        @NotBlank
        @Size(max = 8000, message = "Text must not exceed 8000 characters")
        String text,

        @Min(20) @Max(500)
        int maxWords
) {
    public SummarizeRequest(String text) {
        this(text, 150);
    }
}
