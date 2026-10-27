package com.woori.wonnie.subscription;

import com.woori.wonnie.assistant.AssistantService;
import com.woori.wonnie.consent.ConsentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class SubscriptionPriceObservationService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPriceObservationRepository observationRepository;
    private final AssistantService assistantService;
    private final com.woori.wonnie.finance.BusinessClock clock;
    private final ConsentService consents;

    public SubscriptionPriceObservationService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionPriceObservationRepository observationRepository,
            AssistantService assistantService,
            com.woori.wonnie.finance.BusinessClock clock,
            ConsentService consents
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.observationRepository = observationRepository;
        this.assistantService = assistantService;
        this.clock = clock;
        this.consents = consents;
    }

    @Transactional
    public void process(SubscriptionPriceObservationEvent event) {
        if (event.observedAt().isAfter(clock.nowUtc().plusMinutes(5))) {
            throw new IllegalArgumentException("관측 시각은 현재 시각보다 미래일 수 없습니다.");
        }
        if (observationRepository.existsByEventId(event.eventId())) return;

        var subscription = subscriptionRepository.findByIdForUpdate(event.subscriptionId())
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));
        if (observationRepository.existsByEventId(event.eventId())) return;
        if (!subscription.getCustomerId().equals(event.customerId())) {
            throw new IllegalArgumentException("구독 소유자가 일치하지 않습니다.");
        }

        var latest = observationRepository
                .findFirstBySubscriptionIdAndAppliedTrueOrderByObservedAtDesc(subscription.getId())
                .orElse(null);
        var previousAmount = latest == null ? subscription.getAmount() : latest.getObservedAmount();
        var baselineTimestamp = latest == null ? subscription.getUpdatedAt() : latest.getObservedAt();
        var isStale = !event.observedAt().isAfter(baselineTimestamp);
        var originalBefore=latest!=null && event.currency()!=null && event.currency().equals(latest.getCurrency())
                ? latest.getOriginalAmount() : latest==null && "KRW".equals(event.currency())?previousAmount:null;
        var reason=isStale?"STALE":originalBefore==null?"AMOUNT_CHANGE_UNVERIFIED":
                event.originalAmount().compareTo(originalBefore)>0?"ORIGINAL_AMOUNT_INCREASE":
                event.originalAmount().compareTo(originalBefore)==0 && event.amount().compareTo(previousAmount)>0?"FX_OR_FEE_CHANGE":"NO_ORIGINAL_INCREASE";
        var increased=!isStale && "ORIGINAL_AMOUNT_INCREASE".equals(reason);
        var observation=SubscriptionPriceObservation.of(event.eventId(),subscription.getId(),previousAmount,event.amount(),event.observedAt(),!isStale,increased);
        observation.currencyEvidence(event.currency(),event.originalAmount(),event.exchangeRate(),reason);
        observationRepository.save(observation);
        if(isStale)return;
        subscription.updateAmount(event.amount());
        if(increased && consents.isAgreed(event.customerId(), ConsentService.PUSH_PRICE_CHANGE)) {
            var content=subscription.getName()+" 원통화 구독료가 "+originalBefore.toPlainString()+" "+event.currency()+"에서 "
                    +event.originalAmount().toPlainString()+" "+event.currency()+"로 올랐어요. 원화 결제 금액은 "+previousAmount.toPlainString()+"원 → "
                    +event.amount().toPlainString()+"원이에요. 요금제·프로모션 변경 여부도 확인해보세요.";
            assistantService.addPriceIncreaseNotice(event.customerId(),content,"price-increase:"+event.eventId());
        } else if(event.amount().compareTo(previousAmount)>0
                && consents.isAgreed(event.customerId(), ConsentService.PUSH_PRICE_CHANGE)) {
            var content=subscription.getName()+" 원화 결제 금액이 "+previousAmount.toPlainString()+"원에서 "+event.amount().toPlainString()+"원으로 증가했어요. "
                    +("FX_OR_FEE_CHANGE".equals(reason)?"원통화 금액은 같아 환율·수수료 변화 가능성이 있어요.":"원통화 요금 자료가 없어 실제 구독료 인상인지는 아직 확인되지 않았어요.");
            assistantService.addFinanceNotice(event.customerId(),content,"amount-change:"+event.eventId());
        }
    }
}
