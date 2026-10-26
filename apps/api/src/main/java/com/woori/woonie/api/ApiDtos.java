package com.woori.woonie.api;

import com.woori.woonie.subscription.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record WalletSummary(
            BigDecimal balance,
            boolean autoFundingEnabled,
            String walletStatus,
            String virtualCardLast4,
            String virtualCardStatus
    ) {
    }

    public record SubscriptionSummary(
            String id,
            String name,
            String category,
            BigDecimal amount,
            String billingCycle,
            LocalDate nextPaymentDate,
            SubscriptionStatus status,
            String brandColor,
            String initial,
            WalletSummary wallet
    ) {
    }

    public record CalendarSignalResponse(
            String id,
            String title,
            LocalDate startsOn,
            LocalDate endsOn,
            String signalType,
            BigDecimal confidence
    ) {
    }

    public record DashboardResponse(
            String customerName,
            BigDecimal totalMonthlyCost,
            long activeCount,
            long pausedCount,
            String nextPaymentName,
            LocalDate nextPaymentDate,
            List<SubscriptionSummary> subscriptions,
            List<CalendarSignalResponse> calendarSignals
    ) {
    }

    public record AssistantRequest(String message) {
    }

    public record ActionPlanResponse(
            String id,
            String subscriptionId,
            String subscriptionName,
            String actionType,
            LocalDate startsOn,
            LocalDate endsOn,
            String reason,
            String warning,
            String status
    ) {
    }

    public record AssistantResponse(
            String reply,
            ActionPlanResponse actionPlan,
            List<String> highlights
    ) {
    }

    public record AuthChallengeResponse(
            String challengeId,
            String challenge,
            LocalDateTime expiresAt
    ) {
    }

    public record MockFaceIdRequest(
            String challengeId,
            String challenge
    ) {
    }

    public record SagaResult(
            String sagaId,
            String status,
            String message,
            SubscriptionStatus subscriptionStatus
    ) {
    }

    public record ApiError(String code, String message) {
    }
}
