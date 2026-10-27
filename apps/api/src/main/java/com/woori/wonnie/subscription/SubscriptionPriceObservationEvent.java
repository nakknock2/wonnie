package com.woori.wonnie.subscription;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionPriceObservationEvent(
        String eventId,
        String customerId,
        String subscriptionId,
        BigDecimal amount,
        LocalDateTime observedAt,
        String currency,
        BigDecimal originalAmount,
        BigDecimal exchangeRate
) {
}
