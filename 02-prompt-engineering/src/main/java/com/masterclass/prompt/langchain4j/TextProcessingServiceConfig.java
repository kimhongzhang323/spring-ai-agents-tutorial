package com.masterclass.prompt.langchain4j;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Wires the LangChain4j typed AiService as a Spring bean.
 * The interface TextProcessingService has zero implementation code — LangChain4j generates it.
 */
@Configuration
public class TextProcessingServiceConfig {

    @Bean
    public TextProcessingService textProcessingService(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {

        var model = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName("llama3.1")
                .timeout(Duration.ofSeconds(60))
                .build();

        return AiServices.builder(TextProcessingService.class)
                .chatLanguageModel(model)
                .build();
    }
}
