package com.woori.wonnie.calendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CalendarProvider {
    List<CalendarEventSnapshot> events(String principalName, LocalDate from, LocalDate toExclusive);
    WatchRegistration watch(String principalName, String channelId, String channelToken, LocalDateTime expiresAt);
    record WatchRegistration(String resourceId, LocalDateTime expiresAt) {}
}
