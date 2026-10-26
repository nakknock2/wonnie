package com.woori.woonie.outbox;

import com.woori.woonie.config.RabbitMessagingConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class OutboxRelay {

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelay(OutboxEventRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${app.messaging.relay-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        repository.findTop50ByStatusOrderByCreatedAtAsc("PENDING").forEach(event -> {
            rabbitTemplate.convertAndSend(
                    RabbitMessagingConfig.EXCHANGE,
                    "subscription.payment.event",
                    event.getPayload()
            );
            event.markPublished();
        });
    }
}
