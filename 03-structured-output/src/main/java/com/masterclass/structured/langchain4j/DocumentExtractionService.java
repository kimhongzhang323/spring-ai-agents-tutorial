package com.masterclass.structured.langchain4j;

import com.masterclass.structured.domain.InvoiceData;
import com.masterclass.structured.domain.ProductReview;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j typed AiService with structured (non-String) return types.
 *
 * How it works:
 * - Return type is a Java record (or POJO), not String.
 * - LangChain4j automatically appends a JSON schema hint to the prompt (like BeanOutputConverter).
 * - Parses the response via Jackson and returns the typed object.
 * - No explicit BeanOutputConverter wiring needed — it's built into AiServices.
 *
 * Spring AI equivalent:
 *   chatClient.prompt().user(...).call().entity(InvoiceData.class)
 *   — uses BeanOutputConverter internally, same concept, different API style.
 *
 * LangChain4j wins here for: clean interface definitions without service boilerplate.
 * Spring AI wins for: mixing structured + streaming output, advisor chains, advisors.
 */
public interface DocumentExtractionService {

    @SystemMessage("""
            You are a data extraction engine.
            Your response MUST be valid JSON matching the provided schema.
            Do not include markdown fences or any text outside the JSON object.
            Use null for optional fields that cannot be determined.
            """)
    @UserMessage("Extract all invoice data from this text:\n\n{{text}}")
    InvoiceData extractInvoice(@V("text") String text);

    @SystemMessage("""
            You are a product review analyst.
            Analyze the review and return structured data as valid JSON matching the schema.
            """)
    @UserMessage("Analyze this product review:\n\n{{text}}")
    ProductReview analyzeReview(@V("text") String text);
}
