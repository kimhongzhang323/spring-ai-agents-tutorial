package com.masterclass.microservices.enterprise.camel;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CamelRouteTool {

    private static final Logger log = LoggerFactory.getLogger(CamelRouteTool.class);

    private final ProducerTemplate producerTemplate;

    public CamelRouteTool(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @Tool(description = """
            Routes an agent message through an Apache Camel integration pipeline.
            Apache Camel implements 65+ Enterprise Integration Patterns (EIPs) and connects
            to 300+ systems out of the box — databases, file systems, cloud services,
            legacy ERPs, FTP, email, and more. Use this when the destination system requires
            complex transformation, routing logic, or protocol mediation that would be
            impractical to code directly in the agent.
            Input: routeUri (the Camel endpoint URI, e.g. 'direct:processOrder'),
            payload (the message body to route).
            Returns: the result from the Camel route, or confirmation of async dispatch.
            """)
    public String routeViaCamel(String routeUri, String payload) {
        try {
            Object result = producerTemplate.requestBody(routeUri, payload);
            log.debug("Camel route invoked: uri={}", routeUri);
            return result != null ? result.toString() : "Routed to " + routeUri;
        } catch (Exception e) {
            log.error("Camel route failed: uri={}", routeUri, e);
            return "Camel route error: " + e.getMessage();
        }
    }
}
