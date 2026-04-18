package com.masterclass.research;

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
public class ResearchConfig {

    @Bean
    @Profile("local")
    ChatLanguageModel ollamaModel(
            @Value("${langchain4j.ollama.base-url}") String url,
            @Value("${langchain4j.ollama.model-name}") String model,
            @Value("${langchain4j.ollama.temperature}") double temp) {
        return OllamaChatModel.builder().baseUrl(url).modelName(model)
                .temperature(temp).timeout(Duration.ofSeconds(120)).build();
    }

    @Bean
    @Profile("cloud")
    ChatLanguageModel openAiModel(
            @Value("${langchain4j.open-ai.api-key}") String key,
            @Value("${langchain4j.open-ai.model-name}") String model,
            @Value("${langchain4j.open-ai.temperature}") double temp) {
        return OpenAiChatModel.builder().apiKey(key).modelName(model).temperature(temp).build();
    }

    @Bean
    ResearchAgentDef researchAgent(ChatLanguageModel model, WebSearchTool webSearchTool) {
        return AiServices.builder(ResearchAgentDef.class)
                .chatLanguageModel(model)
                .tools(webSearchTool)
                .build();
    }
}
