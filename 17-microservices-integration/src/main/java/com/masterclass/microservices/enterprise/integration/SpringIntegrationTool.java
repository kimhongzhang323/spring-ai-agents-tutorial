package com.masterclass.microservices.enterprise.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SpringIntegrationTool {

    private static final Logger log = LoggerFactory.getLogger(SpringIntegrationTool.class);

    private final DirectChannel agentInputChannel;

    public SpringIntegrationTool(DirectChannel agentInputChannel) {
        this.agentInputChannel = agentInputChannel;
    }

    @Tool(description = """
            Sends an agent message into the Spring Integration pipeline via the agent input channel.
            Spring Integration implements Enterprise Integration Patterns (EIPs) natively in Spring —
            message channels, transformers, routers, splitters, aggregators, and service activators.
            Use this when you need fine-grained pipeline control within a Spring application context
            without the overhead of Apache Camel's external DSL.
            Input: payload (the message content), headerJson (optional JSON of message headers, or '{}').
            Returns: confirmation that the message entered the integration pipeline.
            """)
    public String sendToIntegrationPipeline(String payload, String headerJson) {
        try {
            var msg = MessageBuilder.withPayload(payload)
                    .setHeader("source", "agent")
                    .setHeader("timestamp", System.currentTimeMillis())
                    .build();
            boolean sent = agentInputChannel.send(msg, 3000);
            log.debug("Spring Integration message sent: success={}", sent);
            return sent
                    ? "Message dispatched to Spring Integration pipeline"
                    : "Message send timed out after 3 seconds";
        } catch (Exception e) {
            log.error("Spring Integration send failed", e);
            return "Integration pipeline error: " + e.getMessage();
        }
    }
}
