package com.masterclass.knowledgegraph.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildRequest(
        @NotBlank(message = "text must not be blank")
        @Size(min = 50, max = 20_000, message = "text must be 50–20,000 characters")
        String text
) {}
