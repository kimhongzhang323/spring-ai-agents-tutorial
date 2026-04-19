package com.masterclass.knowledgegraph.agent;

import com.masterclass.knowledgegraph.graph.GraphNode;
import com.masterclass.knowledgegraph.graph.GraphState;
import com.masterclass.knowledgegraph.graph.InterruptException;
import com.masterclass.knowledgegraph.model.KgEntity;
import com.masterclass.knowledgegraph.model.KgRelation;
import com.masterclass.knowledgegraph.model.KnowledgeGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Graph node: validates extracted entities and relations for consistency.
 *
 * Checks:
 *   - Dangling relations (entity ID referenced in a relation but not in graph)
 *   - Low-confidence relations (below threshold)
 *   - Human-in-the-loop interrupt when quality is too low to proceed automatically
 *
 * This node demonstrates the interrupt/resume pattern.
 */
public class ValidateGraphNode implements GraphNode {

    private static final Logger log = LoggerFactory.getLogger(ValidateGraphNode.class);

    private final KnowledgeGraph graph;
    private final double         lowConfidenceThreshold;
    private final int            maxDanglingAllowed;

    public ValidateGraphNode(KnowledgeGraph graph, double lowConfidenceThreshold, int maxDanglingAllowed) {
        this.graph                  = graph;
        this.lowConfidenceThreshold = lowConfidenceThreshold;
        this.maxDanglingAllowed     = maxDanglingAllowed;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GraphState execute(GraphState state) {
        List<KgEntity>   entities  = state.require("extracted_entities");
        List<KgRelation> relations = state.require("extracted_relations");

        List<String> issues = new ArrayList<>();

        // Check for dangling relations
        java.util.Set<String> knownIds = new java.util.HashSet<>();
        entities.forEach(e -> knownIds.add(e.id()));

        for (KgRelation rel : relations) {
            if (!knownIds.contains(rel.fromId())) {
                issues.add("Dangling from-entity: " + rel.fromId());
            }
            if (!knownIds.contains(rel.toId())) {
                issues.add("Dangling to-entity: " + rel.toId());
            }
            if (rel.weight() < lowConfidenceThreshold) {
                issues.add("Low-confidence relation: %s -[%s]-> %s (%.2f)"
                        .formatted(rel.fromId(), rel.relation(), rel.toId(), rel.weight()));
            }
        }

        boolean humanApproved = state.<Boolean>get("human_approved").orElse(false);

        if (issues.size() > maxDanglingAllowed && !humanApproved) {
            // Pause execution and ask a human to review
            String prompt = ("Graph validation found %d issues. Please review and approve to continue.\n\nIssues:\n%s\n\n" +
                    "Reply 'approve' to continue or 'reject' to abort.")
                    .formatted(issues.size(), String.join("\n", issues));
            log.warn("ValidateGraphNode: triggering human-in-the-loop interrupt ({} issues)", issues.size());
            throw new InterruptException(prompt);
        }

        String validationStatus = issues.isEmpty() ? "CLEAN" : "ISSUES_IGNORED";
        log.info("ValidateGraphNode: status={} issues={}", validationStatus, issues.size());

        return GraphState.of(Map.of(
                "validation_status", validationStatus,
                "validation_issues", issues,
                "graph_entities",   graph.entityCount(),
                "graph_relations",  graph.relationCount()
        ));
    }
}
