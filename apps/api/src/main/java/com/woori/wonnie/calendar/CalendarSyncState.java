package com.woori.wonnie.calendar;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "calendar_sync_state")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarSyncState {
    @Id private String customerId;
    private String principalName;
    private String channelId;
    private String channelToken;
    private String resourceId;
    private LocalDateTime watchExpiresAt;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime updatedAt;

    public static CalendarSyncState create(String customerId, String principalName) {
        var state = new CalendarSyncState();
        state.customerId = customerId;
        state.principalName = principalName;
        state.updatedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        return state;
    }

    public void synced() {
        lastSyncedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        updatedAt = lastSyncedAt;
    }

    public void watched(String channelId, String channelToken, String resourceId, LocalDateTime expiresAt) {
        this.channelId = channelId;
        this.channelToken = channelToken;
        this.resourceId = resourceId;
        this.watchExpiresAt = expiresAt;
        this.updatedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    }

    public boolean watchNeedsRenewal(LocalDateTime now) {
        return watchExpiresAt == null || watchExpiresAt.isBefore(now.plusDays(1));
    }
}
