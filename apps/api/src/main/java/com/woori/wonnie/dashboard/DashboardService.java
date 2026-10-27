package com.woori.wonnie.dashboard;

import com.woori.wonnie.api.ApiDtos.CalendarSignalResponse;
import com.woori.wonnie.api.ApiDtos.DashboardResponse;
import com.woori.wonnie.api.ApiDtos.SubscriptionSummary;
import com.woori.wonnie.auth.CustomerAccountRepository;
import com.woori.wonnie.calendar.CalendarSignalRepository;
import com.woori.wonnie.subscription.Subscription;
import com.woori.wonnie.subscription.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class DashboardService {

    public static final String DEMO_CUSTOMER_ID = "cust-kimwoori";

    private final SubscriptionRepository subscriptionRepository;
    private final CalendarSignalRepository calendarSignalRepository;
    private final CustomerAccountRepository customerAccountRepository;

    public DashboardService(
            SubscriptionRepository subscriptionRepository,
            CalendarSignalRepository calendarSignalRepository,
            CustomerAccountRepository customerAccountRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.calendarSignalRepository = calendarSignalRepository;
        this.customerAccountRepository = customerAccountRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String customerId) {
        var subscriptions = subscriptionRepository
                .findAllByCustomerIdOrderByNextPaymentDateAsc(customerId).stream()
                .filter(subscription -> subscription.getStatus() == com.woori.wonnie.subscription.SubscriptionStatus.ACTIVE)
                .toList();
        var subscriptionResponses = subscriptions.stream().map(this::toSummary).toList();
        var signals = calendarSignalRepository
                .findAllByCustomerIdOrderByStartsOnAsc(customerId)
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
                customerAccountRepository.findById(customerId).map(account -> account.getName()).orElse("고객"),
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
