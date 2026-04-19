package com.masterclass.microservices.messaging.activemq;

import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class ActiveMqTool {

    private static final Logger log = LoggerFactory.getLogger(ActiveMqTool.class);
    private static final String QUEUE = "agent.jms.queue";
    private static final String TOPIC = "agent.jms.topic";

    private final JmsTemplate jmsTemplate;

    public ActiveMqTool(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Tool(description = """
            Sends an agent task message to an ActiveMQ Artemis JMS 2.0 queue.
            Use this when integrating with legacy enterprise systems that rely on JMS —
            SAP, IBM MQ bridges, Oracle AQ, or any Java EE system using the JMS API.
            JMS queues guarantee point-to-point delivery with exactly-once semantics via
            transactional sessions.
            Input: message payload string.
            Returns: confirmation of JMS delivery.
            """)
    public String sendToJmsQueue(String payload) {
        jmsTemplate.send(QUEUE, session -> {
            TextMessage msg = session.createTextMessage(payload);
            msg.setStringProperty("source", "agent");
            return msg;
        });
        log.debug("JMS message sent to queue={}", QUEUE);
        return "Message sent to JMS queue: " + QUEUE;
    }

    @Tool(description = """
            Publishes a message to an ActiveMQ Artemis JMS topic (pub/sub).
            Unlike queues, topics deliver the message to ALL active subscribers simultaneously.
            Use this for broadcasting agent decisions to multiple enterprise systems at once
            (ERP, CRM, auditing service) via the JMS topic model.
            Input: message payload string.
            Returns: confirmation of topic publish.
            """)
    public String publishToJmsTopic(String payload) {
        jmsTemplate.setPubSubDomain(true);
        jmsTemplate.convertAndSend(TOPIC, payload);
        jmsTemplate.setPubSubDomain(false);
        log.debug("JMS message published to topic={}", TOPIC);
        return "Message published to JMS topic: " + TOPIC;
    }
}
