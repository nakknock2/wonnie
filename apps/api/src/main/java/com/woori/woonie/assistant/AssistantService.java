package com.woori.woonie.assistant;

import com.woori.woonie.action.ActionPlan;
import com.woori.woonie.action.ActionPlanService;
import com.woori.woonie.api.ApiDtos.ActionPlanResponse;
import com.woori.woonie.api.ApiDtos.AssistantResponse;
import com.woori.woonie.calendar.CalendarSignalRepository;
import com.woori.woonie.dashboard.DashboardService;
import com.woori.woonie.subscription.Subscription;
import com.woori.woonie.subscription.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class AssistantService {

    private static final List<String> SUGGESTIONS = List.of(
            "현재 구독 현황이 적절한지 알려줘",
            "시험 기간 동안 넷플릭스 결제를 잠시 중지해줘",
            "다음 달 결제 예정 구독을 보여줘",
            "비슷한 서비스에 중복 결제하고 있는지 확인해줘"
    );

    private final SubscriptionRepository subscriptionRepository;
    private final CalendarSignalRepository signalRepository;
    private final ActionPlanService actionPlanService;

    public AssistantService(
            SubscriptionRepository subscriptionRepository,
            CalendarSignalRepository signalRepository,
            ActionPlanService actionPlanService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.signalRepository = signalRepository;
        this.actionPlanService = actionPlanService;
    }

    public List<String> suggestions() {
        return SUGGESTIONS;
    }

    public AssistantResponse respond(String message) {
        var normalized = message == null ? "" : message.toLowerCase(Locale.KOREAN);
        var subscriptions = subscriptionRepository
                .findAllByCustomerIdOrderByNextPaymentDateAsc(DashboardService.DEMO_CUSTOMER_ID);

        if (containsAny(normalized, "중지", "쉬", "멈", "시험")) {
            var target = findTargetSubscription(normalized, subscriptions);
            var signal = signalRepository
                    .findAllByCustomerIdOrderByStartsOnAsc(DashboardService.DEMO_CUSTOMER_ID)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("캘린더 신호를 찾을 수 없습니다."));
            var plan = actionPlanService.createPausePlan(
                    DashboardService.DEMO_CUSTOMER_ID,
                    target.getId(),
                    signal.getStartsOn(),
                    signal.getEndsOn(),
                    signal.getTitle() + "에 맞춘 결제 중지 제안"
            );
            return new AssistantResponse(
                    signal.getTitle() + "이 " + signal.getStartsOn() + "부터 잡혀 있어요. "
                            + target.getName() + " 전용 지갑의 자동 충전을 이 기간에 중지할까요?",
                    toResponse(plan, target),
                    List.of("구독 전용 지갑", "자동 충전 중지", "종료일 자동 재개")
            );
        }

        if (containsAny(normalized, "현황", "적절", "분석")) {
            var total = subscriptions.stream()
                    .map(Subscription::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var formatted = NumberFormat.getNumberInstance(Locale.KOREA).format(total);
            return new AssistantResponse(
                    "현재 구독은 " + subscriptions.size() + "개이고 월 " + formatted
                            + "원을 사용하고 있어요. 영상 구독이 2개라 시험 기간에는 Netflix 결제 중지를 검토해볼 만해요.",
                    null,
                    List.of("월 구독비 " + formatted + "원", "활성 구독 " + subscriptions.size() + "개", "영상 구독 2개")
            );
        }

        if (containsAny(normalized, "다음", "예정", "결제")) {
            var next = subscriptions.get(0);
            return new AssistantResponse(
                    "가장 가까운 결제는 " + next.getNextPaymentDate() + "의 " + next.getName()
                            + " " + next.getAmount().toPlainString() + "원이에요.",
                    null,
                    List.of(next.getName(), next.getNextPaymentDate().toString())
            );
        }

        return new AssistantResponse(
                "구독 현황 분석, 다음 결제 확인, 시험 기간 결제 중지를 도와드릴 수 있어요.",
                null,
                List.of("구독 분석", "결제 일정", "결제 중지")
        );
    }

    private Subscription findTargetSubscription(String message, List<Subscription> subscriptions) {
        return subscriptions.stream()
                .filter(subscription -> message.contains(subscription.getName().toLowerCase(Locale.KOREAN)))
                .findFirst()
                .orElseGet(() -> subscriptions.stream()
                        .filter(subscription -> subscription.getName().equals("Netflix"))
                        .findFirst()
                        .orElseThrow());
    }

    private ActionPlanResponse toResponse(ActionPlan plan, Subscription subscription) {
        return new ActionPlanResponse(
                plan.getId(),
                plan.getSubscriptionId(),
                subscription.getName(),
                plan.getActionType(),
                plan.getStartsOn(),
                plan.getEndsOn(),
                plan.getReason(),
                plan.getWarning(),
                plan.getStatus().name()
        );
    }

    private boolean containsAny(String value, String... candidates) {
        for (var candidate : candidates) {
            if (value.contains(candidate)) return true;
        }
        return false;
    }
}
