package com.masterclass.microservices.messaging.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPubSubTool {

    private static final Logger log = LoggerFactory.getLogger(RedisPubSubTool.class);
    private static final String CHANNEL = "agent:notifications";

    private final StringRedisTemplate redisTemplate;

    public RedisPubSubTool(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Tool(description = """
            Publishes a real-time notification to the Redis Pub/Sub channel 'agent:notifications'.
            All active subscribers (e.g. WebSocket gateways, dashboards) receive it instantly.
            Use this for broadcasting agent status updates, alerts, or streaming token-by-token
            responses to connected clients without polling.
            Input: message (the notification text to broadcast).
            Returns: confirmation of how many subscribers received the message.
            """)
    public String publishToRedisPubSub(String message) {
        Long subscribers = redisTemplate.convertAndSend(CHANNEL, message);
        log.debug("Redis Pub/Sub published to channel={} subscribers={}", CHANNEL, subscribers);
        return "Published to channel '%s' — received by %d subscriber(s)".formatted(CHANNEL, subscribers);
    }
}
