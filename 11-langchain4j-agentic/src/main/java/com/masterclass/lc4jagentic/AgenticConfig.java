package com.masterclass.lc4jagentic;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Configuration
public class AgenticConfig {

    // ── Model beans (profile-selected) ──────────────────────────────────────

    @Bean
    @Profile("local")
    ChatLanguageModel ollamaChatModel(
            @Value("${langchain4j.ollama.base-url}") String baseUrl,
            @Value("${langchain4j.ollama.model-name}") String modelName,
            @Value("${langchain4j.ollama.temperature}") double temperature) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    @Profile("cloud")
    ChatLanguageModel openAiChatModel(
            @Value("${langchain4j.open-ai.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.model-name}") String modelName,
            @Value("${langchain4j.open-ai.temperature}") double temperature) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }

    // ── Typed AiService beans ────────────────────────────────────────────────

    @Bean
    ResearchAgent researchAgent(ChatLanguageModel model) {
        return AiServices.builder(ResearchAgent.class)
                .chatLanguageModel(model)
                .build();
    }

    @Bean
    CriticAgent criticAgent(ChatLanguageModel model) {
        return AiServices.builder(CriticAgent.class)
                .chatLanguageModel(model)
                .build();
    }

    @Bean
    SynthesizerAgent synthesizerAgent(ChatLanguageModel model) {
        return AiServices.builder(SynthesizerAgent.class)
                .chatLanguageModel(model)
                .build();
    }
}
