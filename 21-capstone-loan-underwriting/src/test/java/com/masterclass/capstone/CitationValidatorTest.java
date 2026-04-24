package com.masterclass.capstone;

import com.masterclass.capstone.domain.Finding;
import com.masterclass.capstone.domain.UnderwritingDecision;
import com.masterclass.capstone.guardrails.CitationValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CitationValidatorTest {

    private final CitationValidator validator = new CitationValidator();

    private static final List<Finding> EVIDENCE = List.of(
            new Finding("CR-001", "CREDIT", "FICO 782", Finding.Severity.POSITIVE),
            new Finding("FR-001", "FRAUD", "No fraud signals.", Finding.Severity.INFO),
            new Finding("CO-001", "COMPLIANCE", "Complies with §1.1 prime band.", Finding.Severity.INFO)
    );

    @Test
    void passesWhenAllCitationsAreValid() {
        var decision = new UnderwritingDecision("job-1", "APP-001",
                UnderwritingDecision.Outcome.APPROVED, 250000, 6.5,
                List.of(new UnderwritingDecision.RationaleItem("Strong credit profile.", List.of("CR-001", "FR-001"))),
                EVIDENCE, Instant.now());

        assertThat(validator.validate(decision, EVIDENCE).valid()).isTrue();
    }

    @Test
    void failsWhenCitedIdDoesNotExist() {
        var decision = new UnderwritingDecision("job-2", "APP-001",
                UnderwritingDecision.Outcome.APPROVED, 250000, 6.5,
                List.of(new UnderwritingDecision.RationaleItem("Hallucinated finding.", List.of("CR-999"))),
                EVIDENCE, Instant.now());

        var result = validator.validate(decision, EVIDENCE);
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("CR-999");
    }

    @Test
    void failsWhenDeclineHasNoComplianceCitation() {
        var decision = new UnderwritingDecision("job-3", "APP-004",
                UnderwritingDecision.Outcome.DECLINED, 0, 0,
                List.of(new UnderwritingDecision.RationaleItem("Subprime FICO.", List.of("CR-001"))),
                EVIDENCE, Instant.now());

        var result = validator.validate(decision, EVIDENCE);
        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("§6.2");
    }

    @Test
    void failsWhenRationaleHasNoCitations() {
        var decision = new UnderwritingDecision("job-4", "APP-001",
                UnderwritingDecision.Outcome.APPROVED, 250000, 6.5,
                List.of(new UnderwritingDecision.RationaleItem("Looks good.", List.of())),
                EVIDENCE, Instant.now());

        assertThat(validator.validate(decision, EVIDENCE).valid()).isFalse();
    }
}
