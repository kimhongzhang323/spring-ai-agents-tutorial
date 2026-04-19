package com.masterclass.microservices.servicemesh.dapr;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class DaprTool {

    private static final Logger log = LoggerFactory.getLogger(DaprTool.class);

    private final DaprClient daprClient;

    public DaprTool(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    @Tool(description = """
            Publishes an event to a Dapr pub/sub component named 'pubsub'.
            Dapr (Distributed Application Runtime) is a CNCF project that provides
            cloud-agnostic building blocks — the same code runs on Kafka, RabbitMQ,
            Azure Service Bus, or AWS SQS without changes. Use this when you need
            portable, infrastructure-agnostic agent integration that can be deployed
            to any cloud or Kubernetes distribution without vendor lock-in.
            Input: topicName (Dapr topic), message (event payload string).
            Returns: publish confirmation.
            """)
    public String publishViaDapr(String topicName, String message) {
        try {
            daprClient.publishEvent("pubsub", topicName, message).block();
            log.debug("Dapr pub/sub published: topic={}", topicName);
            return "Published to Dapr topic '%s' via 'pubsub' component".formatted(topicName);
        } catch (Exception e) {
            log.error("Dapr publish failed: topic={}", topicName, e);
            return "Dapr publish failed: " + e.getMessage();
        }
    }

    @Tool(description = """
            Invokes another microservice via Dapr service invocation (sidecar proxy).
            Dapr handles service discovery, retries, mTLS, and observability automatically.
            Use this to call any other Dapr-enabled microservice by its app ID without
            knowing its IP address or port — Dapr resolves it at runtime.
            Input: appId (the target service's Dapr app ID), method (HTTP method like 'GET' or 'POST'),
            path (the endpoint path), body (request body or empty string for GET).
            Returns: the service response body.
            """)
    public String invokeViaDapr(String appId, String method, String path, String body) {
        try {
            String result = daprClient.invokeMethod(
                    appId, path, body.isEmpty() ? null : body,
                    HttpExtension.valueOf(method), String.class).block();
            log.debug("Dapr service invoke: appId={} method={} path={}", appId, method, path);
            return result != null ? result : "{}";
        } catch (Exception e) {
            log.error("Dapr invoke failed: appId={} path={}", appId, path, e);
            return "Dapr invoke failed: " + e.getMessage();
        }
    }
}
