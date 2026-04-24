package com.masterclass.capstone.tool;

import com.masterclass.capstone.repository.ApplicantRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FraudCheckTool {

    private final ApplicantRepository applicants;

    public FraudCheckTool(ApplicantRepository applicants) {
        this.applicants = applicants;
    }

    public record FraudScore(int score, List<String> signals) {}

    @Tool(description = """
            Evaluate fraud risk for the applicant. Returns an integer score 0–100
            (higher = more suspicious) and a list of contributing signals such as
            identity mismatches, synthetic-identity heuristics, or address anomalies.
            """)
    public FraudScore screenForFraud(
            @ToolParam(description = "The internal applicant ID") String applicantId) {

        var a = applicants.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown applicant: " + applicantId));

        List<String> signals = new ArrayList<>();
        int score = 10;

        // Income-to-credit-score mismatch heuristic
        if (a.statedAnnualIncome() > 150_000 && a.creditScore() < 680) {
            score += 35;
            signals.add("Income/credit mismatch: high stated income with subprime FICO");
        }
        // Contract work with very high income is a known synthetic-identity vector
        if ("CONTRACT".equals(a.employmentStatus()) && a.statedAnnualIncome() > 180_000) {
            score += 25;
            signals.add("High contract income, short tenure");
        }
        // Short tenure + large debt load
        if (a.yearsAtEmployer() < 2 && a.existingMonthlyDebt() > 2000) {
            score += 20;
            signals.add("Short employment tenure with high existing debt");
        }
        return new FraudScore(Math.min(score, 100), signals);
    }
}
