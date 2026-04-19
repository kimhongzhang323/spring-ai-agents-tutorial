package com.masterclass.microservices.databases.dynamodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DynamoDbTool {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTool.class);

    private final DynamoDbClient dynamoDbClient;

    public DynamoDbTool(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Tool(description = """
            Retrieves an item from AWS DynamoDB by its partition key.
            DynamoDB is a fully managed, serverless NoSQL database with single-digit millisecond
            latency at any scale. Use this when the agent needs to look up a specific item
            by its exact key — user profiles, session data, product configurations.
            Input: tableName (e.g. 'AgentSessions'), partitionKeyName (e.g. 'userId'),
            partitionKeyValue (the actual key value).
            Returns: the DynamoDB item as JSON, or 'Not found' if the key does not exist.
            """)
    public String getDynamoDbItem(String tableName, String partitionKeyName, String partitionKeyValue) {
        try {
            GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(partitionKeyName, AttributeValue.fromS(partitionKeyValue)))
                    .build());
            if (!response.hasItem() || response.item().isEmpty()) {
                return "Item not found in table '%s' for key '%s'='%s'"
                        .formatted(tableName, partitionKeyName, partitionKeyValue);
            }
            String result = response.item().entrySet().stream()
                    .map(e -> "\"%s\":\"%s\"".formatted(e.getKey(), e.getValue().s()))
                    .collect(Collectors.joining(",", "{", "}"));
            log.debug("DynamoDB get: table={} key={}", tableName, partitionKeyValue);
            return result;
        } catch (Exception e) {
            log.error("DynamoDB get failed: table={} key={}", tableName, partitionKeyValue, e);
            return "DynamoDB error: " + e.getMessage();
        }
    }

    @Tool(description = """
            Puts (upserts) an item into a DynamoDB table.
            Use this when the agent needs to persist state, decisions, or user-specific
            data into a serverless key-value store that scales to millions of requests per second.
            Input: tableName, itemJson (JSON object where each field becomes a DynamoDB attribute).
            Returns: confirmation of successful put.
            """)
    public String putDynamoDbItem(String tableName, String partitionKeyName, String partitionKeyValue, String valueJson) {
        try {
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                            partitionKeyName, AttributeValue.fromS(partitionKeyValue),
                            "data", AttributeValue.fromS(valueJson)))
                    .build());
            log.debug("DynamoDB put: table={} key={}", tableName, partitionKeyValue);
            return "Item stored in DynamoDB table '%s'".formatted(tableName);
        } catch (Exception e) {
            log.error("DynamoDB put failed", e);
            return "DynamoDB put failed: " + e.getMessage();
        }
    }
}
