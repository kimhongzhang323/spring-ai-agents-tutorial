package com.masterclass.microservices.enterprise.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * Defines Camel integration routes that connect agent decisions to downstream systems.
 * Routes are defined using Camel's fluent DSL — each route is a pipeline.
 */
@Component
public class AgentCamelRoutes extends RouteBuilder {

    @Override
    public void configure() {
        // Error handling for all routes in this context
        errorHandler(deadLetterChannel("direct:dead-letter")
                .maximumRedeliveries(3)
                .redeliveryDelay(1000));

        // Route 1: Agent order → transform → Kafka
        from("direct:processOrder")
                .routeId("order-to-kafka")
                .log("Processing agent order: ${body}")
                .transform().simple("${body.toUpperCase()}")
                .to("kafka:agent-events?brokers={{spring.kafka.bootstrap-servers}}")
                .log("Order routed to Kafka");

        // Route 2: Agent alert → RabbitMQ → email fanout
        from("direct:sendAlert")
                .routeId("alert-fanout")
                .log("Agent alert: ${body}")
                .multicast().parallelProcessing()
                    .to("rabbitmq:agent.exchange?routingKey=agent.task")
                    .to("log:agent-audit?level=WARN")
                .end()
                .log("Alert fanned out to RabbitMQ and audit log");

        // Route 3: Dead letter channel — park failed messages for inspection
        from("direct:dead-letter")
                .routeId("dead-letter")
                .log("Dead letter — failed message: ${body} | cause: ${exception.message}");
    }
}
