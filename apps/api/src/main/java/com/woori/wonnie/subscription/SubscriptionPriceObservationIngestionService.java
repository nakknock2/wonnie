package com.woori.wonnie.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woori.wonnie.api.ApiDtos.SubscriptionPriceObservationRequest;
import com.woori.wonnie.dashboard.DashboardService;
import com.woori.wonnie.outbox.OutboxEvent;
import com.woori.wonnie.outbox.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionPriceObservationIngestionService {
    public static final String EVENT_TYPE = "subscription.price-observation";

    private final SubscriptionRepository subscriptionRepository;
    private final OutboxEventRepository outboxRepository;
    private final SubscriptionPriceObservationService observationService;
    private final ObjectMapper objectMapper;
    private final boolean messagingEnabled;

    public SubscriptionPriceObservationIngestionService(
            SubscriptionRepository subscriptionRepository,
            OutboxEventRepository outboxRepository,
            SubscriptionPriceObservationService observationService,
            ObjectMapper objectMapper,
            @Value("${app.messaging.enabled:false}") boolean messagingEnabled
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.outboxRepository = outboxRepository;
        this.observationService = observationService;
        this.objectMapper = objectMapper;
        this.messagingEnabled = messagingEnabled;
    }

    @Transactional
    public String accept(SubscriptionPriceObservationRequest request) {
        if ((request.currency() == null) != (request.originalAmount() == null)) {
            throw new IllegalArgumentException("원통화와 원통화 금액을 함께 전달해야 합니다.");
        }
        var subscription = subscriptionRepository.findById(request.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));
        if (!subscription.getCustomerId().equals(DashboardService.DEMO_CUSTOMER_ID)) {
            throw new IllegalArgumentException("현재 데모 고객의 구독만 처리할 수 있습니다.");
        }
        var event = new SubscriptionPriceObservationEvent(
                request.eventId(), DashboardService.DEMO_CUSTOMER_ID, subscription.getId(),
                request.amount(), request.observedAt(), request.currency(), request.originalAmount(), request.exchangeRate());

        if (!messagingEnabled) {
            observationService.process(event);
            return "PROCESSED";
        }

        var deduplicationKey = "subscription-price-observation:" + request.eventId();
        if (!outboxRepository.existsByDeduplicationKey(deduplicationKey)) {
            try {
                var payload = objectMapper.writeValueAsString(event);
                outboxRepository.save(OutboxEvent.pending(
                        "SUBSCRIPTION", subscription.getId(), EVENT_TYPE, payload, deduplicationKey));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("결제 금액 변경 이벤트를 저장할 수 없습니다.", exception);
            }
        }
        return "QUEUED";
    }
}
