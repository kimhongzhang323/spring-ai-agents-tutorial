package com.masterclass.guardrails;

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Custom Spring AI Advisor that applies guardrails in the advisor chain.
 *
 * Advisor chain order (lower order = runs first on request, last on response):
 *   Request:  InputValidator → ContentModerator → LLM
 *   Response: LLM → PiiRedactor → ContentModerator → caller
 *
 * This is the idiomatic Spring AI way to apply cross-cutting concerns to LLM calls.
 * The alternative (service-level if/else) leaks guardrail logic into business code.
 */
public class GuardrailAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final ContentModerator moderator;
    private final PiiRedactor piiRedactor;

    public GuardrailAdvisor(ContentModerator moderator, PiiRedactor piiRedactor) {
        this.moderator = moderator;
        this.piiRedactor = piiRedactor;
    }

    @Override
    public String getName() { return "GuardrailAdvisor"; }

    @Override
    public int getOrder() { return 0; }  // Run before other advisors

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        // --- Input guardrail ---
        String userText = extractUserText(request);
        var inputCheck = moderator.moderate(userText);
        if (inputCheck.isBlocked()) {
            return blockedResponse(request, inputCheck.reason());
        }

        // --- Proceed to LLM ---
        AdvisedResponse response = chain.nextAroundCall(request);

        // --- Output guardrail ---
        return applyOutputGuardrails(response);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        String userText = extractUserText(request);
        var inputCheck = moderator.moderate(userText);
        if (inputCheck.isBlocked()) {
            return Flux.just(blockedResponse(request, inputCheck.reason()));
        }
        return chain.nextAroundStream(request)
                .map(this::applyOutputGuardrails);
    }

    private AdvisedResponse applyOutputGuardrails(AdvisedResponse response) {
        if (response.response() == null) return response;
        String content = response.response().getResult().getOutput().getText();
        if (content == null) return response;

        // PII redaction
        var redacted = piiRedactor.redact(content);

        // Output moderation
        var outputCheck = moderator.moderate(redacted.redactedText());
        String finalContent = outputCheck.isBlocked()
                ? "[Response blocked by content policy]"
                : redacted.redactedText();

        // Rebuild response with redacted content
        var newGeneration = new Generation(new AssistantMessage(finalContent));
        var newChatResponse = new ChatResponse(List.of(newGeneration), response.response().getMetadata());
        return new AdvisedResponse(newChatResponse, response.adviseContext());
    }

    private String extractUserText(AdvisedRequest request) {
        return request.userText() != null ? request.userText() : "";
    }

    private AdvisedResponse blockedResponse(AdvisedRequest request, String reason) {
        var msg = new AssistantMessage("I'm unable to process that request: " + reason);
        var response = new ChatResponse(List.of(new Generation(msg)));
        return new AdvisedResponse(response, Map.of());
    }
}
