package com.masterclass.knowledgegraph.agent;

import com.masterclass.knowledgegraph.graph.GraphNode;
import com.masterclass.knowledgegraph.graph.GraphState;
import com.masterclass.knowledgegraph.model.KnowledgeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * Graph node: generates a human-readable narrative summary of the entire
 * knowledge graph. Used after extraction to verify the graph makes sense.
 */
public class SummariseGraphNode implements GraphNode {

    private static final Logger log = LoggerFactory.getLogger(SummariseGraphNode.class);

    private static final String SYSTEM_PROMPT = """
            You are a knowledge graph summarisation assistant.
            Given a structured knowledge graph, write a concise 2-3 paragraph narrative
            that explains who/what the main entities are and how they relate to each other.
            Write in plain English. Do not reproduce the raw graph format.
            """;

    private final ChatClient     chatClient;
    private final KnowledgeGraph graph;

    public SummariseGraphNode(ChatClient chatClient, KnowledgeGraph graph) {
        this.chatClient = chatClient;
        this.graph      = graph;
    }

    @Override
    public GraphState execute(GraphState state) {
        String graphContext = graph.toContextString();

        String summary = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("Summarise this knowledge graph:\n\n" + graphContext)
                .call()
                .content();

        log.info("SummariseGraphNode: summary generated ({} chars)", summary.length());
        return GraphState.of(Map.of("graph_summary", summary));
    }
}
