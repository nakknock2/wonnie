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
        // 수집 이벤트의 시각은 실제 UTC 기준이다. 데모 날짜와 현재 시각을 조합하면
        // 자정 이후 seed 데이터보다 과거가 되어 정상 관측이 STALE로 오판된다.
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
