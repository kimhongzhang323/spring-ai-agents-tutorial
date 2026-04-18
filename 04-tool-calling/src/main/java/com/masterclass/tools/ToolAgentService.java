package com.masterclass.tools;

import com.masterclass.shared.guardrails.InputValidator;
import com.masterclass.tools.tool.CalculatorTool;
import com.masterclass.tools.tool.CurrencyTool;
import com.masterclass.tools.tool.StockPriceTool;
import com.masterclass.tools.tool.WeatherTool;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.stereotype.Service;

@Service
public class ToolAgentService {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant with access to real-time tools.
            When you need information about weather, stock prices, currency conversion, or arithmetic,
            use the appropriate tool rather than guessing.
            Always tell the user which tool you used and what it returned.
            If a tool returns an error, explain it clearly and suggest alternatives.
            """;

    private final ChatClient chatClient;
    private final InputValidator inputValidator;

    public ToolAgentService(
            ChatClient.Builder chatClientBuilder,
            InputValidator inputValidator,
            WeatherTool weatherTool,
            CalculatorTool calculatorTool,
            StockPriceTool stockPriceTool,
            CurrencyTool currencyTool) {

        this.inputValidator = inputValidator;
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                /*
                 * SimpleLoggerAdvisor logs every tool call and tool result at DEBUG level.
                 * In application.yml we set org.springframework.ai=DEBUG to see these.
                 * Remove in production or replace with a custom ObservationAdvisor (module 08).
                 *
                 * Log format:
                 *   [TOOL CALL]   toolName(args)
                 *   [TOOL RESULT] toolName → result
                 */
                .defaultAdvisors(new SimpleLoggerAdvisor())
                /*
                 * .tools() registers all @Tool-annotated methods on these beans.
                 * Spring AI inspects the @Tool descriptions to build the tools array
                 * sent in the LLM request — the LLM chooses which tools to invoke.
                 */
                .defaultTools(weatherTool, calculatorTool, stockPriceTool, currencyTool)
                .build();
    }

    @Retry(name = "llmRetry", fallbackMethod = "fallback")
    public String chat(String userMessage) {
        var validation = inputValidator.validate(userMessage);
        if (!validation.valid()) {
            throw new IllegalArgumentException(validation.reason());
        }

        return chatClient
                .prompt()
                .user(userMessage)
                .call()
                .content();
    }

    public String fallback(String userMessage, Exception ex) {
        return "I'm having trouble reaching the AI right now. Please try again in a moment.";
    }
}
