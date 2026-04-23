package com.masterclass.memory;

import com.masterclass.shared.dto.AgentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
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
    @Operation(
            summary = "Start a new conversation",
            description = "Returns a conversationId to use in subsequent chat requests.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "New conversation ID"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded — see Retry-After header",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    public ResponseEntity<Map<String, String>> newConversation() {
        return ResponseEntity.ok(Map.of("conversationId", conversationService.newConversationId()));
    }

    @PostMapping("/{conversationId}/chat")
    @Operation(
            summary = "Send a message in an existing conversation",
            description = "The agent remembers all prior turns in this conversation (sliding window, Redis-backed). " +
                    "Memory is isolated per user — a user cannot access another user's conversation.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            content = @Content(schema = @Schema(implementation = ConversationService.ConversationTurn.class))),
                    @ApiResponse(responseCode = "400", description = "Blank message or input validation failure",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "429", description = "Rate limit exceeded — see Retry-After header",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    public ResponseEntity<ConversationService.ConversationTurn> chat(
            @PathVariable String conversationId,
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                conversationService.chat(conversationId, user.getUsername(), request.message()));
    }

    @DeleteMapping("/{conversationId}")
    @Operation(
            summary = "Clear conversation history",
            description = "Deletes all stored messages for this conversation. Memory is user-scoped — " +
                    "only the owner can clear their own conversation.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Conversation cleared"),
                    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
            }
    )
    public ResponseEntity<Void> clearConversation(
            @PathVariable String conversationId,
            @AuthenticationPrincipal UserDetails user) {
        conversationService.clearConversation(conversationId, user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
