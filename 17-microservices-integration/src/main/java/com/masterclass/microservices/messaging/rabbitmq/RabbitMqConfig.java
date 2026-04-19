package com.masterclass.microservices.messaging.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange agentExchange() {
        return new TopicExchange("agent.exchange", true, false);
    }

    @Bean
    public Queue agentTaskQueue() {
        return QueueBuilder.durable("agent.task.queue").build();
    }

    @Bean
    public Queue agentResultQueue() {
        return QueueBuilder.durable("agent.result.queue").build();
    }

    @Bean
    public Binding taskBinding(Queue agentTaskQueue, TopicExchange agentExchange) {
        return BindingBuilder.bind(agentTaskQueue).to(agentExchange).with("agent.task");
    }

    @Bean
    public Binding replyBinding(Queue agentResultQueue, TopicExchange agentExchange) {
        return BindingBuilder.bind(agentResultQueue).to(agentExchange).with("agent.task.reply");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setReplyTimeout(5000);
        return template;
    }
}
