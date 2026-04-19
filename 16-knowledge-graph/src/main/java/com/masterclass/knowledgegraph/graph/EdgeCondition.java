package com.masterclass.knowledgegraph.graph;

/**
 * A conditional edge that inspects GraphState and returns the name of the next
 * node to visit. Return {@code GraphEngine#END} to terminate the run.
 *
 * Mirrors LangGraph's conditional_edges / add_conditional_edges contract.
 */
@FunctionalInterface
public interface EdgeCondition {

    String next(GraphState state);
}
