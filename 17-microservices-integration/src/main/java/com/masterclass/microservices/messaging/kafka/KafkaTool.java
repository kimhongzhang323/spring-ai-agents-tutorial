package com.masterclass.microservices.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaTool {

    private static final Logger log = LoggerFactory.getLogger(KafkaTool.class);
    private static final String TOPIC = "agent-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaTool(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Tool(description = """
            Publishes a structured event to Apache Kafka topic 'agent-events'.
            Use this when the agent decision must be broadcast to multiple downstream consumers
            simultaneously (fan-out), or when the event must be durably logged for replay and audit.
            Kafka guarantees at-least-once delivery and allows consumers to replay from any offset.
            Input: eventType (e.g. "ORDER_CLASSIFIED"), payload (JSON string of the event data).
            Returns: Kafka partition and offset where the event was written.
            """)
    public String publishToKafka(String eventType, String payload) {
        String message = """
                {"eventType":"%s","payload":%s}""".formatted(eventType, payload);
        var future = kafkaTemplate.send(TOPIC, eventType, message);
        try {
            var result = future.get();
            log.debug("Kafka send: topic={} partition={} offset={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return "Event '%s' published to Kafka partition=%d offset=%d".formatted(
                    eventType,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            log.error("Kafka publish failed", e);
            return "Kafka publish failed: " + e.getMessage();
        }
    }
}
