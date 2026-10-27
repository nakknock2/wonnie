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
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;

    @Test void localHealthIsUpWithoutRabbitBroker() throws Exception {
        http.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test void everyNonCalendarReportIsAvailableWithDatabaseSource() throws Exception {
        for(var topic:List.of("subscriptions","payment-readiness","price-changes","overlaps","renewals",
                "cancellations","usage","card-recommendations","customer-context","card-schedule","alerts")) {
            http.perform(get("/api/finance/"+topic)).andExpect(status().isOk()).andExpect(jsonPath("$.dataSource").value("DATABASE"));
        }
        http.perform(get("/api/finance/calendar")).andExpect(status().isBadRequest());
    }
    @Test void excludesCancelledSubscriptionsAndKeepsTrialProjectionSeparate() {
        var data=finance.report("subscriptions",null).data();
        assertThat((BigDecimal)data.get("projectedMonthlyCostAfterTrials")).isEqualByComparingTo("69190");
        assertThat((BigDecimal)data.get("confirmedMonthlyCost")).isEqualByComparingTo("61300");
        assertThat(data.toString()).doesNotContain("sub-cancelled");
        assertThat(rows(data.get("recurringChargeCandidates"))).hasSizeGreaterThanOrEqualTo(7).allSatisfy(r->assertThat(r.get("classification")).isEqualTo("RECURRING_CANDIDATE"));
    }
    @Test void dashboardUsesActiveSubscriptionsWithoutTrialProjection() throws Exception {
        http.perform(get("/api/dashboard")).andExpect(status().isOk()).andExpect(jsonPath("$.totalMonthlyCost").value(61300));
    }
    @Test void separatesExchangeRateChangeFromOriginalCurrencyPriceIncrease() {
        transaction("verify-fx-old","sub-apple","card-5678","mch-apple",4000,"USD","2.99","2026-10-25 03:00:00","APPROVED");
        transaction("verify-fx-new","sub-apple","card-5678","mch-apple",4400,"USD","2.99","2026-10-26 03:00:00","APPROVED");
        transaction("verify-netflix-rise","sub-netflix","card-5678","mch-netflix",18000,"KRW","18000","2026-10-26 03:00:00","APPROVED");
        var rows=rows(finance.report("price-changes",null).data().get("transactionComparisons"));
        assertThat(rows).anySatisfy(r->{assertThat(r.get("subscription")).isEqualTo("Apple 구독 (어떤 앱인지 확인 필요)");assertThat(r.get("reason")).isEqualTo("FX_CHANGE");});
        assertThat(rows).anySatisfy(r->{assertThat(r.get("subscription")).isEqualTo("넷플릭스");assertThat(r.get("reason")).isEqualTo("ORIGINAL_AMOUNT_INCREASE");});
    }
    @Test void retainsHistoricalSeedPriceIncreaseWhenLatestTwoChargesAreEqual() {
        assertThat(rows(finance.report("price-changes",null).data().get("transactionComparisons")))
            .anySatisfy(r->{
                assertThat(r.get("subscription")).isEqualTo("티빙");
                assertThat(r.get("reason")).isEqualTo("ORIGINAL_AMOUNT_INCREASE");
                assertThat((BigDecimal)r.get("previousKrw")).isEqualByComparingTo("4900");
                assertThat((BigDecimal)r.get("currentKrw")).isEqualByComparingTo("5500");
                assertThat(r.get("currentDate")).isEqualTo(java.time.LocalDate.of(2026,9,11));
            });
    }
    @Test void merchantCannotBeResolvedToOneAppAndRejectionIsNotCancellation() {
        transaction("verify-declined","sub-apple","card-5678","mch-apple",2200,"KRW","2200","2026-10-26 04:00:00","DECLINED");
        var report=finance.report("merchant","APPLE.COM/BILL");
        assertThat(report.data().get("verifiedSubscription")).isEqualTo(false);
        assertThat(report.summary()).contains("영수증", "확인");
        var cancellations=finance.report("cancellations",null).data();
        assertThat((BigDecimal)cancellations.get("monthlyExpectedReleasedBudget")).isEqualByComparingTo("0");
        assertThat(rows(cancellations.get("subscriptions"))).anySatisfy(r->{
            assertThat(r.get("subscription")).isEqualTo("Apple 구독 (어떤 앱인지 확인 필요)");
            assertThat(r.get("cancellationConfirmed")).isEqualTo(false);
            assertThat((List<?>)r.get("declinedTransactions")).isNotEmpty();
        });
    }
    @Test void recommendationsRespectSpendConditionsAndDoNotPromiseIssuance() {
        var rows=rows(finance.report("card-recommendations",null).data().get("cards"));
        assertThat(rows).anySatisfy(r->{assertThat(r.get("cardId")).isEqualTo("card-1234");
            assertThat(r.get("minimumSpendMet")).isEqualTo(false);
            assertThat((BigDecimal)r.get("incrementalDiscountAvailableOnHeldCard")).isZero();});
        assertThat(rows).allSatisfy(r->assertThat(r.get("issuanceEligibility")).isEqualTo("UNKNOWN_REQUIRES_ISSUER_SCREENING"));
    }
    @Test void absentUsageStaysUnknownAndCohortIsExplicitlySyntheticWithoutRawIncome() {
        assertThat(rows(finance.report("usage",null).data().get("usage"))).anySatisfy(r->{
            assertThat(r.get("subscription")).isEqualTo("유튜브 프리미엄");assertThat(r.get("assessment")).isEqualTo("UNKNOWN");});
        var context=finance.report("customer-context",null).data();
        assertThat(context).doesNotContainKeys("monthlyIncome","birthDate","gender");
        assertThat(context.get("peerComparisonAvailable")).isEqualTo(true);
        assertThat(context.get("peerComparison").toString()).contains("가상 집계 데이터");
    }
    @Test void proactiveNoticesArePersistedOnceAndMarkedReadPerConversation() {
        var candidates=alerts.scan();assertThat(candidates).isPositive();
        var first=assistant.conversation(FinanceService.CUSTOMER);
        assertThat(first.unreadCount()).isEqualTo(candidates);
        assertThat(first.messages()).allSatisfy(message -> assertThat(message.content()).doesNotStartWith("[샘플 데이터]"));
        alerts.scan();assertThat(messages.count()).isEqualTo(candidates);
        assistant.markRead(FinanceService.CUSTOMER);
        assertThat(assistant.unreadCount(FinanceService.CUSTOMER)).isZero();
    }
    @Test void annualMembershipHasMonthlyEquivalentAndPayPerUseBreakEven() {
        entityManager.createNativeQuery("INSERT INTO subscription (id,customer_id,card_id,merchant_id,service_id,display_name,category,billing_cycle,amount,charge_day,next_charge_on,status,detection_confidence) VALUES ('sub-gym','cust-kimwoori','card-1234','mch-watcha','svc-watcha','데모 피트니스 연간권','FITNESS','YEARLY',600000,1,'2026-11-01','ACTIVE',1)").executeUpdate();
        entityManager.createNativeQuery("INSERT INTO subscription_context (id,annual_renewal_on,usage_count30_days,pay_per_use_amount) VALUES ('sub-gym','2026-11-01',3,10000)").executeUpdate();
        var usage=rows(finance.report("usage",null).data().get("usage"));
        assertThat(usage).anySatisfy(r->{assertThat(r.get("subscription")).isEqualTo("데모 피트니스 연간권");
            assertThat((BigDecimal)r.get("breakEvenVisits")).isEqualByComparingTo("5");
            assertThat((BigDecimal)r.get("estimatedPayPerUseCostForRecordedVisits")).isEqualByComparingTo("30000");});
    }
    @Test void plainChatUsesFinanceDataAndPersistsBothSides() throws Exception {
        http.perform(post("/api/assistant/messages").contentType("application/json").content("{\"message\":\"구독료가 오른 내역 알려줘\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.assistantMessage.content").value(org.hamcrest.Matchers.containsString("샘플")));
        assertThat(messages.count()).isEqualTo(2);
    }
    private void transaction(String id,String subscription,String card,String merchant,int amount,String currency,String original,String at,String status) {
        entityManager.createNativeQuery("INSERT INTO card_transaction (id,card_id,customer_id,approved_at,merchant_id,merchant_name_raw,category,amount,currency,original_amount,exchange_rate,status,subscription_id) VALUES (:id,:card,'cust-kimwoori',:at,:merchant,:merchant,'SUBSCRIPTION',:amount,:currency,:original,1471.57,:status,:sub)")
            .setParameter("id",id).setParameter("card",card).setParameter("at",java.time.LocalDateTime.parse(at.replace(' ','T')))
            .setParameter("merchant",merchant).setParameter("amount",amount).setParameter("currency",currency)
            .setParameter("original",new BigDecimal(original)).setParameter("status",status).setParameter("sub",subscription).executeUpdate();
    }
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> rows(Object value){return (List<Map<String,Object>>)value;}
}
