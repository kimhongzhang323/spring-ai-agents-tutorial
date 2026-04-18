package com.masterclass.tools.langchain4j;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.time.Duration;

/**
 * LangChain4j tool-calling variant.
 *
 * Key differences vs Spring AI:
 *
 * Spring AI @Tool:
 *   - org.springframework.ai.tool.annotation.Tool
 *   - Registered via chatClientBuilder.defaultTools(bean)
 *   - Spring beans: @Component, injectable, testable in isolation
 *
 * LangChain4j @Tool:
 *   - dev.langchain4j.agent.tool.Tool
 *   - Registered via AiServices.builder().tools(instance)
 *   - Can be any POJO — does not need to be a Spring bean (but can be)
 *
 * Both frameworks inspect method signatures + descriptions to build the tool schema
 * sent in the LLM request. The LLM's response includes tool_call blocks that the
 * framework executes, feeding results back until the LLM produces a final answer.
 *
 * This class is intentionally self-contained — run main() to see tool calls in the logs.
 */
public class Lc4jToolAgent {

    // LangChain4j tool — note the different annotation package
    static class Lc4jWeatherTool {
        @Tool("Get the current weather for a city. Returns temperature and description.")
        public String getCurrentWeather(String city) {
            return switch (city.toLowerCase()) {
                case "london" -> "12°C, Overcast, 78% humidity";
                case "tokyo"  -> "24°C, Partly cloudy, 65% humidity";
                default       -> "20°C, Data unavailable for " + city;
            };
        }
    }

    static class Lc4jCalculatorTool {
        @Tool("Calculate the result of a mathematical expression. Input: a math expression as a string.")
        public String calculate(double a, String operation, double b) {
            return switch (operation.toUpperCase()) {
                case "ADD"      -> String.valueOf(a + b);
                case "SUBTRACT" -> String.valueOf(a - b);
                case "MULTIPLY" -> String.valueOf(a * b);
                case "DIVIDE"   -> b == 0 ? "Error: division by zero" : String.valueOf(a / b);
                default         -> "Unknown operation: " + operation;
            };
        }
    }

    // Typed AiService interface — LangChain4j generates the implementation
    interface ToolAssistant {
        @SystemMessage("You are a helpful assistant. Use tools to answer questions accurately.")
        @UserMessage("{{question}}")
        String answer(@V("question") String question);
    }

    public static void main(String[] args) {
        var model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.1")
                .timeout(Duration.ofSeconds(120))
                .build();

        ToolAssistant assistant = AiServices.builder(ToolAssistant.class)
                .chatLanguageModel(model)
                .tools(new Lc4jWeatherTool(), new Lc4jCalculatorTool())
                .build();

        // The agent will call getCurrentWeather("London") then answer
        String response = assistant.answer("What is the weather in London? Also, what is 250 multiplied by 4?");
        System.out.println(response);
    }
}
