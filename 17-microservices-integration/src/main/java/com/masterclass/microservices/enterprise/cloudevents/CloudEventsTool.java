package com.masterclass.microservices.enterprise.cloudevents;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Wraps agent events in the CloudEvents specification envelope before publishing.
 * CloudEvents is a CNCF standard that makes events interoperable across brokers,
 * clouds, and languages — any consumer that understands CloudEvents can process
 * the event regardless of the underlying transport.
 */
@Component
public class CloudEventsTool {

    private static final Logger log = LoggerFactory.getLogger(CloudEventsTool.class);
    private static final String CE_TOPIC = "cloudevents";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public CloudEventsTool(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Tool(description = """
            Wraps an agent decision in a CloudEvents v1.0 envelope and publishes it to Kafka.
            CloudEvents is a CNCF standard specification for event metadata — it ensures
            interoperability between different message brokers, cloud providers, and languages.
            Any system that understands CloudEvents (Knative, Dapr, Azure EventGrid, AWS EventBridge)
            can consume the event without knowing the producer.
            Use this when building cloud-agnostic event-driven systems where portability matters.
            Input: eventType (e.g. 'com.masterclass.agent.decision'),
            source (e.g. '/agents/orchestrator'), dataJson (the event payload as JSON).
            Returns: the serialized CloudEvent with its generated ID.
            """)
    public String publishCloudEvent(String eventType, String source, String dataJson) {
        try {
            CloudEvent event = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withType(eventType)
                    .withSource(URI.create(source))
                    .withDataContentType("application/json")
                    .withTime(OffsetDateTime.now())
                    .withData(dataJson.getBytes())
                    .build();

            EventFormat format = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
            String serialized = new String(format.serialize(event));

            kafkaTemplate.send(CE_TOPIC, event.getId(), serialized);
            log.debug("CloudEvent published: type={} source={} id={}", eventType, source, event.getId());
            return "CloudEvent published. ID: %s | Type: %s".formatted(event.getId(), eventType);
        } catch (Exception e) {
            log.error("CloudEvent publish failed", e);
            return "CloudEvent error: " + e.getMessage();
        }
    }
}
