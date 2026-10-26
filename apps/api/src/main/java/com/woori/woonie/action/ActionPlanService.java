package com.woori.woonie.action;

import com.woori.woonie.auth.StepUpAuthenticationPort;
import com.woori.woonie.saga.PaymentPauseSagaOrchestrator;
import com.woori.woonie.subscription.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ActionPlanService {

    private final ActionPlanRepository actionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StepUpAuthenticationPort authentication;
    private final PaymentPauseSagaOrchestrator sagaOrchestrator;

    public ActionPlanService(
            ActionPlanRepository actionPlanRepository,
            SubscriptionRepository subscriptionRepository,
            StepUpAuthenticationPort authentication,
            PaymentPauseSagaOrchestrator sagaOrchestrator
    ) {
        this.actionPlanRepository = actionPlanRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.authentication = authentication;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Transactional
    public ActionPlan createPausePlan(
            String customerId,
            String subscriptionId,
            LocalDate startsOn,
            LocalDate endsOn,
            String reason
    ) {
        subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독을 찾을 수 없습니다."));
        return actionPlanRepository.save(new ActionPlan(
                UUID.randomUUID().toString(),
                customerId,
                subscriptionId,
                startsOn,
                endsOn,
                reason,
                "중지 기간 종료 후 구독 업체가 결제를 다시 시도할 수 있어요."
        ));
    }

    @Transactional
    public com.woori.woonie.auth.AuthChallenge createChallenge(String actionPlanId) {
        var plan = actionPlanRepository.findById(actionPlanId)
                .orElseThrow(() -> new IllegalArgumentException("작업 계획을 찾을 수 없습니다."));
        plan.startAuthentication();
        return authentication.createChallenge(actionPlanId);
    }

    public String authenticateAndExecute(
            String actionPlanId,
            String challengeId,
            String challengeValue
    ) {
        verifyActionPlan(actionPlanId, challengeId, challengeValue);
        return sagaOrchestrator.start(actionPlanId);
    }

    private void verifyActionPlan(
            String actionPlanId,
            String challengeId,
            String challengeValue
    ) {
        var challenge = authentication.verify(challengeId, challengeValue);
        if (!challenge.getActionPlanId().equals(actionPlanId)) {
            throw new IllegalArgumentException("인증 대상 작업이 일치하지 않습니다.");
        }
        var plan = actionPlanRepository.findById(actionPlanId)
                .orElseThrow(() -> new IllegalArgumentException("작업 계획을 찾을 수 없습니다."));
        plan.authenticate();
        actionPlanRepository.save(plan);
    }
}
