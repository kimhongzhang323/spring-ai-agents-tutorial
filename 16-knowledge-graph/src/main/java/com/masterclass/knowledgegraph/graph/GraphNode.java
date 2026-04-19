package com.masterclass.knowledgegraph.graph;

/**
 * A single node in the agent graph.
 *
 * Nodes are pure functions: given a current GraphState, they return a map of
 * state updates. The graph engine merges these updates into the next state via
 * GraphState#merge — identical to LangGraph's node contract.
 *
 * Implementations must be stateless and thread-safe; all mutable state lives
 * in GraphState, not in the node object.
 */
@FunctionalInterface
public interface GraphNode {

    /**
     * @param state  read-only view of the current graph state
     * @return       partial state updates to be merged into the next state
     */
    GraphState execute(GraphState state);
}
