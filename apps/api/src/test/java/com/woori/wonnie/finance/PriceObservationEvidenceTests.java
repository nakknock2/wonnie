package com.woori.wonnie.finance;

import com.woori.wonnie.assistant.*;
import com.woori.wonnie.subscription.*;
import com.woori.wonnie.consent.ConsentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PriceObservationEvidenceTests {
    @Autowired SubscriptionPriceObservationService processor;
    @Autowired SubscriptionPriceObservationRepository observations;
    @Autowired AssistantService assistant;
    @Autowired ObjectMapper json;
    @Autowired BusinessClock clock;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired ConsentService consents;
    @Test void foreignCurrencyEvidenceSeparatesFxFromPriceIncreaseAndHandlesReplayAndStaleData() throws Exception {
        consents.updateNotifications(FinanceService.CUSTOMER, false, true);
        var now=clock.nowUtc().plusSeconds(1);
        var first=event("fx-first","2200","2.99",now);
        processor.process(json.readValue(json.writeValueAsString(first),SubscriptionPriceObservationEvent.class));
        assertThat(observations.findAllBySubscriptionIdIn(List.of("sub-apple")))
            .as("clock=%s baseline=%s", now, subscriptions.findById("sub-apple").orElseThrow().getUpdatedAt())
            .allSatisfy(o -> assertThat(o.isApplied()).isTrue());
        processor.process(event("fx-next","4600","2.99",now.plusSeconds(1)));
        var afterFx=assistant.conversation(FinanceService.CUSTOMER);
        assertThat(afterFx.messages()).hasSize(1);
        assertThat(afterFx.messages().get(0).source()).isEqualTo("FINANCE_NOTICE");
        assertThat(afterFx.messages().get(0).content()).contains("환율·수수료");
        var raised=event("fx-price","6000","3.99",now.plusSeconds(2));
        processor.process(raised);processor.process(raised);
        processor.process(event("fx-old","9999","9.99",now));
        var history=observations.findAllBySubscriptionIdIn(List.of("sub-apple"));
        assertThat(history).hasSize(4).filteredOn(SubscriptionPriceObservation::isPriceIncreased).hasSize(1);
        assertThat(history).anySatisfy(o->{assertThat(o.getEventId()).isEqualTo("fx-old");assertThat(o.isApplied()).isFalse();});
        var messages=assistant.conversation(FinanceService.CUSTOMER).messages();
        assertThat(messages).hasSize(2).anySatisfy(m->{assertThat(m.source()).isEqualTo("PRICE_INCREASE");assertThat(m.content()).contains("3.99 USD");});
    }
    @Test void legacyKrwOnlyObservationDoesNotClaimVerifiedServicePriceIncrease() {
        consents.updateNotifications(FinanceService.CUSTOMER, false, true);
        processor.process(new SubscriptionPriceObservationEvent("legacy",FinanceService.CUSTOMER,"sub-netflix",new BigDecimal("18000"),
                clock.nowUtc().plusSeconds(1),null,null,null));
        var message=assistant.conversation(FinanceService.CUSTOMER).messages().get(0);
        assertThat(message.source()).isEqualTo("FINANCE_NOTICE");
        assertThat(message.content()).contains("아직 확인되지 않았어요");
    }
    @Test void updatesObservedPriceWithoutNotifyingWhenPriceAlertsAreOff() {
        processor.process(new SubscriptionPriceObservationEvent("no-notice",FinanceService.CUSTOMER,"sub-netflix",new BigDecimal("18000"),
                clock.nowUtc().plusSeconds(1),"KRW",new BigDecimal("18000"),BigDecimal.ONE));
        assertThat(subscriptions.findById("sub-netflix").orElseThrow().getAmount()).isEqualByComparingTo("18000");
        assertThat(assistant.conversation(FinanceService.CUSTOMER).messages()).isEmpty();
    }
    private SubscriptionPriceObservationEvent event(String id,String amount,String original,LocalDateTime time) {
        return new SubscriptionPriceObservationEvent(id,FinanceService.CUSTOMER,"sub-apple",new BigDecimal(amount),time,
                "USD",new BigDecimal(original),new BigDecimal(amount).divide(new BigDecimal(original),6,java.math.RoundingMode.HALF_UP));
    }
}
