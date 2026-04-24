package com.masterclass.capstone.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoanApplication(
        @NotBlank String applicantId,
        @Min(1000) double loanAmount,
        @Min(6) int termMonths,
        @Pattern(regexp = "HOME_PURCHASE|REFINANCE|AUTO|PERSONAL|BUSINESS") String purpose
) {}
