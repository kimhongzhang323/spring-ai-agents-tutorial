package com.masterclass.microservices.enterprise.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;

@Configuration
@EnableIntegration
public class IntegrationConfig {

    private static final Logger log = LoggerFactory.getLogger(IntegrationConfig.class);

    @Bean
    public DirectChannel agentInputChannel() {
        return MessageChannels.direct("agentInput").getObject();
    }

    @Bean
    public DirectChannel agentOutputChannel() {
        return MessageChannels.direct("agentOutput").getObject();
    }

    @Bean
    public IntegrationFlow agentProcessingFlow() {
        return IntegrationFlow
                .from(agentInputChannel())
                .filter(String.class, msg -> msg != null && !msg.isBlank(),
                        f -> f.discardChannel("nullChannel"))
                .transform(String.class, msg -> msg.trim().toLowerCase())
                .<String, Boolean>route(
                        msg -> msg.contains("urgent"),
                        mapping -> mapping
                                .when(true, c -> c.channel("agentOutputChannel"))
                                .defaultOutputChannel("agentOutputChannel"))
                .get();
    }
}
