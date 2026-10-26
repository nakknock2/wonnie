package com.woori.woonie.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class RabbitMessagingConfig {

    public static final String EXCHANGE = "woonie.domain";
    public static final String PAYMENT_WORKFLOW_QUEUE = "payment.workflow";

    @Bean
    TopicExchange domainExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue paymentWorkflowQueue() {
        return QueueBuilder.durable(PAYMENT_WORKFLOW_QUEUE).quorum().build();
    }

    @Bean
    Binding paymentWorkflowBinding(Queue paymentWorkflowQueue, TopicExchange domainExchange) {
        return BindingBuilder.bind(paymentWorkflowQueue)
                .to(domainExchange)
                .with("subscription.payment.#");
    }
}
