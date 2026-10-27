package com.woori.wonnie.finance;

import com.woori.wonnie.assistant.AssistantService;
import com.woori.wonnie.assistant.AssistantMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FinanceFeatureTests {
    @Autowired FinanceService finance;
    @Autowired FinanceAlertScheduler alerts;
    @Autowired AssistantService assistant;
    @Autowired AssistantMessageRepository messages;
    @Autowired MockMvc http;

    @Test void localHealthIsUpWithoutRabbitBroker() throws Exception {
        http.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test void everyNonCalendarReportIsAvailableAndLabeledDemo() throws Exception {
        for(var topic:List.of("subscriptions","payment-readiness","price-changes","overlaps","renewals",
                "cancellations","usage","card-recommendations","customer-context","card-schedule","alerts")) {
            http.perform(get("/api/finance/"+topic)).andExpect(status().isOk()).andExpect(jsonPath("$.dataSource").value("DEMO"));
        }
        http.perform(get("/api/finance/calendar")).andExpect(status().isBadRequest());
    }
    @Test void excludesCancelledSubscriptionsAndKeepsTrialProjectionSeparate() {
        var data=finance.report("subscriptions",null).data();
        assertThat((BigDecimal)data.get("projectedMonthlyCostAfterTrials")).isEqualByComparingTo("98200");
        assertThat((BigDecimal)data.get("confirmedMonthlyCost")).isEqualByComparingTo("86300");
        assertThat(data.toString()).doesNotContain("sub-cancelled");
        assertThat(rows(data.get("recurringChargeCandidates"))).hasSize(3).allSatisfy(r->assertThat(r.get("classification")).isEqualTo("RECURRING_CANDIDATE"));
    }
    @Test void dashboardUsesMonthlyEquivalentForAnnualMembership() throws Exception {
        http.perform(get("/api/dashboard")).andExpect(status().isOk()).andExpect(jsonPath("$.totalMonthlyCost").value(98200));
    }
    @Test void separatesExchangeRateChangeFromOriginalCurrencyPriceIncrease() {
        var rows=rows(finance.report("price-changes",null).data().get("transactionComparisons"));
        assertThat(rows).anySatisfy(r->{assertThat(r.get("subscription")).isEqualTo("iCloud+");assertThat(r.get("reason")).isEqualTo("FX_CHANGE");});
        assertThat(rows).anySatisfy(r->{assertThat(r.get("subscription")).isEqualTo("Netflix");assertThat(r.get("reason")).isEqualTo("ORIGINAL_AMOUNT_INCREASE");});
    }
    @Test void merchantCannotBeResolvedToOneAppAndRejectionIsNotCancellation() {
        var report=finance.report("merchant","APPLE.COM/BILL");
        assertThat(report.data().get("verifiedSubscription")).isEqualTo(false);
        assertThat(report.summary()).contains("영수증", "확인해야");
        var cancellations=finance.report("cancellations",null).data();
        assertThat((BigDecimal)cancellations.get("monthlyExpectedReleasedBudget")).isEqualByComparingTo("9900");
        assertThat(rows(cancellations.get("subscriptions"))).anySatisfy(r->{
            assertThat(r.get("subscription")).isEqualTo("iCloud+");
            assertThat(r.get("cancellationConfirmed")).isEqualTo(false);
            assertThat((List<?>)r.get("declinedTransactions")).isNotEmpty();
        });
    }
    @Test void recommendationsRespectSpendConditionsAndDoNotPromiseIssuance() {
        var rows=rows(finance.report("card-recommendations",null).data().get("cards"));
        assertThat(rows).anySatisfy(r->{assertThat(r.get("cardId")).isEqualTo("card-woori");
            assertThat(r.get("minimumSpendMet")).isEqualTo(false);
            assertThat((BigDecimal)r.get("incrementalDiscountAvailableOnHeldCard")).isZero();});
        assertThat(rows).allSatisfy(r->assertThat(r.get("issuanceEligibility")).isEqualTo("UNKNOWN_REQUIRES_ISSUER_SCREENING"));
    }
    @Test void absentUsageStaysUnknownAndCohortIsExplicitlySyntheticWithoutRawIncome() {
        assertThat(rows(finance.report("usage",null).data().get("usage"))).anySatisfy(r->{
            assertThat(r.get("subscription")).isEqualTo("YouTube Premium");assertThat(r.get("assessment")).isEqualTo("UNKNOWN");});
        var context=finance.report("customer-context",null).data();
        assertThat(context).doesNotContainKeys("monthlyIncome","birthDate","gender");
        assertThat(context.get("peerComparisonAvailable")).isEqualTo(true);
        assertThat(context.get("peerComparison").toString()).contains("가상 집계 데이터");
    }
    @Test void proactiveNoticesArePersistedOnceAndMarkedReadPerConversation() {
        var candidates=alerts.scan();assertThat(candidates).isPositive();
        var first=assistant.conversation(FinanceService.CUSTOMER);
        assertThat(first.unreadCount()).isEqualTo(candidates);
        alerts.scan();assertThat(messages.count()).isEqualTo(candidates);
        assistant.markRead(FinanceService.CUSTOMER);
        assertThat(assistant.unreadCount(FinanceService.CUSTOMER)).isZero();
    }
    @Test void annualMembershipHasMonthlyEquivalentAndPayPerUseBreakEven() {
        var usage=rows(finance.report("usage",null).data().get("usage"));
        assertThat(usage).anySatisfy(r->{assertThat(r.get("subscription")).isEqualTo("데모 피트니스 연간권");
            assertThat((BigDecimal)r.get("breakEvenVisits")).isEqualByComparingTo("5");
            assertThat((BigDecimal)r.get("estimatedPayPerUseCostForRecordedVisits")).isEqualByComparingTo("30000");});
    }
    @Test void plainChatUsesFinanceDataAndPersistsBothSides() throws Exception {
        http.perform(post("/api/assistant/messages").contentType("application/json").content("{\"message\":\"구독료가 오른 내역 알려줘\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.assistantMessage.content").value(org.hamcrest.Matchers.containsString("Netflix")));
        assertThat(messages.count()).isEqualTo(2);
    }
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> rows(Object value){return (List<Map<String,Object>>)value;}
}
