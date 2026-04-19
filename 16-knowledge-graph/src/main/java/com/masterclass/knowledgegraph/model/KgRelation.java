package com.masterclass.knowledgegraph.model;

/**
 * A directed, typed edge between two KgEntity nodes.
 *
 * @param fromId     source entity ID
 * @param relation   relationship label (e.g. "WORKS_AT", "INVENTED", "KNOWS")
 * @param toId       target entity ID
 * @param weight     optional confidence score [0.0, 1.0]
 */
public record KgRelation(
        String fromId,
        String relation,
        String toId,
        double weight
) {
    public static KgRelation of(String from, String relation, String to) {
        return new KgRelation(from, relation, to, 1.0);
    }
}
