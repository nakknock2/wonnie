package com.woori.wonnie.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class RabbitMessagingConfig {

    public static final String EXCHANGE = "wonnie.domain";
    public static final String PRICE_OBSERVATION_QUEUE = "subscription.price-observations";

    @Bean
    TopicExchange domainExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue subscriptionPriceObservationQueue() {
        return QueueBuilder.durable(PRICE_OBSERVATION_QUEUE).quorum().build();
    }

    @Bean
    Binding subscriptionPriceObservationBinding(Queue subscriptionPriceObservationQueue,
                                                TopicExchange domainExchange) {
        return BindingBuilder.bind(subscriptionPriceObservationQueue)
                .to(domainExchange)
                .with(com.woori.wonnie.subscription.SubscriptionPriceObservationIngestionService.EVENT_TYPE);
    }
}
