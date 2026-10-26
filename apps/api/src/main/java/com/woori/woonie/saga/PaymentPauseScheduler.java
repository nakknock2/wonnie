package com.woori.woonie.saga;

import com.woori.woonie.outbox.OutboxEvent;
import com.woori.woonie.outbox.OutboxEventRepository;
import com.woori.woonie.subscription.SubscriptionRepository;
import com.woori.woonie.wallet.SubscriptionWalletRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class PaymentPauseScheduler {

    private final PaymentPauseRepository paymentPauseRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionWalletRepository walletRepository;
    private final OutboxEventRepository outboxRepository;

    public PaymentPauseScheduler(
            PaymentPauseRepository paymentPauseRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionWalletRepository walletRepository,
            OutboxEventRepository outboxRepository
    ) {
        this.paymentPauseRepository = paymentPauseRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.walletRepository = walletRepository;
        this.outboxRepository = outboxRepository;
    }

    @Scheduled(fixedDelayString = "${app.payment-pause.reconcile-delay-ms:60000}")
    @Transactional
    public void reconcile() {
        var today = LocalDate.now();

        paymentPauseRepository
                .findAllByStatusAndStartsAtLessThanEqual(PaymentPauseStatus.SCHEDULED, today)
                .forEach(pause -> {
                    var subscription = subscriptionRepository.findById(pause.getSubscriptionId()).orElseThrow();
                    var wallet = walletRepository.findBySubscriptionId(pause.getSubscriptionId()).orElseThrow();
                    wallet.pauseFunding();
                    wallet.pauseVirtualCard();
                    subscription.activatePaymentPause();
                    pause.activate();
                    outboxRepository.save(OutboxEvent.pending(
                            "SUBSCRIPTION",
                            subscription.getId(),
                            "SUBSCRIPTION_PAYMENT_PAUSE_ACTIVATED",
                            "{\"paymentPauseId\":\"" + pause.getId() + "\"}"
                    ));
                });

        paymentPauseRepository
                .findAllByStatusAndEndsAtBefore(PaymentPauseStatus.ACTIVE, today)
                .forEach(pause -> {
                    var subscription = subscriptionRepository.findById(pause.getSubscriptionId()).orElseThrow();
                    var wallet = walletRepository.findBySubscriptionId(pause.getSubscriptionId()).orElseThrow();
                    wallet.resume();
                    subscription.resumePayment();
                    pause.complete();
                    outboxRepository.save(OutboxEvent.pending(
                            "SUBSCRIPTION",
                            subscription.getId(),
                            "SUBSCRIPTION_PAYMENT_RESUMED",
                            "{\"paymentPauseId\":\"" + pause.getId() + "\"}"
                    ));
                });
    }
}
