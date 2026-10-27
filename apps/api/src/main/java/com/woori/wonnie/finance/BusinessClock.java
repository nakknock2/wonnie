package com.woori.wonnie.finance;

import com.woori.wonnie.subscription.SubscriptionRepository;
import org.springframework.stereotype.Component;
import java.time.*;

@Component
public class BusinessClock {
    private final SubscriptionRepository subscriptions;
    public BusinessClock(SubscriptionRepository subscriptions) { this.subscriptions = subscriptions; }
    public LocalDate today() { return subscriptions.demoToday(); }
    public LocalDateTime nowUtc() {
        return LocalDateTime.of(today(), LocalTime.now(ZoneId.of("Asia/Seoul")))
                .atZone(ZoneId.of("Asia/Seoul")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
