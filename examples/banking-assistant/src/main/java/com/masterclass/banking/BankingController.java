package com.masterclass.banking;

import com.masterclass.shared.dto.AgentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/banking")
@Tag(name = "Banking Assistant", description = "Multi-agent banking assistant with HITL approval")
@SecurityRequirement(name = "bearerAuth")
public class BankingController {

    private final BankingSupervisorService supervisor;
    private final ApprovalStore approvalStore;

    public BankingController(BankingSupervisorService supervisor, ApprovalStore approvalStore) {
        this.supervisor    = supervisor;
        this.approvalStore = approvalStore;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with the banking assistant")
    public ResponseEntity<String> chat(@RequestBody AgentRequest request) {
        return ResponseEntity.ok(supervisor.chat(request.message()));
    }

    /** Human reviewer approves a pending transfer */
    @PostMapping("/approvals/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a pending transfer (human reviewer only)")
    public ResponseEntity<String> approve(@PathVariable String id) {
        boolean ok = approvalStore.approve(id);
        return ok ? ResponseEntity.ok("Approved") : ResponseEntity.badRequest().body("Not found or not pending");
    }

    /** Human reviewer rejects a pending transfer */
    @PostMapping("/approvals/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a pending transfer (human reviewer only)")
    public ResponseEntity<String> reject(@PathVariable String id) {
        boolean ok = approvalStore.reject(id);
        return ok ? ResponseEntity.ok("Rejected") : ResponseEntity.badRequest().body("Not found or not pending");
    }

    @GetMapping("/approvals/{id}")
    @Operation(summary = "Check the status of a transfer approval")
    public ResponseEntity<ApprovalStore.ApprovalRequest> getApproval(@PathVariable String id) {
        var req = approvalStore.get(id);
        return req != null ? ResponseEntity.ok(req) : ResponseEntity.notFound().build();
    }
}
