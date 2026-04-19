package com.masterclass.microservices.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes raw events from Kafka, enriches them with LLM classification,
 * then republishes to an enriched topic. Demonstrates event-driven LLM pipelines.
 */
@Component
public class KafkaEventEnricher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventEnricher.class);

    private final ChatClient chatClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventEnricher(ChatClient.Builder chatClientBuilder,
                               KafkaTemplate<String, String> kafkaTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "raw-events", groupId = "agent-enricher-group")
    public void enrichEvent(String rawEvent) {
        log.debug("Enriching raw Kafka event");

        String classification = chatClient.prompt()
                .system("""
                        You are an event classifier. Given a raw event payload,
                        respond with a JSON object:
                        {"category": "<CATEGORY>", "severity": "<LOW|MEDIUM|HIGH>", "summary": "<one sentence>"}
                        Respond only with valid JSON, no explanation.
                        """)
                .user("Classify this event: " + rawEvent)
                .call()
                .content();

        String enriched = """
                {"original":%s,"enrichment":%s}""".formatted(rawEvent, classification);
        kafkaTemplate.send("enriched-events", enriched);
        log.debug("Published enriched event to enriched-events topic");
    }
}
