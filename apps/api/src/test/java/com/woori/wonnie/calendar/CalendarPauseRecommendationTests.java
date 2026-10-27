package com.woori.wonnie.calendar;

import com.woori.wonnie.assistant.AssistantService;
import com.woori.wonnie.dashboard.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wonnie-calendar-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@Transactional
class CalendarPauseRecommendationTests {
    @Autowired CalendarPauseRecommendationService recommendations;
    @Autowired AssistantService assistant;

    @Test
    void createsOneStructuredUnreadNoticeForExamEventsAndDeduplicatesIt() {
        var event = new CalendarEventSnapshot("exam-1", "중간고사 시험기간",
                LocalDate.of(2026, 10, 28), LocalDate.of(2026, 10, 31));

        var count = recommendations.evaluate(DashboardService.DEMO_CUSTOMER_ID,
                YearMonth.of(2026, 10), List.of(event));
        recommendations.evaluate(DashboardService.DEMO_CUSTOMER_ID,
                YearMonth.of(2026, 10), List.of(event));

        var conversation = assistant.conversation(DashboardService.DEMO_CUSTOMER_ID);
        var notices = conversation.messages().stream()
                .filter(message -> "CALENDAR_PAUSE".equals(message.source())).toList();
        assertThat(count).isPositive();
        assertThat(notices).hasSize(1);
        assertThat(notices.get(0).metadata()).contains("시험 기간", "recommendations", "sub-watcha");
        assertThat(conversation.unreadCount()).isEqualTo(1);
    }

    @Test
    void ignoresUnrelatedCalendarEvents() {
        var count = recommendations.evaluate(DashboardService.DEMO_CUSTOMER_ID,
                YearMonth.of(2026, 10), List.of(new CalendarEventSnapshot(
                        "lunch", "친구와 점심", LocalDate.of(2026, 10, 28), LocalDate.of(2026, 10, 28))));
        assertThat(count).isZero();
    }
}
