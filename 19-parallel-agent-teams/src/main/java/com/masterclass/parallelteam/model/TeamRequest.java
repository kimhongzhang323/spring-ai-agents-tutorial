package com.masterclass.parallelteam.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamRequest(
        @NotBlank @Size(min = 5, max = 500) String topic
) {}
