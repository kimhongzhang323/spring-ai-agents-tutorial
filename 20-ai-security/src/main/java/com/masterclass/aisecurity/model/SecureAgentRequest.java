package com.masterclass.aisecurity.model;

import jakarta.validation.constraints.NotBlank;

public record SecureAgentRequest(@NotBlank String message) {}
