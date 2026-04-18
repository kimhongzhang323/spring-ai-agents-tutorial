package com.masterclass.apimgmt;

import com.masterclass.apimgmt.cost.CostTracker;
import com.masterclass.apimgmt.cost.UsageRecord;
import com.masterclass.shared.dto.AgentRequest;
import com.masterclass.shared.dto.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "API Management", description = "Module 07 – JWT, rate limiting, API keys, cost tracking, versioning")
@SecurityRequirement(name = "bearerAuth")
public class UsageController {

    private final CostTracker costTracker;
    private final ChatClient chatClient;

    public UsageController(CostTracker costTracker, ChatClient.Builder builder) {
        this.costTracker = costTracker;
        this.chatClient = builder.defaultSystem("You are a helpful assistant.").build();
    }

    /** Demonstration endpoint — chat + cost tracking wired together. */
    @PostMapping("/api/v1/agent/chat")
    @Operation(summary = "Chat with cost tracking",
               description = "Every request records token usage in the usage_records table.")
    public ResponseEntity<AgentResponse> chat(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {

        var response = chatClient.prompt().user(request.message()).call();
        String content = response.content();

        // Record usage — in production use ChatResponse metadata for exact token counts
        costTracker.record(user.getUsername(), "gpt-4o-mini", estimateTokens(request.message()),
                estimateTokens(content));

        return ResponseEntity.ok(AgentResponse.of(content));
    }

    @GetMapping("/api/v1/me/usage")
    @Operation(summary = "Get current user's token usage summary")
    public ResponseEntity<CostTracker.UserUsageSummary> myUsage(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(costTracker.getSummary(user.getUsername()));
    }

    @GetMapping("/api/v1/me/usage/history")
    @Operation(summary = "Get current user's full usage history")
    public ResponseEntity<List<UsageRecord>> myHistory(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(costTracker.getUsageForUser(user.getUsername()));
    }

    // Rough approximation: 1 token ≈ 4 characters (OpenAI rule of thumb)
    private int estimateTokens(String text) {
        return text == null ? 0 : text.length() / 4;
    }
}
