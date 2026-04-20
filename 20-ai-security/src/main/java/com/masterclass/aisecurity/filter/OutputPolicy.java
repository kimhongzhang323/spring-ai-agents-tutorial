package com.masterclass.aisecurity.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Enforces output safety policies on every LLM response (OWASP LLM02).
 *
 * Checks:
 * 1. Canary token leak — detects if the system prompt was extracted
 * 2. Harmful content heuristics — violence, illegal instructions
 * 3. PII re-scan — LLM may hallucinate or regurgitate PII from training data
 */
@Component
public class OutputPolicy {

    private static final Logger log = LoggerFactory.getLogger(OutputPolicy.class);

    private static final List<Pattern> HARMFUL_PATTERNS = List.of(
            Pattern.compile("(?i)(step[- ]by[- ]step|instructions?|how to).{0,30}(make|build|create|synthesize).{0,30}(bomb|explosive|poison|weapon)"),
            Pattern.compile("(?i)(how to|ways? to).{0,20}(hack|bypass|crack|exploit).{0,30}(system|server|account|password)"),
            Pattern.compile("(?i)(child|minor).{0,20}(sexual|explicit|nude|pornograph)")
    );

    @Value("${ai-security.canary-token:CANARY_SECRET_DO_NOT_REPEAT}")
    private String canaryToken;

    private final PiiFilter piiFilter;

    public OutputPolicy(PiiFilter piiFilter) {
        this.piiFilter = piiFilter;
    }

    public String enforce(String userId, String rawResponse) {
        // 1. Canary leak detection
        if (rawResponse.contains(canaryToken)) {
            log.error("SECURITY ALERT: canary token leaked in response for user={}", userId);
            throw new SecurityViolationException(
                    SecurityViolationException.ViolationType.POLICY_VIOLATION,
                    "System prompt integrity violation detected");
        }

        // 2. Harmful content
        for (Pattern p : HARMFUL_PATTERNS) {
            if (p.matcher(rawResponse).find()) {
                log.warn("Harmful output pattern blocked for user={}", userId);
                throw new SecurityViolationException(
                        SecurityViolationException.ViolationType.POLICY_VIOLATION,
                        "Response violates content safety policy");
            }
        }

        // 3. PII re-scan on output
        String cleaned = piiFilter.redact(rawResponse);
        if (!cleaned.equals(rawResponse)) {
            log.warn("PII detected and redacted from LLM output for user={}", userId);
        }

        return cleaned;
    }
}
