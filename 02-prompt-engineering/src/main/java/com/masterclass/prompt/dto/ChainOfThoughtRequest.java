package com.masterclass.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChainOfThoughtRequest(
        @NotBlank @Size(max = 2000)
        String problem
) {}
