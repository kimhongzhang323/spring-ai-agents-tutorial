package com.masterclass.hello.langchain4j;

import dev.langchain4j.model.ollama.OllamaChatModel;

import java.time.Duration;

/**
 * LangChain4j variant of the Hello Agent — same behaviour, different API.
 *
 * Compare with HelloAgentService (Spring AI):
 *   Spring AI: ChatClient.builder(chatModel).build().prompt().user(msg).call().content()
 *   LangChain4j: OllamaChatModel.builder()...build().generate(msg)
 *
 * Key differences:
 * - Spring AI: auto-configured via spring-ai-*-spring-boot-starter; ChatClient is a @Bean.
 * - LangChain4j: manually constructed; no Spring Boot autoconfiguration by default.
 * - LangChain4j shines in typed AiService interfaces (see module 02 langchain4j/ variant).
 *
 * This class is intentionally standalone — it is NOT a Spring @Bean.
 * Run it as a main() to see the raw output.
 */
public class HelloAgentLc4j {

    public static void main(String[] args) {
        var model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.1")
                .timeout(Duration.ofSeconds(60))
                .build();

        String response = model.generate("Hello! What is Spring AI?");
        System.out.println(response);
    }
}
