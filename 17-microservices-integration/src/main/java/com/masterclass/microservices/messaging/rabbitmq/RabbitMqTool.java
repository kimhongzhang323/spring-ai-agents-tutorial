package com.masterclass.microservices.messaging.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqTool {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqTool.class);
    private static final String EXCHANGE = "agent.exchange";
    private static final String ROUTING_KEY = "agent.task";

    private final AmqpTemplate amqpTemplate;

    public RabbitMqTool(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    @Tool(description = """
            Publishes an agent task message to RabbitMQ using AMQP protocol.
            Use this when the task requires asynchronous processing by a downstream consumer,
            such as sending a job to a worker queue, triggering a background workflow,
            or decoupling the agent decision from the execution side-effect.
            Returns a confirmation message with the routing details.
            Input: the task payload as a plain string.
            """)
    public String publishToRabbitMq(String taskPayload) {
        amqpTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, taskPayload);
        log.debug("Published to RabbitMQ exchange={} key={}", EXCHANGE, ROUTING_KEY);
        return "Task published to RabbitMQ exchange '%s' with routing key '%s'".formatted(EXCHANGE, ROUTING_KEY);
    }

    @Tool(description = """
            Sends a message to RabbitMQ and waits synchronously for the reply (request/reply pattern).
            Use this when the agent needs an immediate answer from a downstream service
            without making a direct HTTP call — for example, querying a price-calculation service
            or a validation service via the message broker.
            Input: the request payload. Returns the reply payload as a string.
            """)
    public String requestReplyRabbitMq(String requestPayload) {
        Object reply = amqpTemplate.convertSendAndReceive(EXCHANGE, ROUTING_KEY + ".reply", requestPayload);
        return reply != null ? reply.toString() : "No reply received within timeout";
    }
}
