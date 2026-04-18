package com.masterclass.mcp;

import com.masterclass.mcp.tool.DatabaseQueryTool;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseQueryToolTest {

    private final DatabaseQueryTool tool = new DatabaseQueryTool(new SimpleMeterRegistry());

    @Test
    void selectQueryReturnsData() {
        String result = tool.queryDatabase("SELECT * FROM products LIMIT 3");
        assertThat(result).contains("Spring AI");
    }

    @Test
    void insertQueryIsRejected() {
        String result = tool.queryDatabase("INSERT INTO products VALUES (1, 'hack', 'x', 0, 0)");
        assertThat(result).contains("error").contains("SELECT");
    }

    @Test
    void dropQueryIsRejected() {
        String result = tool.queryDatabase("DROP TABLE products");
        assertThat(result).contains("error");
    }

    @Test
    void updateQueryIsRejected() {
        String result = tool.queryDatabase("UPDATE products SET price=0");
        assertThat(result).contains("error");
    }

    @Test
    void informationSchemaQueryIsRejected() {
        String result = tool.queryDatabase("SELECT * FROM information_schema.tables");
        assertThat(result).contains("error");
    }
}
