package com.woori.wonnie.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woori.wonnie.config.RabbitMessagingConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class SubscriptionPriceObservationWorker {
    private final ObjectMapper objectMapper;
    private final SubscriptionPriceObservationService observationService;

    public SubscriptionPriceObservationWorker(
            ObjectMapper objectMapper,
            SubscriptionPriceObservationService observationService
    ) {
        this.objectMapper = objectMapper;
        this.observationService = observationService;
    }

    @RabbitListener(queues = RabbitMessagingConfig.PRICE_OBSERVATION_QUEUE)
    public void consume(String payload) throws Exception {
        var event = objectMapper.readValue(payload, SubscriptionPriceObservationEvent.class);
        observationService.process(event);
    }
}
