package com.woori.woonie.dashboard;

import com.woori.woonie.api.ApiDtos.CalendarSignalResponse;
import com.woori.woonie.api.ApiDtos.DashboardResponse;
import com.woori.woonie.api.ApiDtos.SubscriptionSummary;
import com.woori.woonie.api.ApiDtos.WalletSummary;
import com.woori.woonie.calendar.CalendarSignalRepository;
import com.woori.woonie.subscription.Subscription;
import com.woori.woonie.subscription.SubscriptionRepository;
import com.woori.woonie.subscription.SubscriptionStatus;
import com.woori.woonie.wallet.SubscriptionWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class DashboardService {

    public static final String DEMO_CUSTOMER_ID = "customer-demo";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionWalletRepository walletRepository;
    private final CalendarSignalRepository calendarSignalRepository;

    public DashboardService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionWalletRepository walletRepository,
            CalendarSignalRepository calendarSignalRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.walletRepository = walletRepository;
        this.calendarSignalRepository = calendarSignalRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        var subscriptions = subscriptionRepository
                .findAllByCustomerIdOrderByNextPaymentDateAsc(DEMO_CUSTOMER_ID);
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
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .map(Subscription::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var activeCount = subscriptions.stream()
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .count();
        var pausedCount = subscriptions.size() - activeCount;
        var next = subscriptions.stream()
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .findFirst()
                .orElse(subscriptions.get(0));

        return new DashboardResponse(
                "김우리",
                total,
                activeCount,
                pausedCount,
                next.getName(),
                next.getNextPaymentDate(),
                subscriptionResponses,
                signals
        );
    }

    private SubscriptionSummary toSummary(Subscription subscription) {
        var wallet = walletRepository.findBySubscriptionId(subscription.getId())
                .orElseThrow(() -> new IllegalStateException("구독 지갑을 찾을 수 없습니다."));
        return new SubscriptionSummary(
                subscription.getId(),
                subscription.getName(),
                subscription.getCategory(),
                subscription.getAmount(),
                subscription.getBillingCycle(),
                subscription.getNextPaymentDate(),
                subscription.getStatus(),
                subscription.getBrandColor(),
                subscription.getInitialText(),
                new WalletSummary(
                        wallet.getBalance(),
                        wallet.isAutoFundingEnabled(),
                        wallet.getStatus().name(),
                        wallet.getVirtualCardLast4(),
                        wallet.getVirtualCardStatus().name()
                )
        );
    }
}
