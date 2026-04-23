package com.masterclass.aisecurity.controller;

import com.masterclass.aisecurity.agent.SecureAgentService;
import com.masterclass.aisecurity.audit.AuditRecord;
import com.masterclass.aisecurity.audit.AuditRecordRepository;
import com.masterclass.aisecurity.filter.SecurityViolationException;
import com.masterclass.aisecurity.model.SecureAgentRequest;
import com.masterclass.aisecurity.model.SecureAgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/secure-agent")
@Tag(name = "Secure Agent", description = "Agent with full OWASP LLM Top 10 defences")
@SecurityRequirement(name = "bearerAuth")
public class SecureAgentController {

    private final SecureAgentService agentService;
    private final AuditRecordRepository auditRepository;

    public SecureAgentController(SecureAgentService agentService,
                                 AuditRecordRepository auditRepository) {
        this.agentService      = agentService;
        this.auditRepository   = auditRepository;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with security screening on input and output")
    public ResponseEntity<?> chat(
            @Valid @RequestBody SecureAgentRequest request,
            @AuthenticationPrincipal UserDetails user) {

        try {
            String response = agentService.chat(user.getUsername(), request.message());
            return ResponseEntity.ok(SecureAgentResponse.of(response, List.of()));
        } catch (SecurityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Request blocked by security policy",
                    "type", e.getViolationType().name()
            ));
        }
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retrieve audit log (admin only) — always paginated, max 100 per page")
    public ResponseEntity<Page<AuditRecord>> audit(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) AuditRecord.AuditOutcome outcome,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending());

        if (userId != null) {
            return ResponseEntity.ok(auditRepository.findByUserIdOrderByOccurredAtDesc(userId, pageable));
        }
        if (outcome != null) {
            return ResponseEntity.ok(auditRepository.findByOutcomeOrderByOccurredAtDesc(outcome, pageable));
        }
        return ResponseEntity.ok(auditRepository.findAll(pageable));
    }
}
