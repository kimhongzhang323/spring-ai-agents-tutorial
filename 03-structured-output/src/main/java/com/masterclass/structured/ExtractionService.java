package com.masterclass.structured;

import com.masterclass.structured.domain.InvoiceData;
import com.masterclass.structured.domain.ProductReview;
import com.masterclass.structured.domain.ResumeData;
import com.masterclass.structured.exception.ParseRetryException;
import com.masterclass.shared.guardrails.InputValidator;
import com.masterclass.shared.observability.TokenUsageMetrics;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
public class ExtractionService {

    /*
     * How BeanOutputConverter works:
     * 1. Reads Jackson annotations (@JsonPropertyDescription, @JsonClassDescription) on the target record.
     * 2. Generates a JSON schema from those annotations.
     * 3. Appends "Your response must be valid JSON matching this schema: <schema>" to the prompt.
     * 4. Parses the LLM's response back into the target type via ObjectMapper.
     *
     * If parsing fails (LLM returned prose, markdown fences, or partial JSON),
     * ParseRetryException is thrown and Resilience4j retries up to max-attempts.
     */

    private static final String EXTRACTION_SYSTEM = """
            You are a data extraction engine. Extract information from the provided text.
            Your response MUST be valid JSON that exactly matches the provided schema.
            Do not include any explanation, markdown fences, or text outside the JSON object.
            If a field cannot be determined from the text, use null for optional fields.
            """;

    private final ChatClient chatClient;
    private final InputValidator inputValidator;
    private final TokenUsageMetrics tokenUsageMetrics;

    public ExtractionService(ChatClient.Builder chatClientBuilder, InputValidator inputValidator,
                             TokenUsageMetrics tokenUsageMetrics) {
        this.chatClient = chatClientBuilder
                .defaultSystem(EXTRACTION_SYSTEM)
                .build();
        this.inputValidator = inputValidator;
        this.tokenUsageMetrics = tokenUsageMetrics;
    }

    @Retry(name = "parseRetry", fallbackMethod = "invoiceFallback")
    public InvoiceData extractInvoice(String rawText) {
        validate(rawText);
        var converter = new BeanOutputConverter<>(InvoiceData.class);

        ChatResponse chatResponse = chatClient.prompt()
                .user(u -> u.text("""
                        Extract all invoice data from the following text.
                        {format}

                        Text:
                        {text}
                        """)
                        .param("format", converter.getFormat())
                        .param("text", rawText))
                .call()
                .chatResponse();

        recordUsage(chatResponse);
        return parseOrThrow(chatResponse.getResult().getOutput().getText(), converter, "invoice");
    }

    @Retry(name = "parseRetry", fallbackMethod = "reviewFallback")
    public ProductReview analyzeReview(String reviewText) {
        validate(reviewText);
        var converter = new BeanOutputConverter<>(ProductReview.class);

        ChatResponse chatResponse = chatClient.prompt()
                .user(u -> u.text("""
                        Analyze the sentiment and extract structured data from this product review.
                        {format}

                        Review:
                        {text}
                        """)
                        .param("format", converter.getFormat())
                        .param("text", reviewText))
                .call()
                .chatResponse();

        recordUsage(chatResponse);
        return parseOrThrow(chatResponse.getResult().getOutput().getText(), converter, "review");
    }

    @Retry(name = "parseRetry", fallbackMethod = "resumeFallback")
    public ResumeData extractResume(String resumeText) {
        validate(resumeText);
        var converter = new BeanOutputConverter<>(ResumeData.class);

        ChatResponse chatResponse = chatClient.prompt()
                .user(u -> u.text("""
                        Extract structured data from this resume or CV.
                        {format}

                        Resume:
                        {text}
                        """)
                        .param("format", converter.getFormat())
                        .param("text", resumeText))
                .call()
                .chatResponse();

        recordUsage(chatResponse);
        return parseOrThrow(chatResponse.getResult().getOutput().getText(), converter, "resume");
    }

    // ── Resilience4j fallback methods ─────────────────────────────────────────

    public InvoiceData invoiceFallback(String rawText, ParseRetryException ex) {
        throw new ExtractionFailedException("Failed to extract invoice data after retries: " + ex.getMessage());
    }

    public ProductReview reviewFallback(String reviewText, ParseRetryException ex) {
        throw new ExtractionFailedException("Failed to analyze review after retries: " + ex.getMessage());
    }

    public ResumeData resumeFallback(String resumeText, ParseRetryException ex) {
        throw new ExtractionFailedException("Failed to extract resume data after retries: " + ex.getMessage());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void recordUsage(ChatResponse response) {
        var usage = response.getMetadata().getUsage();
        if (usage != null) {
            tokenUsageMetrics.record(
                usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : 0,
                usage.getGenerationTokens() != null ? usage.getGenerationTokens().intValue() : 0
            );
        }
    }

    private <T> T parseOrThrow(String response, BeanOutputConverter<T> converter, String context) {
        try {
            return converter.convert(response);
        } catch (Exception e) {
            throw new ParseRetryException(
                    "LLM response could not be parsed as %s: %s".formatted(context, e.getMessage()), e);
        }
    }

    private void validate(String input) {
        var result = inputValidator.validate(input);
        if (!result.valid()) {
            throw new IllegalArgumentException(result.reason());
        }
    }

    public static class ExtractionFailedException extends RuntimeException {
        public ExtractionFailedException(String message) { super(message); }
    }
}
