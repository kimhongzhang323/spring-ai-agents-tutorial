package com.masterclass.microservices.servicemesh.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AgentWorkflow {

    @WorkflowMethod
    String execute(String input);
}
