package com.masterclass.knowledgegraph.agent;

import com.masterclass.knowledgegraph.graph.GraphNode;
import com.masterclass.knowledgegraph.graph.GraphState;
import com.masterclass.knowledgegraph.model.KgEntity;
import com.masterclass.knowledgegraph.model.KgRelation;
import com.masterclass.knowledgegraph.model.KnowledgeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Graph node: extracts typed relations between previously identified entities
 * and adds them to the shared KnowledgeGraph.
 *
 * Runs after ExtractEntitiesNode — depends on "extracted_entities" in state.
 */
public class ExtractRelationsNode implements GraphNode {

    private static final Logger log = LoggerFactory.getLogger(ExtractRelationsNode.class);

    // Expected: RELATION|from-id|RELATION_TYPE|to-id|0.95
    private static final Pattern RELATION_LINE = Pattern.compile(
            "^RELATION\\|([^|]+)\\|([^|]+)\\|([^|]+)\\|([\\d.]+)$", Pattern.MULTILINE);

    private static final String SYSTEM_PROMPT = """
            You are a precise relation extractor for a knowledge graph.
            Given a list of entities and source text, extract typed relations between entities.

            Output one line per relation in EXACTLY this format:
            RELATION|<from-entity-id>|<RELATION_TYPE>|<to-entity-id>|<confidence 0.0-1.0>

            Relation types to use (uppercase_snake_case):
            WORKS_AT, FOUNDED, INVENTED, KNOWS, PART_OF, LOCATED_IN, CREATED_BY,
            ACQUIRED_BY, INVESTED_IN, COLLABORATED_WITH, STUDIED_AT, LED_BY

            Only output relations you can directly support from the text.
            """;

    private final ChatClient     chatClient;
    private final KnowledgeGraph graph;

    public ExtractRelationsNode(ChatClient chatClient, KnowledgeGraph graph) {
        this.chatClient = chatClient;
        this.graph      = graph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GraphState execute(GraphState state) {
        String text = state.require("input_text");
        List<KgEntity> entities = state.require("extracted_entities");

        if (entities.isEmpty()) {
            log.warn("ExtractRelationsNode: no entities in state, skipping");
            return GraphState.of(Map.of("extracted_relations", List.of()));
        }

        String entityList = entities.stream()
                .map(e -> "%s (%s, id=%s)".formatted(e.name(), e.type(), e.id()))
                .reduce("", (a, b) -> a + "\n- " + b);

        String raw = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                      Entities found:
                      %s

                      Source text:
                      %s

                      Extract all relations between these entities.
                      """.formatted(entityList, text))
                .call()
                .content();

        List<KgRelation> extracted = new ArrayList<>();
        Matcher m = RELATION_LINE.matcher(raw);
        while (m.find()) {
            try {
                double confidence = Double.parseDouble(m.group(4).trim());
                KgRelation relation = new KgRelation(
                        m.group(1).trim(), m.group(2).trim(), m.group(3).trim(), confidence);
                graph.addRelation(relation);
                extracted.add(relation);
                log.debug("Extracted relation: {}", relation);
            } catch (NumberFormatException ignored) {
                // malformed confidence value — skip
            }
        }

        log.info("ExtractRelationsNode: extracted {} relations", extracted.size());
        return GraphState.of(Map.of(
                "extracted_relations", extracted,
                "relation_count", extracted.size()
        ));
    }
}
