package com.masterclass.aisecurity.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, String> {
    List<AuditRecord> findByUserIdOrderByOccurredAtDesc(String userId);
    List<AuditRecord> findByOutcomeOrderByOccurredAtDesc(AuditRecord.AuditOutcome outcome);
}
