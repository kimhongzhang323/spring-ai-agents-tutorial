package com.masterclass.aisecurity.filter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects and redacts PII from text using regex patterns (OWASP LLM06).
 * Original values are SHA-256 hashed before audit logging — never stored plaintext.
 * Applied to both input (before LLM sees it) and output (before client sees it).
 */
@Component
public class PiiFilter implements SecurityFilter {

    private static final Logger log = LoggerFactory.getLogger(PiiFilter.class);

    // Ordered map: more specific patterns first
    private static final Map<String, Pattern> PII_PATTERNS = new LinkedHashMap<>();

    static {
        PII_PATTERNS.put("[CREDIT_CARD]", Pattern.compile("\\b(?:\\d[ -]?){13,16}\\b"));
        PII_PATTERNS.put("[SSN]",         Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"));
        PII_PATTERNS.put("[EMAIL]",       Pattern.compile("\\b[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\\b"));
        PII_PATTERNS.put("[PHONE]",       Pattern.compile("\\b(?:\\+?\\d[\\s.-]?){7,15}\\b"));
        PII_PATTERNS.put("[IP_ADDRESS]",  Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"));
    }

    private final Counter piiRedactedCounter;

    public PiiFilter(MeterRegistry meterRegistry) {
        this.piiRedactedCounter = meterRegistry.counter("security.pii.redacted");
    }

    @Override
    public SecurityContext apply(SecurityContext ctx) {
        String redacted = redact(ctx.sanitizedInput());
        if (!redacted.equals(ctx.sanitizedInput())) {
            log.debug("PII redacted for user={}", ctx.userId());
            piiRedactedCounter.increment();
        }
        return ctx.withSanitized(redacted);
    }

    public String redact(String text) {
        if (text == null) return null;
        String result = text;
        for (var entry : PII_PATTERNS.entrySet()) {
            Matcher m = entry.getValue().matcher(result);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String found = m.group();
                log.debug("Redacting PII token hash={}", sha256(found));
                m.appendReplacement(sb, entry.getKey());
            }
            m.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12) + "...";
        } catch (Exception e) {
            return "[hash-error]";
        }
    }
}
