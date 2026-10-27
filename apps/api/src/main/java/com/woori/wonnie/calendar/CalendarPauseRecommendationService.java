package com.woori.wonnie.calendar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woori.wonnie.assistant.AssistantService;
import com.woori.wonnie.subscription.Subscription;
import com.woori.wonnie.subscription.SubscriptionRepository;
import com.woori.wonnie.subscription.SubscriptionStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CalendarPauseRecommendationService {
    private final SubscriptionRepository subscriptions;
    private final AssistantService assistant;
    private final ObjectMapper json;

    public CalendarPauseRecommendationService(
            SubscriptionRepository subscriptions, AssistantService assistant, ObjectMapper json
    ) {
        this.subscriptions = subscriptions;
        this.assistant = assistant;
        this.json = json;
    }

    public int evaluate(String customerId, YearMonth month, List<CalendarEventSnapshot> events) {
        var windows = events.stream().map(this::classify).filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(Window::startsOn)).toList();
        if (windows.isEmpty()) return 0;
        var candidates = subscriptions.findAllByCustomerIdOrderByNextPaymentDateAsc(customerId).stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(this::isPauseCandidate)
                .limit(5)
                .toList();
        if (candidates.isEmpty()) return 0;

        var primary = windows.get(0);
        var recommendations = candidates.stream().map(s -> Map.<String, Object>of(
                "id", s.getId(),
                "name", s.getName(),
                "price", s.getAmount(),
                "billingCycle", s.getBillingCycle(),
                "nextPaymentDate", s.getNextPaymentDate(),
                "color", s.getBrandColor(),
                "letter", s.getInitialText(),
                "reason", primary.reason(),
                "periodStart", primary.startsOn(),
                "periodEnd", primary.endsOn()
        )).toList();
        var payload = Map.of(
                "month", month.toString(),
                "signalType", primary.type(),
                "signalLabel", primary.label(),
                "periodStart", primary.startsOn(),
                "periodEnd", primary.endsOn(),
                "recommendations", recommendations);
        try {
            var metadata = json.writeValueAsString(payload);
            var signature = hash(windows + "|" + candidates.stream().map(Subscription::getId).sorted().toList());
            var sourceKey = "calendar-pause:" + customerId + ":" + month + ":" + signature;
            var content = "이번 달 캘린더에서 " + primary.label() + "을 확인했어요. 잠시 중단을 고려할 구독 "
                    + candidates.size() + "개를 정리했어요.";
            assistant.addCalendarPauseNotice(customerId, content, metadata, sourceKey);
            return candidates.size();
        } catch (Exception error) {
            throw new IllegalStateException("캘린더 추천 알림을 만들지 못했어요.", error);
        }
    }

    private Window classify(CalendarEventSnapshot event) {
        var title = event.title().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if (title.matches(".*(중간고사|기말고사|시험기간|시험|수능|자격증|exam|midterm|final).*")) {
            return new Window("EXAM", "시험 기간", "시험 기간에는 이용이 줄 수 있어 잠시 중단을 고려해볼 수 있어요.",
                    event.startsOn(), event.endsOn());
        }
        if (title.matches(".*(디지털디톡스|디톡스|스크린프리|휴대폰금지|집중기간|집중주간|digitaldetox|screenfree).*")) {
            return new Window("DIGITAL_DETOX", "디지털 디톡스 기간", "디지털 디톡스 기간과 겹쳐 잠시 중단을 고려해볼 수 있어요.",
                    event.startsOn(), event.endsOn());
        }
        return null;
    }

    private boolean isPauseCandidate(Subscription subscription) {
        var category = subscription.getCategory() == null ? "" : subscription.getCategory().toUpperCase(Locale.ROOT);
        return category.equals("OTT") || category.equals("MUSIC") || category.equals("GAME")
                || subscription.getName().toLowerCase(Locale.ROOT).matches(".*(넷플릭스|왓챠|티빙|유튜브|spotify|스포티파이).*" );
    }

    private static String hash(String value) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest, 0, 8);
    }

    private record Window(String type, String label, String reason,
                          java.time.LocalDate startsOn, java.time.LocalDate endsOn) {}
}
