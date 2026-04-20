package com.masterclass.microservices.messaging.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes agent tasks from RabbitMQ, runs LLM analysis on the payload,
 * and routes to a result queue. Demonstrates event-driven agent processing:
 * the HTTP request returns immediately after publishing; this consumer
 * handles the work asynchronously on a separate thread.
 */
@Component
public class AgentTaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(AgentTaskConsumer.class);
    private static final String RESULT_EXCHANGE = "agent.exchange";
    private static final String RESULT_KEY = "agent.task.result";

    private final ChatClient chatClient;
    private final RabbitTemplate rabbitTemplate;

    public AgentTaskConsumer(ChatClient.Builder chatClientBuilder, RabbitTemplate rabbitTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Listens on agent.task.queue, analyzes the task with the LLM,
     * and publishes an enriched result to agent.task.result routing key.
     *
     * In production: add Dead Letter Queue (DLQ) binding for failed tasks.
     */
    @RabbitListener(queues = "agent.task.queue")
    public void consumeTask(String payload) {
        log.info("RabbitMQ received agent task: length={}", payload.length());

        try {
            String analysis = chatClient.prompt()
                    .system("""
                            You are a task analyzer. Given a task payload from the agent queue,
                            respond ONLY with valid JSON:
                            {
                              "taskType": "<inferred task type>",
                              "priority": "HIGH|MEDIUM|LOW",
                              "action": "<recommended next action>",
                              "reason": "<one sentence explanation>"
                            }
                            """)
                    .user("Analyze this agent task: " + payload)
                    .call()
                    .content();

            String result = """
                    {"original":%s,"analysis":%s}""".formatted(payload, analysis);
            rabbitTemplate.convertAndSend(RESULT_EXCHANGE, RESULT_KEY, result);
            log.info("Agent task processed and result published: taskType={}",
                    extractField(analysis, "taskType"));

        } catch (Exception e) {
            log.error("Failed to process agent task, will be DLQ'd: {}", e.getMessage(), e);
            // Re-throw so RabbitMQ negatively acknowledges and routes to DLQ
            throw new RuntimeException("Agent task processing failed", e);
        }
    }

    private String extractField(String json, String field) {
        try {
            int idx = json.indexOf("\"" + field + "\":");
            if (idx < 0) return "unknown";
            int start = json.indexOf('"', idx + field.length() + 3) + 1;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
