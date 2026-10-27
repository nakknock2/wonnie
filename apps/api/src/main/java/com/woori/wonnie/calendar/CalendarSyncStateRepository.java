package com.woori.wonnie.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CalendarSyncStateRepository extends JpaRepository<CalendarSyncState, String> {
    Optional<CalendarSyncState> findByChannelId(String channelId);
}
