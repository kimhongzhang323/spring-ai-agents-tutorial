package com.masterclass.banking;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for pending human-in-the-loop approvals.
 * In production: back this with Redis so approvals survive pod restarts.
 */
@Component
public class ApprovalStore {

    public enum Status { PENDING, APPROVED, REJECTED }

    public record ApprovalRequest(
            String approvalId,
            String customerId,
            String action,
            String details,
            Status status
    ) {}

    private final Map<String, ApprovalRequest> store = new ConcurrentHashMap<>();

    public String submit(String customerId, String action, String details) {
        String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        store.put(id, new ApprovalRequest(id, customerId, action, details, Status.PENDING));
        return id;
    }

    public ApprovalRequest get(String approvalId) {
        return store.get(approvalId);
    }

    public boolean approve(String approvalId) {
        return updateStatus(approvalId, Status.APPROVED);
    }

    public boolean reject(String approvalId) {
        return updateStatus(approvalId, Status.REJECTED);
    }

    private boolean updateStatus(String id, Status status) {
        var existing = store.get(id);
        if (existing == null || existing.status() != Status.PENDING) return false;
        store.put(id, new ApprovalRequest(
                existing.approvalId(), existing.customerId(),
                existing.action(), existing.details(), status));
        return true;
    }
}
