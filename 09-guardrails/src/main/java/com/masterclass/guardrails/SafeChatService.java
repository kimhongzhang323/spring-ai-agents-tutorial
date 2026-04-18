package com.masterclass.guardrails;

import com.masterclass.shared.guardrails.InputValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SafeChatService {

    private final ChatClient chatClient;
    private final InputValidator inputValidator;

    public SafeChatService(ChatClient.Builder builder, ContentModerator moderator,
                           PiiRedactor piiRedactor, InputValidator inputValidator) {
        this.inputValidator = inputValidator;
        this.chatClient = builder
                .defaultSystem("You are a helpful, safe assistant.")
                // GuardrailAdvisor runs input moderation before the LLM call
                // and PII redaction + output moderation after
                .defaultAdvisors(new GuardrailAdvisor(moderator, piiRedactor))
                .build();
    }

    public SafeResponse chat(String message) {
        // Layer 1: structural validation (length, basic injection patterns)
        var structuralCheck = inputValidator.validate(message);
        if (!structuralCheck.valid()) {
            return SafeResponse.blocked(structuralCheck.reason());
        }

        // Layer 2 + 3: content moderation (input) → LLM → PII redaction (output)
        // These are handled transparently by GuardrailAdvisor in the advisor chain
        String reply = chatClient.prompt().user(message).call().content();
        return SafeResponse.ok(reply);
    }

    public record SafeResponse(String message, boolean blocked, String blockReason) {
        public static SafeResponse ok(String message)          { return new SafeResponse(message, false, null); }
        public static SafeResponse blocked(String reason)      { return new SafeResponse(null, true, reason); }
    }
}
