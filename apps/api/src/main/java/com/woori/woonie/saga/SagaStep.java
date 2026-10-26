package com.woori.woonie.saga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_step")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SagaStep {

    @Id
    private String id;
    private String sagaId;
    private String stepType;
    private String status;
    private int attemptCount;
    private String idempotencyKey;
    private String compensationStatus;
    private LocalDateTime executedAt;
    private String lastError;

    public static SagaStep completed(String sagaId, String stepType) {
        var step = new SagaStep();
        step.id = UUID.randomUUID().toString();
        step.sagaId = sagaId;
        step.stepType = stepType;
        step.status = "COMPLETED";
        step.attemptCount = 1;
        step.idempotencyKey = sagaId + ":" + stepType;
        step.compensationStatus = "NOT_REQUIRED";
        step.executedAt = LocalDateTime.now();
        return step;
    }
}
