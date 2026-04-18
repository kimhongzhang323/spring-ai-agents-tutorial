package com.masterclass.guardrails;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Redacts PII patterns from LLM output before returning it to the user.
 * In production: replace regex with a Presidio REST call or AWS Comprehend.
 */
@Component
public class PiiRedactor {

    private static final List<RedactionRule> RULES = List.of(
            new RedactionRule("SSN",          Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),           "[SSN REDACTED]"),
            new RedactionRule("Credit Card",  Pattern.compile("\\b(?:\\d{4}[- ]){3}\\d{4}\\b"),        "[CC REDACTED]"),
            new RedactionRule("Email",        Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"), "[EMAIL REDACTED]"),
            new RedactionRule("Phone US",     Pattern.compile("\\b(?:\\+1[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"), "[PHONE REDACTED]"),
            new RedactionRule("IP Address",   Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"),    "[IP REDACTED]")
    );

    public RedactionResult redact(String text) {
        if (text == null) return new RedactionResult("", 0);
        String result = text;
        int count = 0;
        for (var rule : RULES) {
            var matcher = rule.pattern().matcher(result);
            if (matcher.find()) {
                result = matcher.replaceAll(rule.replacement());
                count++;
            }
        }
        return new RedactionResult(result, count);
    }

    public record RedactionResult(String redactedText, int redactedFieldCount) {}
    private record RedactionRule(String name, Pattern pattern, String replacement) {}
}
