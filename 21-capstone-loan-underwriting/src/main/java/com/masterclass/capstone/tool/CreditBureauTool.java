package com.masterclass.capstone.tool;

import com.masterclass.capstone.repository.ApplicantRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Simulates a credit-bureau pull. In production this would call Experian/Equifax/TransUnion.
 * Returns deterministic values derived from the seeded applicant so evaluation tests are stable.
 */
@Component
public class CreditBureauTool {

    private final ApplicantRepository applicants;

    public CreditBureauTool(ApplicantRepository applicants) {
        this.applicants = applicants;
    }

    public record CreditPull(int ficoScore, int openTradelines, int delinquencies30d,
                             int delinquencies90d, double revolvingUtilization) {}

    @Tool(description = """
            Pull a consumer credit report for the given applicantId.
            Returns FICO score, open tradelines, 30-day and 90-day delinquency counts,
            and revolving-credit utilization ratio (0.0–1.0). Use this to assess credit risk.
            """)
    public CreditPull pullCreditReport(
            @ToolParam(description = "The internal applicant ID, e.g. APP-001") String applicantId) {

        var a = applicants.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown applicant: " + applicantId));

        // Deterministic derivation from stable attributes — reproducible for tests
        int fico = a.creditScore();
        int openLines = 3 + (fico / 200);
        int delin30 = fico < 650 ? 2 : 0;
        int delin90 = fico < 600 ? 1 : 0;
        double util = fico >= 740 ? 0.18 : fico >= 680 ? 0.42 : 0.71;
        return new CreditPull(fico, openLines, delin30, delin90, util);
    }
}
