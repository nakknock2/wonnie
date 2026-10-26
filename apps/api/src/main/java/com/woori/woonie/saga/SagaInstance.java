package com.woori.woonie.saga;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "saga_instance")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaInstance {

    @Id
    private String id;
    private String sagaType;
    private String aggregateId;
    private String currentState;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    @Version
    private long version;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String failedReason;

    public SagaInstance(String id, String aggregateId) {
        this.id = id;
        this.sagaType = "SUBSCRIPTION_PAYMENT_PAUSE";
        this.aggregateId = aggregateId;
        this.currentState = "REQUESTED";
        this.status = SagaStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void moveTo(String state) {
        currentState = state;
    }

    public void complete() {
        currentState = "COMPLETED";
        status = SagaStatus.COMPLETED;
        completedAt = LocalDateTime.now();
    }

    public void fail(Exception exception) {
        status = SagaStatus.FAILED;
        failedReason = exception.getMessage();
        completedAt = LocalDateTime.now();
    }
}
