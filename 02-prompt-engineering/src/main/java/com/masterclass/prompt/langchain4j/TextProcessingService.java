package com.masterclass.prompt.langchain4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * LangChain4j typed AiService — the killer feature for prompt engineering.
 *
 * Compared to Spring AI's PromptTemplate approach:
 *   Spring AI: PromptTemplate(resource).create(Map.of("key", value)) — runtime rendering, flexible.
 *   LangChain4j: @SystemMessage / @UserMessage annotations — compile-time, discoverable, refactor-safe.
 *
 * LangChain4j wins for: multi-step prompt chains where type safety matters.
 * Spring AI wins for: runtime-configurable prompts (user-driven templates, A/B testing).
 *
 * Wire this interface via AiServices.builder(TextProcessingService.class).chatLanguageModel(model).build()
 * See TextProcessingServiceConfig for the Spring @Bean definition.
 */
public interface TextProcessingService {

    @SystemMessage("""
            You are a professional summarizer.
            Never invent facts. Do not exceed {{maxWords}} words.
            Return ONLY the summary, no preamble.
            """)
    @UserMessage("Summarize the following text:\n\n{{text}}")
    String summarize(@V("text") String text, @V("maxWords") int maxWords);

    @SystemMessage("""
            You are a professional translator.
            Translate faithfully — preserve meaning, tone, and formatting.
            Return ONLY the translated text.
            """)
    @UserMessage("Translate from {{sourceLanguage}} to {{targetLanguage}}:\n\n{{text}}")
    String translate(@V("text") String text,
                     @V("sourceLanguage") String sourceLanguage,
                     @V("targetLanguage") String targetLanguage);

    @SystemMessage("""
            You are a sentiment classifier.
            Examples:
            - "I love this!" → POSITIVE
            - "Arrived broken." → NEGATIVE
            - "It arrived Tuesday." → NEUTRAL
            Respond with exactly one word: POSITIVE, NEGATIVE, or NEUTRAL.
            """)
    @UserMessage("Classify: {{text}}")
    String classifySentiment(@V("text") String text);
}
