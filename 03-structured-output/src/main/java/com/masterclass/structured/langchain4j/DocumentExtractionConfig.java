package com.masterclass.structured.langchain4j;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class DocumentExtractionConfig {

    @Bean
    public DocumentExtractionService documentExtractionService(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {

        var model = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName("llama3.1")
                .temperature(0.0)          // zero temperature for deterministic extraction
                .timeout(Duration.ofSeconds(60))
                .build();

        return AiServices.builder(DocumentExtractionService.class)
                .chatLanguageModel(model)
                .build();
    }
}
