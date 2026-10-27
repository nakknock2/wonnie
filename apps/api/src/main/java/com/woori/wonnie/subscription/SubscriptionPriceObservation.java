package com.woori.wonnie.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "subscription_price_observation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionPriceObservation {
    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String subscriptionId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal previousAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal observedAmount;

    @Column(nullable = false)
    private LocalDateTime observedAt;

    @Column(nullable = false)
    private boolean applied;

    @Column(nullable = false)
    private boolean priceIncreased;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(length = 3)
    private String currency;
    @Column(precision = 15, scale = 2)
    private BigDecimal originalAmount;
    @Column(precision = 15, scale = 6)
    private BigDecimal exchangeRate;
    private String changeReason;

    public void currencyEvidence(String currency, BigDecimal originalAmount, BigDecimal exchangeRate, String reason) {
        this.currency=currency;this.originalAmount=originalAmount;this.exchangeRate=exchangeRate;this.changeReason=reason;
    }

    public static SubscriptionPriceObservation of(
            String eventId,
            String subscriptionId,
            BigDecimal previousAmount,
            BigDecimal observedAmount,
            LocalDateTime observedAt,
            boolean applied,
            boolean priceIncreased
    ) {
        var observation = new SubscriptionPriceObservation();
        observation.id = UUID.randomUUID().toString();
        observation.eventId = eventId;
        observation.subscriptionId = subscriptionId;
        observation.previousAmount = previousAmount;
        observation.observedAmount = observedAmount;
        observation.observedAt = observedAt;
        observation.applied = applied;
        observation.priceIncreased = priceIncreased;
        return observation;
    }
}
