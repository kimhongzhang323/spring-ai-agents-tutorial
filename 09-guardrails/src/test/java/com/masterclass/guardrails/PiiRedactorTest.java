package com.masterclass.guardrails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PiiRedactorTest {

    private final PiiRedactor redactor = new PiiRedactor();

    @ParameterizedTest(name = "redacts {0}")
    @CsvSource({
            "SSN,           'Your SSN is 123-45-6789 on file',         '[SSN REDACTED]'",
            "Email,         'Contact us at user@example.com',           '[EMAIL REDACTED]'",
            "Credit Card,   'Card: 4111-1111-1111-1111',               '[CC REDACTED]'",
            "IP Address,    'Server at 192.168.1.1',                   '[IP REDACTED]'",
    })
    void redactsPiiPatterns(String type, String input, String expectedSubstring) {
        var result = redactor.redact(input);
        assertThat(result.redactedText()).contains(expectedSubstring);
        assertThat(result.redactedFieldCount()).isGreaterThan(0);
    }

    @Test
    void safeTextPassesThrough() {
        String safe = "The weather in London is 12 degrees.";
        var result = redactor.redact(safe);
        assertThat(result.redactedText()).isEqualTo(safe);
        assertThat(result.redactedFieldCount()).isZero();
    }

    @Test
    void nullInputReturnsEmptyString() {
        var result = redactor.redact(null);
        assertThat(result.redactedText()).isEmpty();
    }
}
