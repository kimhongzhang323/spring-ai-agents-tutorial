package com.masterclass.knowledgegraph.model;

import java.util.Map;

/**
 * A node in the knowledge graph — a typed entity with arbitrary properties.
 *
 * @param id         unique identifier (e.g. "person:alan-turing")
 * @param type       entity type label (e.g. "Person", "Company", "Concept")
 * @param name       human-readable canonical name
 * @param properties additional key-value attributes
 */
public record KgEntity(
        String id,
        String type,
        String name,
        Map<String, String> properties
) {
    public static KgEntity of(String id, String type, String name) {
        return new KgEntity(id, type, name, Map.of());
    }
}
