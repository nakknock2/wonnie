package com.woori.wonnie.calendar;

import com.woori.wonnie.auth.OAuthIdentityRepository;
import com.woori.wonnie.auth.OAuthIdentity;
import com.woori.wonnie.finance.BusinessClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarSyncServiceTests {
    @Mock CalendarProvider provider;
    @Mock CalendarSyncStateRepository states;
    @Mock OAuthIdentityRepository identities;
    @Mock CalendarPauseRecommendationService recommendations;
    @Mock BusinessClock clock;
    CalendarSyncService service;

    @BeforeEach
    void setUp() {
        service = new CalendarSyncService(provider, states, identities, recommendations, clock, Runnable::run);
    }

    @Test
    void acceptedGoogleWebhookImmediatelyRevalidatesTheCurrentMonth() {
        var state = CalendarSyncState.create("cust-1", "google-subject");
        state.watched("channel-1", "secret-token", "resource-1",
                LocalDateTime.now(ZoneOffset.UTC).plusDays(5));
        when(states.findByChannelId("channel-1")).thenReturn(Optional.of(state));
        when(states.findById("cust-1")).thenReturn(Optional.of(state));
        when(clock.today()).thenReturn(LocalDate.of(2026, 10, 27));
        when(provider.events("google-subject", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 1, 1)))
                .thenReturn(List.of());

        assertThat(service.acceptWebhook("channel-1", "secret-token", "exists")).isTrue();

        verify(recommendations).evaluate("cust-1", YearMonth.of(2026, 10), List.of());
        verify(states).save(state);
    }

    @Test
    void rejectsUnknownOrForgedWebhookWithoutReadingTheCalendar() {
        var state = CalendarSyncState.create("cust-1", "google-subject");
        state.watched("channel-1", "secret-token", "resource-1",
                LocalDateTime.now(ZoneOffset.UTC).plusDays(5));
        when(states.findByChannelId("channel-1")).thenReturn(Optional.of(state));

        assertThat(service.acceptWebhook("channel-1", "wrong-token", "exists")).isFalse();
        assertThat(service.acceptWebhook("channel-1", "secret-token", "forged-state")).isFalse();

        verifyNoInteractions(provider, recommendations);
    }

    @Test
    void revalidatesEveryGoogleCalendarAfterStartup() {
        var first = mock(OAuthIdentity.class);
        var second = mock(OAuthIdentity.class);
        when(first.getCustomerId()).thenReturn("cust-1");
        when(first.getProviderSubject()).thenReturn("google-1");
        when(second.getCustomerId()).thenReturn("cust-2");
        when(second.getProviderSubject()).thenReturn("google-2");
        when(identities.findAllByProvider("google")).thenReturn(List.of(first, second));
        when(clock.today()).thenReturn(LocalDate.of(2026, 10, 28));
        when(provider.events(anyString(), any(), any())).thenReturn(List.of());

        service.revalidateAfterStartup();

        verify(provider).events("google-1", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 1, 1));
        verify(provider).events("google-2", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 1, 1));
    }
}
