package com.masterclass.capstone.evaluation;

import com.masterclass.capstone.domain.Finding;
import com.masterclass.capstone.domain.UnderwritingDecision;
import com.masterclass.capstone.guardrails.CitationValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden-dataset evaluation (module 12 pattern).
 *
 * These are hand-crafted ground-truth cases that test the citation guardrail
 * and decision logic with synthetic evidence — no LLM required for the test run.
 *
 * In a full CI setup, you'd also have @SpringBootTest cases that spin up the
 * entire pipeline with a WireMocked LLM endpoint and assert that specific
 * applicants (APP-001 to APP-005) receive outcomes matching the policy rules.
 */
class GoldenDatasetIT {

    private final CitationValidator validator = new CitationValidator();

    record Case(String applicantId, List<Finding> evidence, UnderwritingDecision expected) {}

    private static final List<Case> GOLDEN = List.of(

        // APP-001: Maria Chen — prime FICO, full-time, no fraud → APPROVED
        new Case("APP-001",
            List.of(
                new Finding("CR-001", "CREDIT", "FICO 782 — prime band per §1.1.", Finding.Severity.POSITIVE),
                new Finding("FR-001", "FRAUD", "Fraud score 10. No signals.", Finding.Severity.INFO),
                new Finding("IN-001", "INCOME", "Verified income $128,000. No discrepancy.", Finding.Severity.POSITIVE),
                new Finding("CO-001", "COMPLIANCE", "Back-end DTI 27% is below §2.1 max of 43%.", Finding.Severity.INFO)
            ),
            new UnderwritingDecision("eval-1", "APP-001",
                UnderwritingDecision.Outcome.APPROVED, 250_000, 6.5,
                List.of(new UnderwritingDecision.RationaleItem(
                    "Prime credit, verified income, compliant DTI.", List.of("CR-001","IN-001","CO-001"))),
                List.of(), Instant.now())),

        // APP-002: Jordan Reyes — near-prime FICO, high fraud score → REFER_TO_HUMAN
        new Case("APP-002",
            List.of(
                new Finding("CR-001", "CREDIT", "FICO 648 — sub-prime per §1.3.", Finding.Severity.WARNING),
                new Finding("FR-001", "FRAUD", "Fraud score 70. High income/credit mismatch.", Finding.Severity.WARNING),
                new Finding("IN-001", "INCOME", "Contract income haircut: verified $157,500.", Finding.Severity.WARNING),
                new Finding("CO-001", "COMPLIANCE", "Fraud score 50–74 requires manual review per §4.2.", Finding.Severity.WARNING)
            ),
            new UnderwritingDecision("eval-2", "APP-002",
                UnderwritingDecision.Outcome.REFER_TO_HUMAN, 0, 0,
                List.of(new UnderwritingDecision.RationaleItem(
                    "Fraud score in review band per §4.2.", List.of("FR-001","CO-001"))),
                List.of(), Instant.now())),

        // APP-004: Samuel Okafor — FICO 580 (< 620, no compensating factors) → DECLINED
        new Case("APP-004",
            List.of(
                new Finding("CR-001", "CREDIT", "FICO 580 — below 620 threshold per §1.4.", Finding.Severity.BLOCKER),
                new Finding("FR-001", "FRAUD", "Fraud score 30. Low risk.", Finding.Severity.INFO),
                new Finding("IN-001", "INCOME", "Self-employed 2yr avg $113,750.", Finding.Severity.INFO),
                new Finding("CO-001", "COMPLIANCE", "FICO < 620 with no documented compensating factors: auto-decline per §1.4.", Finding.Severity.BLOCKER)
            ),
            new UnderwritingDecision("eval-3", "APP-004",
                UnderwritingDecision.Outcome.DECLINED, 0, 0,
                List.of(new UnderwritingDecision.RationaleItem(
                    "FICO < 620 — auto-decline per §1.4.", List.of("CR-001","CO-001"))),
                List.of(), Instant.now()))
    );

    @Test
    void allGoldenCasesPassCitationValidation() {
        for (var c : GOLDEN) {
            var result = validator.validate(c.expected(), c.evidence());
            assertThat(result.valid())
                    .as("Citation validation failed for %s: %s", c.applicantId(), result.reason())
                    .isTrue();
        }
    }

    @Test
    void blockerFindingsShouldNotProduceApproval() {
        for (var c : GOLDEN) {
            boolean hasBlocker = c.evidence().stream()
                    .anyMatch(f -> f.severity() == Finding.Severity.BLOCKER);
            if (hasBlocker) {
                assertThat(c.expected().outcome())
                        .as("Applicant %s has BLOCKER findings but was APPROVED", c.applicantId())
                        .isNotEqualTo(UnderwritingDecision.Outcome.APPROVED);
            }
        }
    }

    @Test
    void declinedCasesHaveComplianceCitation() {
        GOLDEN.stream()
                .filter(c -> c.expected().outcome() == UnderwritingDecision.Outcome.DECLINED)
                .forEach(c -> {
                    boolean hasComplianceCite = c.expected().rationale().stream()
                            .flatMap(r -> r.citedFindingIds().stream())
                            .anyMatch(id -> id.startsWith("CO-"));
                    assertThat(hasComplianceCite)
                            .as("Declined case %s missing CO-xxx citation", c.applicantId())
                            .isTrue();
                });
    }
}
