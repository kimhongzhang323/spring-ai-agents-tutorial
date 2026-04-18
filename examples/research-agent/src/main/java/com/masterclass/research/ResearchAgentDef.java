package com.masterclass.research;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Typed research agent interface.
 * AiServices wires in the WebSearchTool automatically via .tools(webSearchTool).
 */
public interface ResearchAgentDef {

    @SystemMessage("""
            You are a rigorous research assistant with access to a web search tool.
            When given a topic, use the search tool to gather information, then synthesize
            a comprehensive, well-cited report.
            Structure: Executive Summary → Key Findings (cited) → Controversies → Conclusion.
            Be specific. Never make up citations — use only what the search tool returns.
            """)
    @UserMessage("Research this topic and produce a cited report: {{topic}}")
    String research(@V("topic") String topic);
}
