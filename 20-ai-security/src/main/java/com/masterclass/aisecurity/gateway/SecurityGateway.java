package com.masterclass.aisecurity.gateway;

import com.masterclass.aisecurity.audit.AuditLogService;
import com.masterclass.aisecurity.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ordered security filter chain applied to every incoming request.
 * Filters are applied in declaration order — do not reorder without considering dependencies.
 */
@Component
public class SecurityGateway {

    private static final Logger log = LoggerFactory.getLogger(SecurityGateway.class);

    private final List<SecurityFilter> filters;
    private final AuditLogService auditLog;

    public SecurityGateway(InputLengthFilter inputLengthFilter,
                           PiiFilter piiFilter,
                           InjectionDetector injectionDetector,
                           AuditLogService auditLog) {
        // Order matters: length first (fast fail), then PII redact, then injection detection
        this.filters = List.of(inputLengthFilter, piiFilter, injectionDetector);
        this.auditLog = auditLog;
    }

    /**
     * Screens and sanitizes the input. Returns the sanitized input string ready for the agent.
     * Throws {@link SecurityViolationException} if the request must be blocked.
     */
    public String screen(String userId, String rawInput) {
        SecurityContext ctx = SecurityContext.of(userId, rawInput);

        try {
            for (SecurityFilter filter : filters) {
                ctx = filter.apply(ctx);
            }
            auditLog.logAllowed(ctx);
            log.debug("SecurityGateway: ALLOWED user={} warnings={}", userId, ctx.warnings().size());
            return ctx.sanitizedInput();
        } catch (SecurityViolationException e) {
            auditLog.logBlocked(ctx, e.getMessage());
            throw e;
        }
    }
}
