package com.masterclass.knowledgegraph.agent;

import com.masterclass.knowledgegraph.graph.GraphNode;
import com.masterclass.knowledgegraph.graph.GraphState;
import com.masterclass.knowledgegraph.model.KgEntity;
import com.masterclass.knowledgegraph.model.KnowledgeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Graph node: extracts named entities from raw text using an LLM and populates
 * the shared KnowledgeGraph. Writes extracted entities to GraphState so
 * downstream nodes can operate on them without re-querying the graph.
 */
public class ExtractEntitiesNode implements GraphNode {

    private static final Logger log = LoggerFactory.getLogger(ExtractEntitiesNode.class);

    // Expected LLM output format: ENTITY|type|name|id
    private static final Pattern ENTITY_LINE = Pattern.compile(
            "^ENTITY\\|([^|]+)\\|([^|]+)\\|([^|]+)$", Pattern.MULTILINE);

    private static final String SYSTEM_PROMPT = """
            You are a precise named-entity extractor for a knowledge graph.
            Extract all significant entities from the text.

            Output one line per entity in EXACTLY this format (no markdown, no extra text):
            ENTITY|<TYPE>|<canonical name>|<unique-id>

            Types to use: Person, Organization, Location, Concept, Product, Event
            ID rules: lowercase, hyphenated, prefixed with type abbreviation
              (per: person, org: organization, loc: location, con: concept, prod: product, evt: event)

            Example output:
            ENTITY|Person|Alan Turing|per:alan-turing
            ENTITY|Organization|Bletchley Park|org:bletchley-park
            ENTITY|Concept|Artificial Intelligence|con:artificial-intelligence
            """;

    private final ChatClient    chatClient;
    private final KnowledgeGraph graph;

    public ExtractEntitiesNode(ChatClient chatClient, KnowledgeGraph graph) {
        this.chatClient = chatClient;
        this.graph      = graph;
    }

    @Override
    public GraphState execute(GraphState state) {
        String text = state.require("input_text");

        String raw = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("Extract entities from:\n\n" + text)
                .call()
                .content();

        List<KgEntity> extracted = new ArrayList<>();
        Matcher m = ENTITY_LINE.matcher(raw);
        while (m.find()) {
            KgEntity entity = KgEntity.of(m.group(3).trim(), m.group(1).trim(), m.group(2).trim());
            graph.addEntity(entity);
            extracted.add(entity);
            log.debug("Extracted entity: {}", entity);
        }

        log.info("ExtractEntitiesNode: extracted {} entities", extracted.size());
        return GraphState.of(Map.of(
                "extracted_entities", extracted,
                "entity_count", extracted.size()
        ));
    }
}
