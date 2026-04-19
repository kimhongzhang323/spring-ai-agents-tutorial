package com.masterclass.microservices.databases.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CassandraTool {

    private static final Logger log = LoggerFactory.getLogger(CassandraTool.class);

    private final CqlSession cqlSession;

    public CassandraTool(CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    @Tool(description = """
            Executes a CQL SELECT query against Apache Cassandra and returns results as JSON.
            Cassandra is optimized for high write throughput and time-series data — ideal
            for IoT sensor readings, clickstream events, or any agent that processes
            massive write-heavy workloads with predictable partition-key-based access patterns.
            IMPORTANT: Cassandra does not support arbitrary WHERE clauses — queries must
            use partition keys. Only SELECT queries are permitted.
            Input: a valid CQL SELECT statement.
            Returns: JSON array of result rows (max 20).
            """)
    public String queryCassandra(String cqlSelect) {
        if (!cqlSelect.trim().toLowerCase().startsWith("select")) {
            return "Error: only SELECT CQL statements are permitted.";
        }
        try {
            ResultSet rs = cqlSession.execute(cqlSelect);
            List<String> rows = new ArrayList<>();
            int count = 0;
            for (Row row : rs) {
                if (count++ >= 20) break;
                rows.add(row.getFormattedContents());
            }
            log.debug("Cassandra query returned {} rows", rows.size());
            return rows.toString();
        } catch (Exception e) {
            log.error("Cassandra query failed: cql={}", cqlSelect, e);
            return "Cassandra query failed: " + e.getMessage();
        }
    }

    @Tool(description = """
            Returns all table definitions in the Cassandra keyspace 'agentdb'.
            Use this before writing a CQL query to understand available tables and partition keys.
            Returns: list of table descriptions with column names and types.
            """)
    public String getCassandraSchema() {
        try {
            ResultSet rs = cqlSession.execute(
                    "SELECT table_name, column_name, type FROM system_schema.columns " +
                    "WHERE keyspace_name='agentdb'");
            List<String> rows = new ArrayList<>();
            for (Row row : rs) {
                rows.add("{table:%s,column:%s,type:%s}".formatted(
                        row.getString("table_name"),
                        row.getString("column_name"),
                        row.getString("type")));
            }
            return rows.toString();
        } catch (Exception e) {
            return "Schema fetch failed: " + e.getMessage();
        }
    }
}
