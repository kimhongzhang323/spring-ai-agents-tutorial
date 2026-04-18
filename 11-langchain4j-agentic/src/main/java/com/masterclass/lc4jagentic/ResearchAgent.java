package com.masterclass.lc4jagentic;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Typed AiService interface — LangChain4j generates the implementation at runtime.
 * Each method maps to one LLM call with its own system prompt.
 */
public interface ResearchAgent {

    @SystemMessage("""
            You are a rigorous research agent. Given a topic, produce a concise but comprehensive
            summary covering: key definitions, current state, main debates, and 3–5 notable facts.
            Aim for 200–300 words. Use plain prose, no markdown headers.
            """)
    @UserMessage("Research the following topic thoroughly: {{topic}}")
    ResearchResult research(@V("topic") String topic);
}
