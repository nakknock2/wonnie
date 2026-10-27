package com.woori.wonnie.consent;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ConsentService {
    public static final String CARD_TX_READ = "CARD_TX_READ";
    public static final String SUBSCRIPTION_ANALYSIS = "SUBSCRIPTION_ANALYSIS";
    public static final String PUSH_D1 = "PUSH_D1";
    public static final String PUSH_PRICE_CHANGE = "PUSH_PRICE_CHANGE";

    private final JdbcTemplate jdbc;

    public ConsentService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public ConsentState state(String customerId) {
        var subscriptionDataAgreed = isAgreed(customerId, CARD_TX_READ)
                && isAgreed(customerId, SUBSCRIPTION_ANALYSIS);
        var d1 = decision(customerId, PUSH_D1);
        var priceChange = decision(customerId, PUSH_PRICE_CHANGE);
        return new ConsentState(subscriptionDataAgreed,
                new NotificationConsentState(d1.decided() && priceChange.decided(), d1.agreed(), priceChange.agreed()));
    }

    @Transactional
    public ConsentState agreeToSubscriptionData(String customerId) {
        saveDecision(customerId, CARD_TX_READ, true, true);
        saveDecision(customerId, SUBSCRIPTION_ANALYSIS, true, true);
        return state(customerId);
    }

    @Transactional
    public ConsentState updateNotifications(String customerId, boolean d1, boolean priceChange) {
        saveDecision(customerId, PUSH_D1, false, d1);
        saveDecision(customerId, PUSH_PRICE_CHANGE, false, priceChange);
        return state(customerId);
    }

    @Transactional(readOnly = true)
    public boolean isAgreed(String customerId, String type) {
        return decision(customerId, type).agreed();
    }

    private Decision decision(String customerId, String type) {
        return jdbc.query("SELECT agreed, decided_at FROM consent WHERE customer_id=? AND consent_type=?",
                rs -> rs.next() ? new Decision(rs.getBoolean("agreed"), rs.getObject("decided_at") != null)
                        : new Decision(false, false), customerId, type);
    }

    private void saveDecision(String customerId, String type, boolean required, boolean agreed) {
        var now = LocalDateTime.now(ZoneOffset.UTC);
        var updated = jdbc.update("UPDATE consent SET required=?, agreed=?, agreed_at=?, withdrawn_at=?, decided_at=? "
                        + "WHERE customer_id=? AND consent_type=?",
                required, agreed, agreed ? now : null, agreed ? null : now, now, customerId, type);
        if (updated == 0) {
            jdbc.update("INSERT INTO consent (id, customer_id, consent_type, required, agreed, agreed_at, withdrawn_at, decided_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    "cons-" + UUID.randomUUID(), customerId, type, required, agreed,
                    agreed ? now : null, agreed ? null : now, now);
        }
    }

    private record Decision(boolean agreed, boolean decided) {
    }

    public record ConsentState(boolean subscriptionDataAgreed, NotificationConsentState notifications) {
    }

    public record NotificationConsentState(boolean decided, boolean d1, boolean priceChange) {
    }
}
