package com.masterclass.banking;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Sub-agent: executes financial actions (transfers).
 * All actions requiring money movement must go through the HITL approval flow.
 * The supervisor calls requestApproval, then a human approves via the REST webhook,
 * then the supervisor calls executeApprovedTransfer.
 */
@Component
public class ActionAgent {

    private final ApprovalStore approvalStore;

    public ActionAgent(ApprovalStore approvalStore) {
        this.approvalStore = approvalStore;
    }

    @Tool(description = """
            Submit a transfer request for human approval.
            Use this when a customer wants to transfer money between accounts.
            Returns an approvalId that must be approved by a human before the transfer executes.
            NEVER execute a transfer without first obtaining human approval.
            """)
    public String requestTransferApproval(String fromAccount, String toAccount,
                                           double amount, String customerId) {
        String details = "Transfer $%.2f from %s to %s".formatted(amount, fromAccount, toAccount);
        String approvalId = approvalStore.submit(customerId, "TRANSFER", details);
        return "Transfer pending approval. Approval ID: " + approvalId
                + ". A human reviewer will approve or reject this request. "
                + "Inform the customer they will be notified once approved.";
    }

    @Tool(description = """
            Execute a transfer that has already been approved by a human reviewer.
            Only call this after confirming the approval status is APPROVED.
            If the approval is PENDING or REJECTED, do not proceed.
            """)
    public String executeApprovedTransfer(String approvalId) {
        var request = approvalStore.get(approvalId);
        if (request == null) return "Approval ID " + approvalId + " not found.";
        return switch (request.status()) {
            case APPROVED -> "Transfer executed successfully. Details: " + request.details();
            case PENDING  -> "Transfer is still pending human approval. Please wait.";
            case REJECTED -> "Transfer was rejected by the reviewer. No funds moved.";
        };
    }
}
