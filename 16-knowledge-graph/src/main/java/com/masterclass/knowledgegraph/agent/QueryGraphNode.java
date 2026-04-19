package com.masterclass.knowledgegraph.agent;

import com.masterclass.knowledgegraph.graph.GraphNode;
import com.masterclass.knowledgegraph.graph.GraphState;
import com.masterclass.knowledgegraph.model.KgEntity;
import com.masterclass.knowledgegraph.model.KnowledgeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * Graph node: answers a natural-language question using the knowledge graph as
 * grounded context injected into the LLM prompt.
 *
 * This is the RAG-over-graph pattern: structured graph data → LLM context →
 * grounded natural language answer.
 */
public class QueryGraphNode implements GraphNode {

    private static final Logger log = LoggerFactory.getLogger(QueryGraphNode.class);

    private static final String SYSTEM_PROMPT = """
            You are a precise knowledge graph question-answering assistant.
            You will receive a knowledge graph context and a user question.
            Answer ONLY from facts present in the graph context.
            If the answer is not in the graph, say "I don't have enough information in the graph to answer that."
            Be concise and cite entity IDs when relevant.
            """;

    private final ChatClient     chatClient;
    private final KnowledgeGraph graph;

    public QueryGraphNode(ChatClient chatClient, KnowledgeGraph graph) {
        this.chatClient = chatClient;
        this.graph      = graph;
    }

    @Override
    public GraphState execute(GraphState state) {
        String question = state.require("query");

        // Find entities mentioned in the question and fetch their neighbourhood
        List<KgEntity> candidates = graph.findByName(extractSearchTerm(question));
        StringBuilder graphContext = new StringBuilder();

        if (candidates.isEmpty()) {
            graphContext.append(graph.toContextString());
        } else {
            // Multi-hop traversal from matching entities (up to 2 hops)
            for (KgEntity entity : candidates) {
                graphContext.append("Entity: %s (%s, id=%s)\n".formatted(
                        entity.name(), entity.type(), entity.id()));
                graph.relationsFrom(entity.id()).forEach(r ->
                        graphContext.append("  -[%s]-> %s\n".formatted(r.relation(), r.toId())));
                graph.relationsTo(entity.id()).forEach(r ->
                        graphContext.append("  <-[%s]- %s\n".formatted(r.relation(), r.fromId())));

                graph.traverse(entity.id(), 2).forEach(hop ->
                        graph.findById(hop.entityId()).ifPresent(e ->
                                graphContext.append("  (hop %d via %s) %s: %s\n"
                                        .formatted(hop.hops(), hop.path(), e.type(), e.name()))));
            }
        }

        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                      Knowledge Graph Context:
                      %s

                      Question: %s
                      """.formatted(graphContext, question))
                .call()
                .content();

        log.info("QueryGraphNode: answered question '{}', answer length={}", question, answer.length());
        return GraphState.of(Map.of(
                "answer",          answer,
                "graph_context",   graphContext.toString(),
                "entities_found",  candidates.size()
        ));
    }

    /** Extract a simple search term from the question for entity lookup. */
    private String extractSearchTerm(String question) {
        // Strip common question words to get the noun phrase
        return question.replaceAll("(?i)^(who|what|where|when|how|did|does|is|are|was|were)\\s+", "")
                .replaceAll("\\?", "")
                .trim();
    }
}
