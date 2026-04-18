package com.masterclass.mcp.tool;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool: safe, read-only SQL query execution.
 *
 * In a real deployment, replace the mock data with a DataSource/JdbcTemplate.
 * This tool only allows SELECT queries — never INSERT/UPDATE/DELETE.
 *
 * The @Tool description is the LLM-facing contract: be specific about what
 * queries are allowed, what columns exist, and what the tool returns.
 */
@Component
public class DatabaseQueryTool {

    private static final Logger log = LoggerFactory.getLogger(DatabaseQueryTool.class);

    private final Counter queryCounter;
    private final Timer queryTimer;

    public DatabaseQueryTool(MeterRegistry meterRegistry) {
        this.queryCounter = Counter.builder("mcp.tool.database.queries")
                .description("Total database queries executed via MCP")
                .register(meterRegistry);
        this.queryTimer = Timer.builder("mcp.tool.database.query.duration")
                .description("Database query execution time")
                .register(meterRegistry);
    }

    @Tool(description = """
            Execute a read-only SQL SELECT query against the masterclass database.

            Available tables:
              - products(id, name, category, price, stock_quantity)
              - orders(id, customer_id, product_id, quantity, status, created_at)
              - customers(id, email, name, tier, created_at)

            Rules:
              - Only SELECT statements are allowed; any other statement will be rejected.
              - Maximum 100 rows returned; use LIMIT in your query.
              - Do NOT include passwords, tokens, or PII columns in your query.

            Returns: a JSON array of row objects, or an error message if the query fails.

            Examples:
              SELECT name, price FROM products WHERE category = 'electronics' LIMIT 10
              SELECT status, COUNT(*) as cnt FROM orders GROUP BY status
            """)
    public String queryDatabase(
            @ToolParam(description = "A valid SQL SELECT statement") String sql) {

        log.debug("MCP database query: {}", sql);

        if (!isSafeQuery(sql)) {
            return "{\"error\": \"Only SELECT statements are permitted.\"}";
        }

        queryCounter.increment();
        return queryTimer.record(() -> executeMockQuery(sql));
    }

    private boolean isSafeQuery(String sql) {
        String normalized = sql.trim().toLowerCase();
        if (!normalized.startsWith("select")) return false;
        // Block any attempt to modify data or read system tables
        List<String> forbidden = List.of("insert", "update", "delete", "drop", "truncate",
                "create", "alter", "exec", "execute", "information_schema", "pg_catalog");
        return forbidden.stream().noneMatch(normalized::contains);
    }

    private String executeMockQuery(String sql) {
        // In production: replace with JdbcTemplate.queryForList(sql)
        // Mock data for demonstration
        if (sql.toLowerCase().contains("products")) {
            return """
                    [
                      {"id":1,"name":"Spring AI Handbook","category":"books","price":49.99,"stock_quantity":150},
                      {"id":2,"name":"LangChain4j Guide","category":"books","price":39.99,"stock_quantity":89},
                      {"id":3,"name":"Java 21 Deep Dive","category":"books","price":54.99,"stock_quantity":203}
                    ]""";
        }
        if (sql.toLowerCase().contains("orders")) {
            return """
                    [
                      {"status":"completed","cnt":342},
                      {"status":"pending","cnt":87},
                      {"status":"cancelled","cnt":23}
                    ]""";
        }
        return "[]";
    }
}
