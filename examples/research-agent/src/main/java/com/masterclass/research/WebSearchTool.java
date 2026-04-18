package com.masterclass.research;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Simulated web search tool.
 * In production: integrate with Tavily, SerpAPI, or Brave Search API.
 * The @Tool annotation here is from dev.langchain4j (not Spring AI).
 */
@Component
public class WebSearchTool {

    @Tool("Search the web for current information on a topic. Returns a list of relevant snippets. Use this to find facts, recent news, or background information.")
    public List<String> search(String query) {
        // Stub: returns fake snippets. Replace with real HTTP call to search API.
        return List.of(
                "Snippet 1 about '" + query + "': According to recent research, this topic involves multiple competing approaches...",
                "Snippet 2 about '" + query + "': Industry analysts note that the key developments include...",
                "Snippet 3 about '" + query + "': A 2024 study found significant implications for..."
        );
    }
}
