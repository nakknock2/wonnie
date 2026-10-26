package com.woori.woonie.api;

import com.woori.woonie.action.ActionPlanService;
import com.woori.woonie.api.ApiDtos.AuthChallengeResponse;
import com.woori.woonie.api.ApiDtos.MockFaceIdRequest;
import com.woori.woonie.api.ApiDtos.SagaResult;
import com.woori.woonie.subscription.SubscriptionRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/action-plans")
public class ActionPlanController {

    private final ActionPlanService actionPlanService;
    private final com.woori.woonie.action.ActionPlanRepository actionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;

    public ActionPlanController(
            ActionPlanService actionPlanService,
            com.woori.woonie.action.ActionPlanRepository actionPlanRepository,
            SubscriptionRepository subscriptionRepository
    ) {
        this.actionPlanService = actionPlanService;
        this.actionPlanRepository = actionPlanRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostMapping("/{actionPlanId}/auth-challenge")
    public AuthChallengeResponse createChallenge(@PathVariable String actionPlanId) {
        var challenge = actionPlanService.createChallenge(actionPlanId);
        return new AuthChallengeResponse(
                challenge.getId(),
                challenge.getChallengeValue(),
                challenge.getExpiresAt()
        );
    }

    @PostMapping("/{actionPlanId}/mock-face-id")
    public SagaResult completeMockFaceId(
            @PathVariable String actionPlanId,
            @RequestBody MockFaceIdRequest request
    ) {
        var sagaId = actionPlanService.authenticateAndExecute(
                actionPlanId,
                request.challengeId(),
                request.challenge()
        );
        var plan = actionPlanRepository.findById(actionPlanId).orElseThrow();
        var subscription = subscriptionRepository.findById(plan.getSubscriptionId()).orElseThrow();
        return new SagaResult(
                sagaId,
                "COMPLETED",
                subscription.getName() + " 결제 중지를 예약했어요. 종료일에는 자동 충전을 다시 시작할게요.",
                subscription.getStatus()
        );
    }
}
