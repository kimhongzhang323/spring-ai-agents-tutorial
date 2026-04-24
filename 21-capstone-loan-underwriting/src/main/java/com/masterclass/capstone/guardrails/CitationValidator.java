package com.masterclass.capstone.guardrails;

import com.masterclass.capstone.domain.Finding;
import com.masterclass.capstone.domain.UnderwritingDecision;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enforces that every rationale item in the supervisor's decision cites at least one
 * finding ID that actually appears in the collected evidence.
 *
 * This is the anti-hallucination gate: if the LLM invents a finding ID (e.g. "CR-999")
 * that doesn't exist in our evidence list, the decision is rejected and returned for
 * human review rather than silently passing through.
 */
@Component
public class CitationValidator {

    public record ValidationResult(boolean valid, String reason) {
        static ValidationResult ok() { return new ValidationResult(true, null); }
        static ValidationResult fail(String r) { return new ValidationResult(false, r); }
    }

    public ValidationResult validate(UnderwritingDecision decision, List<Finding> evidence) {
        Set<String> evidenceIds = evidence.stream().map(Finding::id).collect(Collectors.toSet());

        for (var item : decision.rationale()) {
            if (item.citedFindingIds().isEmpty()) {
                return ValidationResult.fail(
                        "Rationale item has no citations: \"" + item.statement() + "\"");
            }
            for (String cited : item.citedFindingIds()) {
                if (!evidenceIds.contains(cited)) {
                    return ValidationResult.fail(
                            "Rationale cites unknown finding ID '" + cited +
                            "' — not present in evidence set " + evidenceIds);
                }
            }
        }

        if (decision.outcome() == UnderwritingDecision.Outcome.DECLINED) {
            boolean hasComplianceCite = decision.rationale().stream()
                    .flatMap(r -> r.citedFindingIds().stream())
                    .anyMatch(id -> id.startsWith("CO-"));
            if (!hasComplianceCite) {
                return ValidationResult.fail(
                        "DECLINED outcome must cite at least one compliance finding (CO-xxx) per §6.2");
            }
        }

        return ValidationResult.ok();
    }
}
