package com.woori.wonnie.dashboard;

import com.woori.wonnie.api.ApiDtos.CalendarSignalResponse;
import com.woori.wonnie.api.ApiDtos.DashboardResponse;
import com.woori.wonnie.api.ApiDtos.SubscriptionSummary;
import com.woori.wonnie.calendar.CalendarSignalRepository;
import com.woori.wonnie.subscription.Subscription;
import com.woori.wonnie.subscription.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class DashboardService {

    public static final String DEMO_CUSTOMER_ID = "customer-demo";

    private final SubscriptionRepository subscriptionRepository;
    private final CalendarSignalRepository calendarSignalRepository;

    public DashboardService(
            SubscriptionRepository subscriptionRepository,
            CalendarSignalRepository calendarSignalRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.calendarSignalRepository = calendarSignalRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        var subscriptions = subscriptionRepository
                .findAllByCustomerIdOrderByNextPaymentDateAsc(DEMO_CUSTOMER_ID).stream()
                .filter(subscription -> subscription.getStatus() == com.woori.wonnie.subscription.SubscriptionStatus.ACTIVE)
                .toList();
        var subscriptionResponses = subscriptions.stream().map(this::toSummary).toList();
        var signals = calendarSignalRepository
                .findAllByCustomerIdOrderByStartsOnAsc(DEMO_CUSTOMER_ID)
                .stream()
                .map(signal -> new CalendarSignalResponse(
                        signal.getId(),
                        signal.getTitle(),
                        signal.getStartsOn(),
                        signal.getEndsOn(),
                        signal.getSignalType(),
                        signal.getConfidence()
                ))
                .toList();

        var total = subscriptions.stream()
                .map(Subscription::monthlyEquivalent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var next = subscriptions.isEmpty() ? null : subscriptions.get(0);

        return new DashboardResponse(
                "김우리",
                total,
                subscriptions.size(),
                next == null ? "없음" : next.getName(),
                next == null ? null : next.getNextPaymentDate(),
                subscriptionResponses,
                signals
        );
    }

    private SubscriptionSummary toSummary(Subscription subscription) {
        return new SubscriptionSummary(
                subscription.getId(),
                subscription.getName(),
                subscription.getCategory(),
                subscription.getAmount(),
                subscription.getBillingCycle(),
                subscription.getNextPaymentDate(),
                subscription.getStatus(),
                subscription.getBrandColor(),
                subscription.getInitialText()
        );
    }
}
