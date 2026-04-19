package com.masterclass.guardrails;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

/**
 * Custom Spring AI Advisor that applies guardrails in the advisor chain.
 *
 * Advisor chain order (lower order = runs first on request, last on response):
 *   Request:  InputValidator → ContentModerator → LLM
 *   Response: LLM → PiiRedactor → ContentModerator → caller
 *
 * This is the idiomatic Spring AI way to apply cross-cutting concerns to LLM calls.
 * The alternative (service-level if/else) leaks guardrail logic into business code.
 *
 * In Spring AI 1.0.0, the preferred advisor base is {@link BaseAdvisor} which exposes
 * {@code before()} and {@code after()} hooks — cleaner than implementing the full
 * {@code adviseCall()} lifecycle yourself.
 */
public class GuardrailAdvisor implements BaseAdvisor {

    private final ContentModerator moderator;
    private final PiiRedactor piiRedactor;

    public GuardrailAdvisor(ContentModerator moderator, PiiRedactor piiRedactor) {
        this.moderator = moderator;
        this.piiRedactor = piiRedactor;
    }

    @Override
    public String getName() { return "GuardrailAdvisor"; }

    @Override
    public int getOrder() { return 0; }

    /**
     * Input guardrail: runs BEFORE the LLM call.
     * Returns a blocked request stub if the input fails moderation.
     * BaseAdvisor calls this then proceeds to the LLM only if no exception is thrown.
     */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String userText = extractUserText(request);
        var inputCheck = moderator.moderate(userText);
        if (inputCheck.isBlocked()) {
            // Throw a custom exception that the service layer catches to return a blocked response
            throw new GuardrailBlockedException("Input blocked: " + inputCheck.reason());
        }
        return request;
    }

    /**
     * Output guardrail: runs AFTER the LLM call.
     * Applies PII redaction and output content moderation.
     */
    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        if (response.chatResponse() == null) return response;

        var result = response.chatResponse().getResult();
        if (result == null || result.getOutput() == null) return response;

        String content = result.getOutput().getText();
        if (content == null) return response;

        // PII redaction
        var redacted = piiRedactor.redact(content);

        // Output moderation
        var outputCheck = moderator.moderate(redacted.redactedText());
        String finalContent = outputCheck.isBlocked()
                ? "[Response blocked by content policy]"
                : redacted.redactedText();

        // Rebuild the ChatClientResponse with redacted content
        var newGeneration = new Generation(new AssistantMessage(finalContent));
        var newChatResponse = new ChatResponse(List.of(newGeneration), response.chatResponse().getMetadata());
        return new ChatClientResponse(newChatResponse, response.context());
    }

    private String extractUserText(ChatClientRequest request) {
        var userMsg = request.prompt().getUserMessage();
        return userMsg != null ? userMsg.getText() : "";
    }

    public static class GuardrailBlockedException extends RuntimeException {
        public GuardrailBlockedException(String message) {
            super(message);
        }
    }
}
