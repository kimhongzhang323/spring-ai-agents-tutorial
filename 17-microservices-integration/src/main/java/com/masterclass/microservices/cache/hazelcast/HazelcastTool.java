package com.masterclass.microservices.cache.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class HazelcastTool {

    private static final Logger log = LoggerFactory.getLogger(HazelcastTool.class);
    private static final String MAP_NAME = "agent-shared-state";

    private final HazelcastInstance hazelcastInstance;

    public HazelcastTool(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Tool(description = """
            Stores a key-value pair in the Hazelcast distributed in-memory grid.
            Unlike Redis (single master), Hazelcast is a fully distributed, peer-to-peer
            in-memory data grid — every node owns a partition of the data.
            Use this for sharing agent state across multiple application instances
            in a cluster where all pods must see the same value instantly.
            Data expires after 10 minutes.
            Input: key (string), value (string to store).
            Returns: confirmation of storage.
            """)
    public String putInHazelcast(String key, String value) {
        IMap<String, String> map = hazelcastInstance.getMap(MAP_NAME);
        map.put(key, value, 10, TimeUnit.MINUTES);
        log.debug("Hazelcast put: key={}", key);
        return "Stored in Hazelcast distributed map '%s': key='%s'".formatted(MAP_NAME, key);
    }

    @Tool(description = """
            Retrieves a value from the Hazelcast distributed in-memory grid by key.
            Use this when the agent needs to read shared state that was written by
            another instance or node — for example, a distributed lock state, shared
            configuration, or cluster-wide counter.
            Input: key (string).
            Returns: the stored value, or 'NOT_FOUND' if key does not exist.
            """)
    public String getFromHazelcast(String key) {
        IMap<String, String> map = hazelcastInstance.getMap(MAP_NAME);
        String value = map.get(key);
        if (value == null) {
            log.debug("Hazelcast miss: key={}", key);
            return "NOT_FOUND";
        }
        log.debug("Hazelcast hit: key={}", key);
        return value;
    }
}
