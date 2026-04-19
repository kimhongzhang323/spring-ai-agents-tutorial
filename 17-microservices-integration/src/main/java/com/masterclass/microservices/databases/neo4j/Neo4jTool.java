package com.masterclass.microservices.databases.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class Neo4jTool {

    private static final Logger log = LoggerFactory.getLogger(Neo4jTool.class);

    private final Driver driver;

    public Neo4jTool(Driver driver) {
        this.driver = driver;
    }

    @Tool(description = """
            Executes a Cypher query against Neo4j graph database and returns results.
            Use this when the user's question involves relationships, paths, or connections
            between entities — for example: 'Who is connected to whom?', 'What is the
            shortest path between A and B?', 'Which nodes share common neighbors?'.
            Graph databases excel at multi-hop relationship traversal that would require
            complex JOINs in SQL. This module builds on Module 16 (Knowledge Graph).
            Only MATCH/RETURN queries are allowed — no write operations.
            Input: a Cypher MATCH...RETURN statement.
            Returns: JSON list of result records.
            """)
    public String queryCypher(String cypherQuery) {
        if (!cypherQuery.trim().toUpperCase().startsWith("MATCH")) {
            return "Error: only MATCH Cypher queries are permitted.";
        }
        try (Session session = driver.session()) {
            Result result = session.run(cypherQuery);
            List<Map<String, Object>> rows = result.list(Record::asMap);
            log.debug("Neo4j query returned {} records", rows.size());
            return rows.stream().limit(20).toList().toString();
        } catch (Exception e) {
            log.error("Neo4j query failed: cypher={}", cypherQuery, e);
            return "Cypher query failed: " + e.getMessage();
        }
    }

    @Tool(description = """
            Returns the node labels and relationship types in the Neo4j graph database.
            Call this first to understand the graph schema before writing a Cypher query.
            Returns: JSON with available labels and relationship types.
            """)
    public String getNeo4jSchema() {
        try (Session session = driver.session()) {
            List<String> labels = session.run("CALL db.labels()").list(r -> r.get(0).asString());
            List<String> relTypes = session.run("CALL db.relationshipTypes()").list(r -> r.get(0).asString());
            return "{\"labels\":%s,\"relationshipTypes\":%s}".formatted(labels, relTypes);
        } catch (Exception e) {
            return "Schema fetch failed: " + e.getMessage();
        }
    }
}
