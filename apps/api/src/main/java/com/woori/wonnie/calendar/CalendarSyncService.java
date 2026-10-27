package com.woori.wonnie.calendar;

import com.woori.wonnie.auth.OAuthIdentityRepository;
import com.woori.wonnie.finance.BusinessClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class CalendarSyncService {
    private static final Logger log = LoggerFactory.getLogger(CalendarSyncService.class);
    private final CalendarProvider provider;
    private final CalendarSyncStateRepository states;
    private final OAuthIdentityRepository identities;
    private final CalendarPauseRecommendationService recommendations;
    private final BusinessClock clock;
    private final Executor calendarSyncExecutor;

    public CalendarSyncService(CalendarProvider provider, CalendarSyncStateRepository states,
            OAuthIdentityRepository identities, CalendarPauseRecommendationService recommendations,
            BusinessClock clock, @Qualifier("calendarSyncExecutor") Executor calendarSyncExecutor) {
        this.provider = provider;
        this.states = states;
        this.identities = identities;
        this.recommendations = recommendations;
        this.clock = clock;
        this.calendarSyncExecutor = calendarSyncExecutor;
    }

    public void schedule(String customerId, String principalName) {
        calendarSyncExecutor.execute(() -> safeSync(customerId, principalName));
    }

    public boolean acceptWebhook(String channelId, String channelToken, String resourceState) {
        if (channelId == null || channelToken == null) return false;
        var state = states.findByChannelId(channelId).orElse(null);
        if (state == null || !channelToken.equals(state.getChannelToken())) return false;
        if (!"sync".equals(resourceState) && !"exists".equals(resourceState) && !"not_exists".equals(resourceState)) return false;
        schedule(state.getCustomerId(), state.getPrincipalName());
        return true;
    }

    @Scheduled(cron = "${app.calendar.revalidation-cron:0 15 3 * * *}", zone = "Asia/Seoul")
    public void dailyRevalidation() {
        identities.findAllByProvider("google")
                .forEach(identity -> schedule(identity.getCustomerId(), identity.getProviderSubject()));
    }

    public void sync(String customerId, String principalName) {
        var month = YearMonth.from(clock.today());
        // 월말에 등록한 다음 달 시험도 즉시 감지할 수 있도록 현재 달부터 향후 3개월을 확인한다.
        var events = provider.events(principalName, month.atDay(1), month.plusMonths(3).atDay(1));
        recommendations.evaluate(customerId, month, events);
        var state = states.findById(customerId).orElseGet(() -> CalendarSyncState.create(customerId, principalName));
        state.synced();
        if (state.watchNeedsRenewal(LocalDateTime.now(ZoneOffset.UTC))) {
            var channelId = UUID.randomUUID().toString();
            var channelToken = UUID.randomUUID().toString();
            var requestedExpiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(7);
            var watch = provider.watch(principalName, channelId, channelToken, requestedExpiry);
            if (watch != null) state.watched(channelId, channelToken, watch.resourceId(), watch.expiresAt());
        }
        states.save(state);
    }

    private void safeSync(String customerId, String principalName) {
        try {
            sync(customerId, principalName);
        } catch (RuntimeException error) {
            log.warn("Google Calendar sync failed. customerId={}", customerId, error);
        }
    }
}
