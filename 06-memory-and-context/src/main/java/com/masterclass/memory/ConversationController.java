package com.masterclass.memory;

import com.masterclass.shared.dto.AgentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Memory & Context", description = "Module 06 – multi-turn conversations with Redis-backed memory")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/new")
    @Operation(summary = "Start a new conversation, returns a conversationId")
    public ResponseEntity<Map<String, String>> newConversation() {
        return ResponseEntity.ok(Map.of("conversationId", conversationService.newConversationId()));
    }

    @PostMapping("/{conversationId}/chat")
    @Operation(summary = "Send a message in an existing conversation",
               description = "The agent remembers all prior turns in this conversation (sliding window, Redis-backed).")
    public ResponseEntity<ConversationService.ConversationTurn> chat(
            @PathVariable String conversationId,
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        // Pass authenticated username so memory is scoped per user
        return ResponseEntity.ok(
                conversationService.chat(conversationId, user.getUsername(), request.message()));
    }
}
