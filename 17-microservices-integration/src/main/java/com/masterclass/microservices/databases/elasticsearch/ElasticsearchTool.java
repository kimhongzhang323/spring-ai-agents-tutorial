package com.masterclass.microservices.databases.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

@Component
public class ElasticsearchTool {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchTool.class);

    private final ElasticsearchClient esClient;

    public ElasticsearchTool(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @Tool(description = """
            Performs a full-text search on an Elasticsearch index and returns matching documents.
            Use this when the user needs fuzzy text search, relevance ranking, or hybrid
            keyword+semantic search across large document corpora. Elasticsearch is the
            industry standard for log analysis (ELK stack), product search, and document retrieval.
            Input: indexName (e.g. 'products'), queryText (the search phrase).
            Returns: top 5 matching documents as JSON.
            """)
    public String searchElasticsearch(String indexName, String queryText) {
        try {
            SearchResponse<Map> response = esClient.search(s -> s
                    .index(indexName)
                    .query(q -> q.multiMatch(m -> m.query(queryText).fields("*")))
                    .size(5),
                    Map.class);
            List<String> hits = response.hits().hits().stream()
                    .map(Hit::source)
                    .map(Object::toString)
                    .toList();
            log.debug("Elasticsearch search: index={} query={} hits={}", indexName, queryText, hits.size());
            return hits.toString();
        } catch (Exception e) {
            log.error("Elasticsearch search failed", e);
            return "Search failed: " + e.getMessage();
        }
    }

    @Tool(description = """
            Indexes a new document into Elasticsearch.
            Use this to store agent-generated summaries, classification results,
            or extracted entities so they can be full-text searched later.
            Input: indexName (e.g. 'agent-outputs'), documentJson (JSON string of the document).
            Returns: the Elasticsearch document ID.
            """)
    public String indexDocument(String indexName, String documentJson) {
        try {
            var response = esClient.index(i -> i
                    .index(indexName)
                    .withJson(new StringReader(documentJson)));
            log.debug("Elasticsearch indexed: index={} id={}", indexName, response.id());
            return "Document indexed in '%s' with ID: %s".formatted(indexName, response.id());
        } catch (Exception e) {
            log.error("Elasticsearch index failed", e);
            return "Index failed: " + e.getMessage();
        }
    }
}
