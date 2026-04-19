package com.masterclass.microservices.messaging.azure;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AzureServiceBusTool {

    private static final Logger log = LoggerFactory.getLogger(AzureServiceBusTool.class);

    private final ServiceBusSenderClient senderClient;

    public AzureServiceBusTool(
            @Value("${azure.servicebus.connection-string}") String connectionString,
            @Value("${azure.servicebus.queue-name:agent-tasks}") String queueName) {
        this.senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
    }

    @Tool(description = """
            Sends an agent task message to Azure Service Bus queue 'agent-tasks'.
            Azure Service Bus is the enterprise messaging backbone for Azure-hosted workloads.
            It supports sessions (ordered delivery per customer), dead-letter queues,
            and message deferral — critical for multi-tenant SaaS agent systems.
            Use this when your downstream consumers are Azure Functions, Logic Apps,
            or any Azure-native microservice.
            Input: message body string.
            Returns: confirmation with message ID.
            """)
    public String sendToAzureServiceBus(String messageBody) {
        ServiceBusMessage message = new ServiceBusMessage(messageBody);
        message.setContentType("application/json");
        senderClient.sendMessage(message);
        log.debug("Azure Service Bus message sent: id={}", message.getMessageId());
        return "Message sent to Azure Service Bus. ID: " + message.getMessageId();
    }
}
