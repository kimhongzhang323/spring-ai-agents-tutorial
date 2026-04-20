package com.masterclass.aisecurity.filter;

public class SecurityViolationException extends RuntimeException {

    public enum ViolationType {
        PROMPT_INJECTION, PII_IN_INPUT, JAILBREAK_ATTEMPT, EXCESSIVE_AGENCY,
        INPUT_TOO_LONG, POLICY_VIOLATION
    }

    private final ViolationType violationType;

    public SecurityViolationException(ViolationType violationType, String message) {
        super(message);
        this.violationType = violationType;
    }

    public ViolationType getViolationType() {
        return violationType;
    }
}
