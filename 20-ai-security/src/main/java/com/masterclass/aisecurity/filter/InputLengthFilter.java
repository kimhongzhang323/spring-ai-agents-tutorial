package com.masterclass.aisecurity.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InputLengthFilter implements SecurityFilter {

    @Value("${ai-security.max-input-length:2000}")
    private int maxLength;

    @Override
    public SecurityContext apply(SecurityContext ctx) {
        if (ctx.sanitizedInput().length() > maxLength) {
            throw new SecurityViolationException(
                    SecurityViolationException.ViolationType.INPUT_TOO_LONG,
                    "Input exceeds maximum allowed length of %d characters".formatted(maxLength));
        }
        return ctx;
    }
}
