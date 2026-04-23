package com.masterclass.aisecurity.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, String> {
    Page<AuditRecord> findByUserIdOrderByOccurredAtDesc(String userId, Pageable pageable);
    Page<AuditRecord> findByOutcomeOrderByOccurredAtDesc(AuditRecord.AuditOutcome outcome, Pageable pageable);
}
