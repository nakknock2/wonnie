package com.woori.woonie.saga;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "payment_pause")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentPause {

    @Id
    private String id;
    private String subscriptionId;
    private String actionPlanId;
    private String sagaId;
    private LocalDate startsAt;
    private LocalDate endsAt;

    @Enumerated(EnumType.STRING)
    private PaymentPauseStatus status;

    private LocalDateTime createdAt;

    public PaymentPause(
            String id,
            String subscriptionId,
            String actionPlanId,
            String sagaId,
            LocalDate startsAt,
            LocalDate endsAt
    ) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.actionPlanId = actionPlanId;
        this.sagaId = sagaId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = startsAt.isAfter(LocalDate.now())
                ? PaymentPauseStatus.SCHEDULED
                : PaymentPauseStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void activate() {
        status = PaymentPauseStatus.ACTIVE;
    }

    public void complete() {
        status = PaymentPauseStatus.COMPLETED;
    }
}
