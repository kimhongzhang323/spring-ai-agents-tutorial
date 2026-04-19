package com.masterclass.microservices.databases.postgres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PostgresTool {

    private static final Logger log = LoggerFactory.getLogger(PostgresTool.class);
    private static final int MAX_ROWS = 20;

    private final JdbcTemplate jdbcTemplate;

    public PostgresTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool(description = """
            Executes a read-only SQL SELECT query against PostgreSQL and returns the results as JSON.
            Use this when the user asks for data that is stored in a relational database —
            orders, users, products, transactions, etc.
            IMPORTANT: Only SELECT statements are allowed. Do NOT use INSERT, UPDATE, DELETE, or DDL.
            Results are limited to 20 rows to prevent runaway costs.
            Input: a valid PostgreSQL SELECT statement.
            Returns: JSON array of result rows.
            """)
    public String queryPostgres(String selectSql) {
        if (!selectSql.trim().toLowerCase().startsWith("select")) {
            return "Error: only SELECT queries are permitted.";
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql + " LIMIT " + MAX_ROWS);
            if (rows.isEmpty()) return "[]";
            var sb = new StringBuilder("[");
            rows.forEach(row -> sb.append(row).append(","));
            sb.setLength(sb.length() - 1);
            sb.append("]");
            log.debug("PostgreSQL query returned {} rows", rows.size());
            return sb.toString();
        } catch (Exception e) {
            log.error("PostgreSQL query failed: sql={}", selectSql, e);
            return "Query failed: " + e.getMessage();
        }
    }

    @Tool(description = """
            Returns the schema (table names and columns) of the PostgreSQL database.
            Call this BEFORE writing a SQL query so you know which tables and columns exist.
            Returns: JSON list of table names with their column definitions.
            """)
    public String getPostgresSchema() {
        String sql = """
                SELECT table_name, column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                ORDER BY table_name, ordinal_position
                LIMIT 200
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        return rows.toString();
    }
}
