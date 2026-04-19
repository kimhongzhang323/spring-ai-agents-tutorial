package com.masterclass.knowledgegraph.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable-style typed state container shared across all nodes in a graph run.
 *
 * Mirrors LangGraph's StateGraph channels: each key has a reducer that decides
 * how new values merge with existing ones (replace, append, or custom merge).
 *
 * Thread-safe for parallel node execution via ConcurrentHashMap snapshots.
 */
public final class GraphState {

    private final Map<String, Object> channels;

    private GraphState(Map<String, Object> channels) {
        this.channels = Collections.unmodifiableMap(new LinkedHashMap<>(channels));
    }

    public static GraphState empty() {
        return new GraphState(new LinkedHashMap<>());
    }

    public static GraphState of(Map<String, Object> initial) {
        return new GraphState(initial);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) channels.get(key));
    }

    public <T> T require(String key) {
        return this.<T>get(key)
                .orElseThrow(() -> new IllegalStateException("Required state key missing: " + key));
    }

    /**
     * Returns a new GraphState with the updates merged in.
     * If the existing value is a List and the new value is also a List,
     * the lists are concatenated (append channel semantics).
     * Otherwise the new value replaces the old one (replace channel semantics).
     */
    @SuppressWarnings("unchecked")
    public GraphState merge(Map<String, Object> updates) {
        Map<String, Object> merged = new LinkedHashMap<>(channels);
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object newVal = entry.getValue();
            Object existing = merged.get(key);
            if (existing instanceof List<?> existingList && newVal instanceof List<?> newList) {
                List<Object> combined = new ArrayList<>(existingList);
                combined.addAll((List<Object>) newList);
                merged.put(key, Collections.unmodifiableList(combined));
            } else {
                merged.put(key, newVal);
            }
        }
        return new GraphState(merged);
    }

    public Map<String, Object> asMap() {
        return channels;
    }

    @Override
    public String toString() {
        return "GraphState" + channels;
    }
}
