package com.masterclass.microservices.messaging.pulsar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.stereotype.Component;

@Component
public class PulsarTool {

    private static final Logger log = LoggerFactory.getLogger(PulsarTool.class);
    private static final String TOPIC = "persistent://public/default/agent-events";

    private final PulsarTemplate<String> pulsarTemplate;

    public PulsarTool(PulsarTemplate<String> pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    @Tool(description = """
            Publishes an agent event to Apache Pulsar topic 'agent-events'.
            Pulsar is a cloud-native alternative to Kafka with built-in multi-tenancy,
            geo-replication, and tiered storage. Use this for large-scale multi-tenant
            AI SaaS platforms where tenant isolation at the namespace level is required,
            or when you need Pulsar's unique subscription models (exclusive, shared, failover, key-shared).
            Input: message payload string.
            Returns: Pulsar message ID.
            """)
    public String publishToPulsar(String payload) {
        try {
            var msgId = pulsarTemplate.send(TOPIC, payload);
            log.debug("Pulsar published: topic={} msgId={}", TOPIC, msgId);
            return "Published to Pulsar topic '%s'. MessageId: %s".formatted(TOPIC, msgId);
        } catch (Exception e) {
            log.error("Pulsar publish failed", e);
            return "Pulsar publish failed: " + e.getMessage();
        }
    }
}
