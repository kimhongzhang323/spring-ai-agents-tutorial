package com.masterclass.microservices.servicemesh.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class AgentWorkflowImpl implements AgentWorkflow {

    private static final Logger log = Workflow.getLogger(AgentWorkflowImpl.class);

    private final AgentActivity activity = Workflow.newActivityStub(
            AgentActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .build());

    @Override
    public String execute(String input) {
        log.info("Agent workflow executing with input length={}", input.length());
        String processed = activity.processInput(input);
        String validated = activity.validateOutput(processed);
        String stored = activity.storeResult(validated);
        return stored;
    }
}
