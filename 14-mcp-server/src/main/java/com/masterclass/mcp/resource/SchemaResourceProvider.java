package com.masterclass.mcp.resource;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.server.McpServerFeatures;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Exposes database schema descriptions as MCP Resources.
 *
 * Giving the LLM schema context upfront dramatically improves the quality of
 * generated SQL queries compared to prompting it to guess column names.
 *
 * URI scheme: schemas/{table-name}
 */
@Component
public class SchemaResourceProvider {

    public List<McpServerFeatures.SyncResourceRegistration> registrations() {
        return List.of(
                schemaResource("schemas/products", "Products Table Schema", PRODUCTS_SCHEMA),
                schemaResource("schemas/orders", "Orders Table Schema", ORDERS_SCHEMA),
                schemaResource("schemas/customers", "Customers Table Schema", CUSTOMERS_SCHEMA),
                schemaResource("schemas/all", "Full Database Schema", FULL_SCHEMA)
        );
    }

    private McpServerFeatures.SyncResourceRegistration schemaResource(
            String uri, String name, String schema) {
        var resource = new McpSchema.Resource(uri, name, "Database table schema definition", "text/plain", null);
        return new McpServerFeatures.SyncResourceRegistration(resource, req -> {
            var content = new McpSchema.TextResourceContents(req.uri(), "text/plain", schema);
            return new McpSchema.ReadResourceResult(List.of(content));
        });
    }

    private static final String PRODUCTS_SCHEMA = """
            CREATE TABLE products (
                id             BIGSERIAL PRIMARY KEY,
                name           VARCHAR(255) NOT NULL,
                category       VARCHAR(100) NOT NULL,
                price          DECIMAL(10,2) NOT NULL,
                stock_quantity INTEGER DEFAULT 0,
                created_at     TIMESTAMPTZ DEFAULT NOW()
            );
            -- Indexes: (category), (price)
            -- Sample categories: books, courses, software
            """;

    private static final String ORDERS_SCHEMA = """
            CREATE TABLE orders (
                id          BIGSERIAL PRIMARY KEY,
                customer_id BIGINT REFERENCES customers(id),
                product_id  BIGINT REFERENCES products(id),
                quantity    INTEGER NOT NULL,
                status      VARCHAR(50) NOT NULL,  -- pending, completed, cancelled
                created_at  TIMESTAMPTZ DEFAULT NOW()
            );
            -- Indexes: (customer_id), (status), (created_at)
            """;

    private static final String CUSTOMERS_SCHEMA = """
            CREATE TABLE customers (
                id         BIGSERIAL PRIMARY KEY,
                email      VARCHAR(255) UNIQUE NOT NULL,
                name       VARCHAR(255) NOT NULL,
                tier       VARCHAR(50) DEFAULT 'free',  -- free, pro, enterprise
                created_at TIMESTAMPTZ DEFAULT NOW()
            );
            """;

    private static final String FULL_SCHEMA = PRODUCTS_SCHEMA + "\n" + ORDERS_SCHEMA + "\n" + CUSTOMERS_SCHEMA;
}
