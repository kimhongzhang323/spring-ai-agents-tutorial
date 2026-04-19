package com.masterclass.microservices.orchestrator;

import java.util.List;
import java.util.Map;

public record IntegrationCatalog(Map<String, List<String>> integrations) {

    public static IntegrationCatalog full() {
        return new IntegrationCatalog(Map.of(
                "messaging", List.of(
                        "RabbitMQ (AMQP — work queues, request/reply)",
                        "Apache Kafka (event streaming, fan-out, replay)",
                        "Redis Streams (persistent ordered log)",
                        "Redis Pub/Sub (real-time broadcast)",
                        "NATS (ultra-low-latency, request/reply)",
                        "Apache Pulsar (multi-tenant cloud streaming)",
                        "ActiveMQ Artemis / JMS 2.0 (enterprise messaging)",
                        "Azure Service Bus (cloud enterprise queue)",
                        "AWS SQS + SNS via LocalStack (serverless queuing)"
                ),
                "databases", List.of(
                        "PostgreSQL (relational SQL, ACID)",
                        "MongoDB (document store, unstructured data)",
                        "Apache Cassandra (wide-column, high write throughput)",
                        "Elasticsearch (full-text search, log analytics)",
                        "Neo4j (graph database, relationship traversal)",
                        "ClickHouse (OLAP analytics, columnar storage)",
                        "AWS DynamoDB via LocalStack (serverless key-value)"
                ),
                "cache", List.of(
                        "Redis Semantic Cache (LLM response memoization)",
                        "Hazelcast (distributed in-memory data grid)"
                ),
                "serviceMesh", List.of(
                        "gRPC (binary RPC over HTTP/2, Protocol Buffers)",
                        "GraphQL (flexible query API)",
                        "OpenFeign REST (type-safe HTTP clients)",
                        "Dapr (cloud-agnostic portable building blocks)",
                        "Temporal (durable long-running workflows)"
                ),
                "enterprise", List.of(
                        "Apache Camel (300+ connectors, Enterprise Integration Patterns)",
                        "Spring Integration (Spring-native EIP pipelines)",
                        "CloudEvents v1.0 (CNCF standard interoperable events)"
                )
        ));
    }
}
