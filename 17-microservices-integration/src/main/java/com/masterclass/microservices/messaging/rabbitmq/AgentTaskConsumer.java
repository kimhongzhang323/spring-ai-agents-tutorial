package com.masterclass.microservices.messaging.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AgentTaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(AgentTaskConsumer.class);

    @RabbitListener(queues = "agent.task.queue")
    public void consumeTask(String payload) {
        log.info("RabbitMQ consumer received agent task: length={}", payload.length());
        // In production: hand off to a processing service or another agent
    }
}
