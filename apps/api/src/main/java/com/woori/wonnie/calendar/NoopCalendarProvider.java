package com.woori.wonnie.calendar;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("!google-oidc")
class NoopCalendarProvider implements CalendarProvider {
    public List<CalendarEventSnapshot> events(String principalName, LocalDate from, LocalDate toExclusive) {
        return List.of();
    }
    public WatchRegistration watch(String principalName, String channelId, String channelToken, LocalDateTime expiresAt) {
        return null;
    }
}
