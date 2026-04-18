package com.masterclass.mcp.prompt;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.server.McpServerFeatures;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Prompt: data analysis template.
 *
 * Guides the LLM through structured data analysis: describe the dataset,
 * identify patterns, surface insights, and suggest follow-up queries.
 */
@Component
public class DataAnalysisPromptProvider {

    public McpServerFeatures.SyncPromptRegistration registration() {
        var prompt = new McpSchema.Prompt(
                "data-analysis",
                "Analyse tabular or JSON data and generate business insights with SQL follow-up suggestions",
                List.of(
                        new McpSchema.PromptArgument("data", "The data to analyse (JSON, CSV, or plain text)", true),
                        new McpSchema.PromptArgument("goal", "What business question should the analysis answer?", true),
                        new McpSchema.PromptArgument("format", "Output format: narrative | bullets | table", false)
                )
        );

        return new McpServerFeatures.SyncPromptRegistration(prompt, req -> {
            Map<String, String> args = req.arguments() != null ? req.arguments() : Map.of();
            String data = args.getOrDefault("data", "(no data provided)");
            String goal = args.getOrDefault("goal", "Identify key patterns and anomalies");
            String format = args.getOrDefault("format", "narrative");

            String systemText = """
                    You are a senior data analyst. Your job is to:
                    1. Understand the business goal provided by the user.
                    2. Analyse the supplied data thoroughly.
                    3. Present findings in %s format.
                    4. Suggest 3 follow-up SQL queries to deepen the analysis.
                    5. Highlight any data quality issues you notice.

                    Be precise with numbers. Do not make assumptions beyond the data.
                    """.formatted(format);

            String userText = """
                    Business goal: %s

                    Data:
                    %s
                    """.formatted(goal, data);

            return new McpSchema.GetPromptResult(
                    "Data analysis: " + goal,
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
