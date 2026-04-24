package com.masterclass.capstone.tool;

import com.masterclass.capstone.repository.ApplicantRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class IncomeVerificationTool {

    private final ApplicantRepository applicants;

    public IncomeVerificationTool(ApplicantRepository applicants) {
        this.applicants = applicants;
    }

    public record IncomeVerification(double verifiedAnnualIncome, double statedAnnualIncome,
                                     double discrepancyPercent, String method) {}

    @Tool(description = """
            Verify applicant income via employer records (W-2) or last two years of tax
            returns (for self-employed/contract). Returns verified annual income, the
            applicant's stated income, and the discrepancy percentage (absolute).
            """)
    public IncomeVerification verifyIncome(
            @ToolParam(description = "The internal applicant ID") String applicantId) {

        var a = applicants.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown applicant: " + applicantId));

        // Simulate: W-2 employees verify cleanly; contract/self-employed are haircut
        double verified = switch (a.employmentStatus()) {
            case "FULL_TIME" -> a.statedAnnualIncome() * 1.00;
            case "CONTRACT" -> a.statedAnnualIncome() * 0.75;       // 2-year avg haircut
            case "SELF_EMPLOYED" -> a.statedAnnualIncome() * 0.65;  // tax-return net income
            default -> a.statedAnnualIncome() * 0.90;
        };
        double discrepancy = Math.abs(verified - a.statedAnnualIncome()) / a.statedAnnualIncome() * 100.0;
        String method = "FULL_TIME".equals(a.employmentStatus()) ? "W2_EMPLOYER" : "TAX_RETURN_2YR";
        return new IncomeVerification(verified, a.statedAnnualIncome(), discrepancy, method);
    }
}
