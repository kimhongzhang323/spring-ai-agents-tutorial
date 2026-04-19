package com.masterclass.microservices.messaging.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RedisStreamsTool {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamsTool.class);
    private static final String STREAM_KEY = "agent:stream";

    private final StringRedisTemplate redisTemplate;

    public RedisStreamsTool(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Tool(description = """
            Appends an agent event to a Redis Stream named 'agent:stream'.
            Redis Streams provide a persistent, ordered, replayable log — similar to Kafka but
            built into Redis. Use this for lightweight durable event pipelines where you already
            have Redis deployed and don't need Kafka's throughput guarantees.
            Input: eventType (string), data (string value to store).
            Returns: the Redis Stream entry ID (used to track position).
            """)
    public String appendToRedisStream(String eventType, String data) {
        var ops = redisTemplate.opsForStream();
        var id = ops.add(MapRecord.create(STREAM_KEY, Map.of("eventType", eventType, "data", data)));
        log.debug("Appended to Redis Stream: key={} id={}", STREAM_KEY, id);
        return "Appended to Redis Stream '%s' with ID: %s".formatted(STREAM_KEY, id);
    }

    @Tool(description = """
            Reads the latest 10 entries from the Redis Stream 'agent:stream'.
            Use this to inspect recent agent decisions or replay events for debugging.
            Returns: a JSON array of the most recent stream entries.
            """)
    public String readFromRedisStream() {
        var ops = redisTemplate.opsForStream();
        List<MapRecord<String, Object, Object>> records =
                ops.read(StreamOffset.fromStart(STREAM_KEY));
        if (records == null || records.isEmpty()) {
            return "[]";
        }
        var sb = new StringBuilder("[");
        records.stream().limit(10).forEach(r ->
                sb.append("{\"id\":\"").append(r.getId())
                  .append("\",\"value\":").append(r.getValue()).append("},"));
        if (sb.charAt(sb.length() - 1) == ',') sb.setLength(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }
}
