package com.woori.woonie.subscription;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "subscription")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    private String id;
    private String customerId;
    private String name;
    private String category;
    private BigDecimal amount;
    private String billingCycle;
    private LocalDate nextPaymentDate;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private String brandColor;
    private String initialText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void schedulePaymentPause() {
        status = SubscriptionStatus.PAYMENT_PAUSE_SCHEDULED;
        updatedAt = LocalDateTime.now();
    }

    public void activatePaymentPause() {
        status = SubscriptionStatus.PAYMENT_PAUSED;
        updatedAt = LocalDateTime.now();
    }

    public void resumePayment() {
        status = SubscriptionStatus.ACTIVE;
        updatedAt = LocalDateTime.now();
    }
}
