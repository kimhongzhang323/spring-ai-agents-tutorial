package com.masterclass.antihallucination.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared mutable context threaded through all steps of a workflow execution.
 * Step outputs are stored here so later steps can reference them via {{key}} in prompt templates.
 */
public class WorkflowContext {

    private final String workflowId;
    private final Map<String, String> values = new ConcurrentHashMap<>();

    public WorkflowContext(String workflowId) {
        this.workflowId = workflowId;
    }

    public String workflowId() {
        return workflowId;
    }

    public void put(String key, String value) {
        values.put(key, value);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    /**
     * Interpolates {{key}} placeholders in a prompt template with values from this context.
     */
    public String interpolate(String template) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    public Map<String, String> snapshot() {
        return new HashMap<>(values);
    }
}
