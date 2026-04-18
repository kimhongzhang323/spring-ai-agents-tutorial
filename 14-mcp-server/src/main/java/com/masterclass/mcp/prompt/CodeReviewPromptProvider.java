package com.masterclass.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.server.McpServerFeatures;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Prompt: structured code review template.
 *
 * Clients request this prompt, supply arguments (code snippet, language, focus),
 * and get back a pre-filled message list ready to send to the LLM.
 *
 * This is the MCP "Prompt" primitive: a reusable, parameterised interaction pattern.
 */
@Component
public class CodeReviewPromptProvider {

    public McpServerFeatures.SyncPromptRegistration registration() {
        var prompt = new McpSchema.Prompt(
                "code-review",
                "Perform a structured code review with security, performance, and maintainability analysis",
                List.of(
                        new McpSchema.PromptArgument("code", "The code snippet to review", true),
                        new McpSchema.PromptArgument("language", "Programming language (java, python, typescript, etc.)", true),
                        new McpSchema.PromptArgument("focus", "Optional focus area: security | performance | maintainability | all", false)
                )
        );

        return new McpServerFeatures.SyncPromptRegistration(prompt, req -> {
            Map<String, String> args = req.arguments() != null ? req.arguments() : Map.of();
            String code = args.getOrDefault("code", "(no code provided)");
            String language = args.getOrDefault("language", "unknown");
            String focus = args.getOrDefault("focus", "all");

            String systemText = """
                    You are a senior software engineer performing a thorough code review.
                    Review the provided %s code with special attention to: %s.

                    Structure your review as:
                    1. **Summary** (2-3 sentences)
                    2. **Critical Issues** (security holes, bugs, data loss risks)
                    3. **Performance** (complexity, I/O, memory)
                    4. **Maintainability** (naming, SOLID, test coverage gaps)
                    5. **Positive Observations** (what was done well)
                    6. **Actionable Recommendations** (prioritised, concrete)
                    """.formatted(language, focus);

            String userText = """
                    Please review this %s code:

                    ```%s
                    %s
                    ```
                    """.formatted(language, language, code);

            return new McpSchema.GetPromptResult(
                    "Structured code review for " + language + " code",
                    List.of(
                            new McpSchema.PromptMessage(
                                    McpSchema.Role.ASSISTANT,
                                    new McpSchema.TextContent(systemText)
                            ),
                            new McpSchema.PromptMessage(
                                    McpSchema.Role.USER,
                                    new McpSchema.TextContent(userText)
                            )
                    )
            );
        });
    }
}
