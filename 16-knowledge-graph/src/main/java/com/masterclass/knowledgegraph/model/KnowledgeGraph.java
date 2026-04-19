package com.masterclass.knowledgegraph.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory adjacency-list knowledge graph.
 *
 * Thread-safe for concurrent reads; writes are synchronised.
 * Production deployments should back this with Neo4j or a graph DB.
 */
public final class KnowledgeGraph {

    private final Map<String, KgEntity>         entities  = new LinkedHashMap<>();
    private final List<KgRelation>              relations = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Mutations
    // -----------------------------------------------------------------------

    public synchronized void addEntity(KgEntity entity) {
        entities.put(entity.id(), entity);
    }

    public synchronized void addRelation(KgRelation relation) {
        relations.add(relation);
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    public Optional<KgEntity> findById(String id) {
        return Optional.ofNullable(entities.get(id));
    }

    public List<KgEntity> findByType(String type) {
        return entities.values().stream()
                .filter(e -> e.type().equalsIgnoreCase(type))
                .toList();
    }

    public List<KgEntity> findByName(String partialName) {
        String lower = partialName.toLowerCase();
        return entities.values().stream()
                .filter(e -> e.name().toLowerCase().contains(lower))
                .toList();
    }

    public List<KgRelation> relationsFrom(String entityId) {
        return relations.stream()
                .filter(r -> r.fromId().equals(entityId))
                .toList();
    }

    public List<KgRelation> relationsTo(String entityId) {
        return relations.stream()
                .filter(r -> r.toId().equals(entityId))
                .toList();
    }

    public List<KgRelation> relationsOfType(String relationType) {
        return relations.stream()
                .filter(r -> r.relation().equalsIgnoreCase(relationType))
                .toList();
    }

    /**
     * BFS multi-hop traversal up to {@code maxHops} from a starting entity.
     * Returns all reachable entities along with the shortest-path relation chain.
     */
    public List<TraversalResult> traverse(String startId, int maxHops) {
        List<TraversalResult> results = new ArrayList<>();
        Queue<TraversalResult> queue  = new ArrayDeque<>();
        Set<String>            seen   = new HashSet<>();

        seen.add(startId);
        queue.add(new TraversalResult(startId, List.of(), 0));

        while (!queue.isEmpty()) {
            TraversalResult current = queue.poll();
            if (current.hops() >= maxHops) continue;

            for (KgRelation rel : relationsFrom(current.entityId())) {
                if (seen.contains(rel.toId())) continue;
                seen.add(rel.toId());
                List<String> path = new ArrayList<>(current.path());
                path.add(rel.relation());
                TraversalResult next = new TraversalResult(rel.toId(), path, current.hops() + 1);
                results.add(next);
                queue.add(next);
            }
        }
        return results;
    }

    /** Serialize graph to a compact string for LLM context injection. */
    public String toContextString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Knowledge Graph ===\n");
        sb.append("Entities (%d):\n".formatted(entities.size()));
        entities.values().forEach(e ->
                sb.append("  [%s] %s (%s)\n".formatted(e.id(), e.name(), e.type())));
        sb.append("Relations (%d):\n".formatted(relations.size()));
        relations.forEach(r ->
                sb.append("  %s -[%s]-> %s (%.2f)\n".formatted(r.fromId(), r.relation(), r.toId(), r.weight())));
        return sb.toString();
    }

    public int entityCount()   { return entities.size(); }
    public int relationCount() { return relations.size(); }

    public record TraversalResult(String entityId, List<String> path, int hops) {}
}
