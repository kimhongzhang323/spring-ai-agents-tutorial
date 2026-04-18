package com.masterclass.guardrails;

import com.masterclass.shared.dto.AgentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/safe")
@Tag(name = "Guardrails", description = "Module 09 – input/output moderation, PII redaction, prompt injection detection")
@SecurityRequirement(name = "bearerAuth")
public class SafeChatController {

    private final SafeChatService safeChatService;

    public SafeChatController(SafeChatService safeChatService) {
        this.safeChatService = safeChatService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with full guardrail stack",
               description = "Input: validated (length + injection) → moderated. Output: PII-redacted → moderated.")
    public ResponseEntity<SafeChatService.SafeResponse> chat(@Valid @RequestBody AgentRequest request) {
        var response = safeChatService.chat(request.message());
        if (response.blocked()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
