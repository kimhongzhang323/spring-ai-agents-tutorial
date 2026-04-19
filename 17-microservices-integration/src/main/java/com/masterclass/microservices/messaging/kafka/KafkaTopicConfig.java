package com.masterclass.microservices.messaging.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic agentEventsTopic() {
        return TopicBuilder.name("agent-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic rawEventsTopic() {
        return TopicBuilder.name("raw-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic enrichedEventsTopic() {
        return TopicBuilder.name("enriched-events").partitions(3).replicas(1).build();
    }
}
