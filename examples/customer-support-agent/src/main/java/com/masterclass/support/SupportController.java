package com.masterclass.support;

import com.masterclass.shared.dto.AgentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/support")
@Tag(name = "Customer Support", description = "AI-powered customer support with tools + RAG + memory")
@SecurityRequirement(name = "bearerAuth")
public class SupportController {

    private final SupportAgentService agent;

    public SupportController(SupportAgentService agent) {
        this.agent = agent;
    }

    @PostMapping("/chat/{conversationId}")
    @Operation(summary = "Send a message in a support conversation",
               description = "Maintains context across turns. Each user gets their own conversationId namespace.")
    public ResponseEntity<String> chat(
            @PathVariable String conversationId,
            @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        // Scope conversationId to the authenticated user to prevent cross-user access
        String scopedId = user.getUsername() + ":" + conversationId;
        String response = agent.chat(scopedId, request.message());
        return ResponseEntity.ok(response);
    }
}
