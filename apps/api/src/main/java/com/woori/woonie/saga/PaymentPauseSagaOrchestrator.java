package com.woori.woonie.saga;

import org.springframework.stereotype.Service;

@Service
public class PaymentPauseSagaOrchestrator {

    private final SagaTransactionService transactions;

    public PaymentPauseSagaOrchestrator(SagaTransactionService transactions) {
        this.transactions = transactions;
    }

    public String start(String actionPlanId) {
        var sagaId = transactions.create(actionPlanId);
        try {
            transactions.disableFunding(sagaId, actionPlanId);
            transactions.pauseVirtualCard(sagaId, actionPlanId);
            transactions.scheduleResume(sagaId, actionPlanId);
            transactions.complete(sagaId, actionPlanId);
            return sagaId;
        } catch (Exception exception) {
            transactions.compensate(sagaId, actionPlanId, exception);
            throw exception;
        }
    }
}
