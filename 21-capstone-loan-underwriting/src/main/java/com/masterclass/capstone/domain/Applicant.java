package com.masterclass.capstone.domain;

public record Applicant(
        String applicantId,
        String fullName,
        int age,
        String employmentStatus,
        String employer,
        int yearsAtEmployer,
        double statedAnnualIncome,
        int creditScore,
        double existingMonthlyDebt,
        String addressState
) {}
