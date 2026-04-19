package com.masterclass.knowledgegraph.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResumeRequest(
        @NotBlank String threadId,
        @NotBlank @Pattern(regexp = "approve|reject", message = "response must be 'approve' or 'reject'")
        String response
) {}
