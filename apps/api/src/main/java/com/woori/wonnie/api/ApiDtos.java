package com.woori.wonnie.api;

import com.woori.wonnie.subscription.SubscriptionStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
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
            String initial
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
            String nextPaymentName,
            LocalDate nextPaymentDate,
            List<SubscriptionSummary> subscriptions,
            List<CalendarSignalResponse> calendarSignals
    ) {
    }

    public record AssistantRequest(
            @NotBlank @Size(max = 4000) String message,
            @Size(max = 200) String topic
    ) {
    }

    public record SubscriptionPriceObservationRequest(
            @NotBlank @Size(max = 100) String eventId,
            @NotBlank String subscriptionId,
            @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal amount,
            @NotNull LocalDateTime observedAt,
            @jakarta.validation.constraints.Pattern(regexp = "[A-Z]{3}") String currency,
            @Positive @Digits(integer = 13, fraction = 2) BigDecimal originalAmount,
            @Positive @Digits(integer = 8, fraction = 6) BigDecimal exchangeRate
    ) {
    }

    public record SubscriptionPriceObservationAcceptedResponse(String eventId, String status) {
    }

    public record AssistantMessageResponse(
            String id,
            String role,
            String source,
            String content,
            LocalDateTime createdAt
    ) {
    }

    public record AssistantConversationResponse(
            String conversationId,
            List<AssistantMessageResponse> messages,
            long unreadCount
    ) {
    }

    public record AssistantMessageExchange(
            AssistantMessageResponse userMessage,
            AssistantMessageResponse assistantMessage
    ) {
    }

    public record UnreadCountResponse(long unreadCount) {
    }

    public record ApiError(String code, String message) {
    }
}
