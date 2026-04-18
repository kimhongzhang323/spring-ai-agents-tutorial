package com.masterclass.shared.guardrails;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Basic structural guardrails applied before any input reaches the LLM.
 * Deep-dive in module 09; extended there with ContentModerator and PiiRedactor.
 */
@Component
public class InputValidator {

    // Heuristic patterns for obvious prompt injection attempts
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore (all )?(previous|prior|above) instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bDAN\\b"),
            Pattern.compile("jailbreak", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act as (if )?you (are|were) (a )?(?!helpful)", Pattern.CASE_INSENSITIVE)
    );

    public ValidationResult validate(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.fail("Input must not be blank");
        }
        if (input.length() > 4000) {
            return ValidationResult.fail("Input exceeds maximum length of 4000 characters");
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return ValidationResult.fail("Input contains disallowed content");
            }
        }
        return ValidationResult.ok();
    }

    public record ValidationResult(boolean valid, String reason) {
        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult fail(String reason) { return new ValidationResult(false, reason); }
    }
}
