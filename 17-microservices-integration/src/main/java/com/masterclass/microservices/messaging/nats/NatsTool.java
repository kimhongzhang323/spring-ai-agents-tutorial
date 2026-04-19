package com.masterclass.microservices.messaging.nats;

import io.nats.client.Connection;
import io.nats.client.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class NatsTool {

    private static final Logger log = LoggerFactory.getLogger(NatsTool.class);

    private final Connection natsConnection;

    public NatsTool(Connection natsConnection) {
        this.natsConnection = natsConnection;
    }

    @Tool(description = """
            Publishes a message to NATS on the subject 'agent.tasks'.
            NATS is an ultra-low-latency (<1ms) messaging system ideal for real-time
            agent-to-agent communication within the same data center or edge cluster.
            Unlike Kafka or RabbitMQ, NATS does not persist messages by default —
            use NATS JetStream variant for persistence.
            Input: subject suffix (appended to 'agent.'), payload string.
            Returns: publish confirmation.
            """)
    public String publishToNats(String subjectSuffix, String payload) {
        String subject = "agent." + subjectSuffix;
        natsConnection.publish(subject, payload.getBytes(StandardCharsets.UTF_8));
        log.debug("NATS publish: subject={}", subject);
        return "Published to NATS subject: " + subject;
    }

    @Tool(description = """
            Sends a NATS request and waits up to 3 seconds for a reply (request/reply pattern).
            NATS request/reply is the fastest RPC mechanism available — ideal when an agent
            needs a near-instant response from another microservice without HTTP overhead.
            Input: subjectSuffix (e.g. 'inventory.check'), requestPayload string.
            Returns: the reply payload, or a timeout message.
            """)
    public String requestFromNats(String subjectSuffix, String requestPayload) {
        String subject = "agent." + subjectSuffix;
        try {
            Message reply = natsConnection.request(
                    subject,
                    requestPayload.getBytes(StandardCharsets.UTF_8),
                    Duration.ofSeconds(3));
            return reply != null
                    ? new String(reply.getData(), StandardCharsets.UTF_8)
                    : "No reply received within 3 seconds";
        } catch (Exception e) {
            log.error("NATS request failed: subject={}", subject, e);
            return "NATS request failed: " + e.getMessage();
        }
    }
}
