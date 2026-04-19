package com.masterclass.microservices.databases.clickhouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClickHouseTool {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseTool.class);

    private final String url;
    private final String username;
    private final String password;

    public ClickHouseTool(
            @Value("${clickhouse.url:jdbc:clickhouse://localhost:8123/default}") String url,
            @Value("${clickhouse.username:default}") String username,
            @Value("${clickhouse.password:}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Tool(description = """
            Executes an analytical SELECT query against ClickHouse OLAP database.
            ClickHouse processes billions of rows per second using columnar storage —
            ideal for agent-driven analytics, BI queries, log analysis, and real-time dashboards.
            Use this when the user asks for aggregations, time-series analysis, or
            scanning large datasets that would be too slow in PostgreSQL.
            Only SELECT queries are allowed. Results limited to 50 rows.
            Input: a valid ClickHouse SQL SELECT statement.
            Returns: JSON array of result rows.
            """)
    public String queryClickHouse(String selectSql) {
        if (!selectSql.trim().toLowerCase().startsWith("select")) {
            return "Error: only SELECT queries are permitted.";
        }
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql + " LIMIT 50")) {
            ResultSetMetaData meta = rs.getMetaData();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
            log.debug("ClickHouse query returned {} rows", rows.size());
            return rows.toString();
        } catch (Exception e) {
            log.error("ClickHouse query failed: sql={}", selectSql, e);
            return "ClickHouse query failed: " + e.getMessage();
        }
    }
}
