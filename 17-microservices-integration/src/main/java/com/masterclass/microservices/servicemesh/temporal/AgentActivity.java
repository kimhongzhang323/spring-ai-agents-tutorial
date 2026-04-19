package com.masterclass.microservices.servicemesh.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface AgentActivity {

    @ActivityMethod
    String processInput(String input);

    @ActivityMethod
    String validateOutput(String output);

    @ActivityMethod
    String storeResult(String result);
}
