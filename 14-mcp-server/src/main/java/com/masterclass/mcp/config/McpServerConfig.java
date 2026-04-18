package com.masterclass.mcp.config;

import com.masterclass.mcp.prompt.CodeReviewPromptProvider;
import com.masterclass.mcp.prompt.DataAnalysisPromptProvider;
import com.masterclass.mcp.resource.DocumentResourceProvider;
import com.masterclass.mcp.resource.SchemaResourceProvider;
import org.springframework.ai.mcp.server.McpServerFeatures;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers MCP Resources and Prompts as Spring beans.
 * Tools are auto-discovered via @Tool annotations — no explicit registration needed.
 */
@Configuration
public class McpServerConfig {

    /**
     * Expose static documents and API specs as readable MCP Resources.
     * Resources are for content the LLM passively reads; use Tools for actions.
     */
    @Bean
    McpServerFeatures.SyncResourceRegistrationCustomizer resourceRegistrations(
            DocumentResourceProvider docs,
            SchemaResourceProvider schemas) {
        return registrar -> {
            docs.registrations().forEach(registrar::addResource);
            schemas.registrations().forEach(registrar::addResource);
        };
    }

    /**
     * Expose reusable prompt templates as MCP Prompts.
     * Clients can request these to pre-fill the conversation with structured context.
     */
    @Bean
    McpServerFeatures.SyncPromptRegistrationCustomizer promptRegistrations(
            CodeReviewPromptProvider codeReview,
            DataAnalysisPromptProvider dataAnalysis) {
        return registrar -> {
            registrar.addPrompt(codeReview.registration());
            registrar.addPrompt(dataAnalysis.registration());
        };
    }
}
