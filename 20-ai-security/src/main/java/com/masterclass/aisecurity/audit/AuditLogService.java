package com.masterclass.aisecurity.audit;

import com.masterclass.aisecurity.filter.SecurityContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditRecordRepository repository;
    private final Counter allowedCounter;
    private final Counter blockedCounter;

    public AuditLogService(AuditRecordRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.allowedCounter = meterRegistry.counter("security.audit.allowed");
        this.blockedCounter = meterRegistry.counter("security.audit.blocked");
    }

    public void logAllowed(SecurityContext ctx) {
        AuditRecord.AuditOutcome outcome = ctx.warnings().isEmpty()
                ? AuditRecord.AuditOutcome.ALLOWED
                : AuditRecord.AuditOutcome.ALLOWED_WITH_WARNINGS;

        repository.save(new AuditRecord(ctx.userId(), ctx.sanitizedInput(),
                ctx.riskScore(), outcome, String.join("; ", ctx.warnings())));
        allowedCounter.increment();
        log.debug("Audit: ALLOWED user={} riskScore={}", ctx.userId(), ctx.riskScore());
    }

    public void logBlocked(SecurityContext ctx, String reason) {
        repository.save(new AuditRecord(ctx.userId(), ctx.sanitizedInput(),
                1.0, AuditRecord.AuditOutcome.BLOCKED, reason));
        blockedCounter.increment();
        log.warn("Audit: BLOCKED user={} reason={}", ctx.userId(), reason);
    }
}
