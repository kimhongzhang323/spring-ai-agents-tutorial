package com.masterclass.knowledgegraph.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryRequest(
        @NotBlank(message = "query must not be blank")
        @Size(max = 500, message = "query must not exceed 500 characters")
        String query
) {}
