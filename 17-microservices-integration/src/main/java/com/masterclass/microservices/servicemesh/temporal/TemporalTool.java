package com.masterclass.microservices.servicemesh.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TemporalTool {

    private static final Logger log = LoggerFactory.getLogger(TemporalTool.class);
    private static final String TASK_QUEUE = "agent-workflows";

    private final WorkflowClient workflowClient;

    public TemporalTool(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @Tool(description = """
            Starts a durable, long-running agent workflow in Temporal.
            Temporal provides workflow orchestration with automatic retry, state persistence,
            and replay — if the server crashes mid-execution, the workflow resumes from where
            it left off with zero data loss. Use this for multi-step agent pipelines that
            span minutes, hours, or days (e.g., document review, approval chains,
            multi-day data collection). Unlike Kafka or RabbitMQ, Temporal maintains
            full workflow history and supports versioning of business logic.
            Input: workflowType (name of the registered workflow, e.g. 'DocumentReviewWorkflow'),
            workflowId (unique business identifier), input (JSON string passed to the workflow).
            Returns: workflow run ID for tracking.
            """)
    public String startTemporalWorkflow(String workflowType, String workflowId, String input) {
        try {
            AgentWorkflow workflow = workflowClient.newWorkflowStub(
                    AgentWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(TASK_QUEUE)
                            .setWorkflowId(workflowId)
                            .setWorkflowExecutionTimeout(Duration.ofHours(24))
                            .build());

            WorkflowClient.start(workflow::execute, input);
            log.debug("Temporal workflow started: type={} id={}", workflowType, workflowId);
            return "Temporal workflow started. WorkflowId: %s (track at Temporal UI)".formatted(workflowId);
        } catch (Exception e) {
            log.error("Temporal start failed: type={} id={}", workflowType, workflowId, e);
            return "Temporal workflow start failed: " + e.getMessage();
        }
    }
}
