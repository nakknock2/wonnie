package com.woori.woonie.saga;

import com.woori.woonie.action.ActionPlanRepository;
import com.woori.woonie.outbox.OutboxEvent;
import com.woori.woonie.outbox.OutboxEventRepository;
import com.woori.woonie.subscription.SubscriptionRepository;
import com.woori.woonie.wallet.SubscriptionWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class SagaTransactionService {

    private final SagaInstanceRepository sagaRepository;
    private final SagaStepRepository stepRepository;
    private final ActionPlanRepository actionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionWalletRepository walletRepository;
    private final PaymentPauseRepository paymentPauseRepository;
    private final OutboxEventRepository outboxRepository;

    public SagaTransactionService(
            SagaInstanceRepository sagaRepository,
            SagaStepRepository stepRepository,
            ActionPlanRepository actionPlanRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionWalletRepository walletRepository,
            PaymentPauseRepository paymentPauseRepository,
            OutboxEventRepository outboxRepository
    ) {
        this.sagaRepository = sagaRepository;
        this.stepRepository = stepRepository;
        this.actionPlanRepository = actionPlanRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.walletRepository = walletRepository;
        this.paymentPauseRepository = paymentPauseRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String create(String actionPlanId) {
        var plan = actionPlanRepository.findById(actionPlanId)
                .orElseThrow(() -> new IllegalArgumentException("작업 계획을 찾을 수 없습니다."));
        var sagaId = UUID.randomUUID().toString();
        sagaRepository.save(new SagaInstance(sagaId, plan.getSubscriptionId()));
        return sagaId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void disableFunding(String sagaId, String actionPlanId) {
        var plan = actionPlanRepository.findById(actionPlanId).orElseThrow();
        var wallet = walletRepository.findBySubscriptionId(plan.getSubscriptionId())
                .orElseThrow(() -> new IllegalStateException("구독 지갑을 찾을 수 없습니다."));
        if (plan.getStartsOn().isAfter(LocalDate.now())) {
            recordStep(sagaId, "AUTO_FUNDING_DISABLE_SCHEDULED");
        } else {
            wallet.pauseFunding();
            recordStep(sagaId, "AUTO_FUNDING_DISABLED");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pauseVirtualCard(String sagaId, String actionPlanId) {
        var plan = actionPlanRepository.findById(actionPlanId).orElseThrow();
        var wallet = walletRepository.findBySubscriptionId(plan.getSubscriptionId())
                .orElseThrow(() -> new IllegalStateException("가상카드를 찾을 수 없습니다."));
        if (plan.getStartsOn().isAfter(LocalDate.now())) {
            recordStep(sagaId, "VIRTUAL_CARD_PAUSE_SCHEDULED");
        } else {
            wallet.pauseVirtualCard();
            recordStep(sagaId, "VIRTUAL_CARD_PAUSED");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleResume(String sagaId, String actionPlanId) {
        var plan = actionPlanRepository.findById(actionPlanId).orElseThrow();
        var subscription = subscriptionRepository.findById(plan.getSubscriptionId()).orElseThrow();

        paymentPauseRepository.save(new PaymentPause(
                UUID.randomUUID().toString(),
                subscription.getId(),
                plan.getId(),
                sagaId,
                plan.getStartsOn(),
                plan.getEndsOn()
        ));
        subscription.schedulePaymentPause();
        recordStep(sagaId, "RESUME_SCHEDULED");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String sagaId, String actionPlanId) {
        var plan = actionPlanRepository.findById(actionPlanId).orElseThrow();
        plan.execute();

        var saga = sagaRepository.findById(sagaId).orElseThrow();
        saga.complete();
        stepRepository.save(SagaStep.completed(sagaId, "WORKFLOW_COMPLETED"));

        outboxRepository.save(OutboxEvent.pending(
                "SUBSCRIPTION",
                plan.getSubscriptionId(),
                "SUBSCRIPTION_PAYMENT_PAUSE_SCHEDULED",
                "{\"sagaId\":\"" + sagaId + "\",\"actionPlanId\":\"" + actionPlanId + "\"}"
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(String sagaId, String actionPlanId, Exception exception) {
        var plan = actionPlanRepository.findById(actionPlanId).orElseThrow();
        walletRepository.findBySubscriptionId(plan.getSubscriptionId())
                .ifPresent(wallet -> {
                    wallet.resume();
                    stepRepository.save(SagaStep.completed(sagaId, "WALLET_COMPENSATED"));
                });
        var saga = sagaRepository.findById(sagaId).orElseThrow();
        saga.fail(exception);
    }

    private void recordStep(String sagaId, String stepType) {
        var saga = sagaRepository.findById(sagaId).orElseThrow();
        saga.moveTo(stepType);
        stepRepository.save(SagaStep.completed(sagaId, stepType));
    }
}
