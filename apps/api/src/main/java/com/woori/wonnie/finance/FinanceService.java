package com.woori.wonnie.finance;

import com.woori.wonnie.dashboard.DashboardService;
import com.woori.wonnie.subscription.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FinanceService {
    public static final String CUSTOMER = DashboardService.DEMO_CUSTOMER_ID;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final SubscriptionRepository subscriptions;
    private final SubscriptionContextRepository contexts;
    private final FinanceCustomerRepository customers;
    private final FinanceCardRepository cards;
    private final FinanceTransactionRepository transactions;
    private final MerchantDirectoryRepository merchants;
    private final FinanceAlternativeRepository alternatives;
    private final SubscriptionPriceObservationRepository observations;
    private final FinanceCohortBenchmarkRepository cohorts;
    public FinanceService(SubscriptionRepository subscriptions,SubscriptionContextRepository contexts,
            FinanceCustomerRepository customers,FinanceCardRepository cards,FinanceTransactionRepository transactions,
            MerchantDirectoryRepository merchants,FinanceAlternativeRepository alternatives,
            SubscriptionPriceObservationRepository observations, FinanceCohortBenchmarkRepository cohorts) {
        this.subscriptions=subscriptions; this.contexts=contexts; this.customers=customers; this.cards=cards;
        this.transactions=transactions; this.merchants=merchants; this.alternatives=alternatives; this.observations=observations;this.cohorts=cohorts;
    }
    public record Report(String dataSource, LocalDateTime asOf, String summary, Map<String,Object> data) {}
    public record Notice(String key,String content) {}
    public Report report(String topic,String merchant) {
        if (!customers.existsById(CUSTOMER)) return wrap("금융 데이터가 연결되지 않았어요.", map("available",false));
        return switch(topic) {
            case "subscriptions" -> subscriptionReport();
            case "merchant" -> merchantReport(merchant);
            case "payment-readiness" -> paymentReadiness();
            case "price-changes" -> priceChanges();
            case "overlaps" -> overlaps();
            case "renewals" -> renewals();
            case "cancellations" -> cancellations();
            case "usage" -> usage();
            case "card-recommendations" -> recommendations();
            case "customer-context" -> customerContext();
            case "card-schedule" -> cardSchedule();
            case "alerts" -> wrap("데이터로 확인한 안내 후보입니다.",map("notices",notices()));
            default -> throw new IllegalArgumentException("지원하지 않는 금융 조회입니다.");
        };
    }
    private List<Subscription> subs() { return subscriptions.findAllByCustomerIdOrderByNextPaymentDateAsc(CUSTOMER); }
    private Map<String,SubscriptionContext> ctx() {
        var ids=subs().stream().map(Subscription::getId).collect(Collectors.toSet());
        return contexts.findAll().stream().filter(c->ids.contains(c.getId()))
            .collect(Collectors.toMap(SubscriptionContext::getId,Function.identity()));
    }
    private List<FinanceCard> customerCards() { return cards.findAllByCustomerId(CUSTOMER); }
    private List<FinanceTransaction> txs() { return transactions.findAllByCustomerId(CUSTOMER); }
    private FinanceCustomer customer() { return customers.findById(CUSTOMER).orElseThrow(); }
    private static boolean active(Subscription s) { return s.getStatus()==SubscriptionStatus.ACTIVE; }
    private static boolean approved(FinanceTransaction t) { return "APPROVED".equals(t.getTransactionStatus()); }
    private Report subscriptionReport() {
        var context=ctx();
        var current=subs().stream().filter(FinanceService::active).toList();
        var total=current.stream().map(this::monthly).reduce(BigDecimal.ZERO,BigDecimal::add);
        var confirmed=current.stream().filter(s -> context.get(s.getId())==null || !"TRIAL".equals(context.get(s.getId()).getContractStatus()))
                .map(this::monthly).reduce(BigDecimal.ZERO,BigDecimal::add);
        var rows=current.stream().map(s -> map("id",s.getId(),"name",s.getName(),"amount",s.getAmount(),
            "monthlyEquivalent",monthly(s),"billingCycle",s.getBillingCycle(),"nextPaymentDate",s.getNextPaymentDate(),
            "contractEvidence",context.get(s.getId()))).toList();
        var monthStart=today().withDayOfMonth(1).atStartOfDay();
        var recent=txs().stream().filter(FinanceService::approved).filter(t -> !t.getAuthorizedAt().isBefore(monthStart)).toList();
        var approvedAmount=recent.stream().map(FinanceTransaction::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        return wrap("등록된 활성 구독은 "+current.size()+"개이며, 무료체험 전환 예정분을 포함한 월 환산액은 "+won(total)+"원이에요.",
            map("subscriptions",rows,"recurringChargeCandidates",recurringCandidates(),"confirmedMonthlyCost",confirmed,"projectedMonthlyCostAfterTrials",total,
                "currentMonthApprovedCardAmount",approvedAmount,"periodStart",monthStart.toLocalDate(),
                "subscriptionShareOfCardSpend",ratio(confirmed,approvedAmount),"livingBills",recent.stream().filter(t -> "LIVING".equals(t.getCategory())).map(FinanceService::transactionView).toList(),
                "totalFixedCostEstimate",total.add(recent.stream().filter(t -> "LIVING".equals(t.getCategory())).map(FinanceTransaction::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add)),
                "note","카드 승인 내역은 샘플로 일부만 제공됩니다. 청구 확정액·전체 소비로 간주하지 마세요."));
    }
    private List<Map<String,Object>> recurringCandidates() {
        var grouped=txs().stream().filter(FinanceService::approved)
            .collect(Collectors.groupingBy(t->t.getCardId()+"|"+t.getMerchantLabel()));
        var result=new ArrayList<Map<String,Object>>();
        for(var group:grouped.values()) {
            var latest=group.stream().sorted(Comparator.comparing(FinanceTransaction::getAuthorizedAt).reversed()).limit(2).toList();
            if(latest.size()<2)continue;
            var days=ChronoUnit.DAYS.between(latest.get(1).getAuthorizedAt().toLocalDate(),latest.get(0).getAuthorizedAt().toLocalDate());
            if(days>=20 && days<=40) result.add(map("merchantLabel",latest.get(0).getMerchantLabel(),"intervalDays",days,
                "latestAmount",latest.get(0).getAmount(),"previousAmount",latest.get(1).getAmount(),"matchedSubscriptionId",latest.get(0).getSubscriptionId(),
                "classification","RECURRING_CANDIDATE","evidence","동일 카드·결제명이 20~40일 간격으로 반복. 계약 확인을 대신하지 않습니다."));
        }
        return result;
    }
    private Report merchantReport(String name) {
        if(name==null || name.isBlank()) throw new IllegalArgumentException("결제명을 입력해 주세요.");
        var query=name.strip().toUpperCase(Locale.ROOT);
        var matches=merchants.findAll().stream().filter(m -> m.getMerchantLabel().toUpperCase(Locale.ROOT).contains(query)
            || query.contains(m.getMerchantLabel().toUpperCase(Locale.ROOT))).toList();
        return wrap(matches.isEmpty()?"결제명 매핑 정보가 없어요. 영수증이나 구입 내역 확인이 필요해요.":
            matches.get(0).getDisplayName()+" 관련 결제명이에요. "+matches.get(0).getVerificationHint(),
            map("query",name,"matches",matches,"verifiedSubscription",false));
    }
    private Report paymentReadiness() {
        var byCard=customerCards().stream().collect(Collectors.toMap(FinanceCard::getId,Function.identity()));
        var context=ctx();var person=customer();
        var rows=subs().stream().filter(FinanceService::active).map(s -> {
            var c=context.get(s.getId());var card=c==null?null:byCard.get(c.getCardId());
            var foreign=txs().stream().filter(FinanceService::approved).filter(t->s.getId().equals(t.getSubscriptionId()) && !"KRW".equals(t.getCurrency()))
                .max(Comparator.comparing(FinanceTransaction::getAuthorizedAt)).orElse(null);
            return map("subscriptionId",s.getId(),"name",s.getName(),"paymentDate",s.getNextPaymentDate(),"amount",s.getAmount(),
                "accountBalance",person.getAccountBalance(),"accountShortfall",s.getAmount().subtract(person.getAccountBalance()).max(BigDecimal.ZERO),
                "card",card==null?null:card.getProductName(),"cardStatus",card==null?"UNKNOWN":card.getCardStatus(),
                "availableLimit",card==null?null:card.getAvailableLimit(),"limitShortfall",card==null?null:s.getAmount().subtract(card.getAvailableLimit()).max(BigDecimal.ZERO),
                "minimumSpendGap",card==null?null:card.getMinimumSpend().subtract(card.getPreviousMonthSpend()).max(BigDecimal.ZERO),
                "pointsUsableEstimate",card==null?null:card.getPointsBalance().min(s.getAmount()),
                "foreignPaymentEstimate",foreign==null?null:map("originalAmount",foreign.getOriginalAmount(),"currency",foreign.getCurrency(),
                    "recordedExchangeRate",foreign.getExchangeRate(),"rateAsOf",foreign.getAuthorizedAt(),
                    "estimatedKrwAtRecordedRate",foreign.getOriginalAmount().multiply(foreign.getExchangeRate()).setScale(0,RoundingMode.HALF_UP),
                    "note","현재 실시간 환율과 해외 이용 수수료는 미연동입니다."),
                "pointsNote","실제 포인트 사용 가능 가맹점·사용 단위·사용 동의는 별도 확인해야 합니다.");
        }).toList();
        return wrap("결제 전에는 카드 상태·이용한도·전월실적을 확인하세요. 잔액은 계좌 자동이체에만 해당하며 신용카드 승인 가능 여부와는 별도예요.",
            map("payments",rows,"heldCardBenefitComparisons",cardEstimates().stream().filter(r -> Boolean.TRUE.equals(r.get("owned"))).toList(),"paymentMethodChanged",false));
    }
    private Report priceChanges() {
        var ids=subs().stream().map(Subscription::getId).toList();
        var history=observations.findAllBySubscriptionIdIn(ids);
        var changes=new ArrayList<Map<String,Object>>();
        for(var s:subs()) {
            var charges=txs().stream().filter(FinanceService::approved).filter(t -> s.getId().equals(t.getSubscriptionId()))
                .sorted(Comparator.comparing(FinanceTransaction::getAuthorizedAt).reversed()).limit(2).toList();
            if(charges.size()<2)continue;
            var newer=charges.get(0);var older=charges.get(1);var delta=newer.getAmount().subtract(older.getAmount());
            var sameCurrency=Objects.equals(newer.getCurrency(),older.getCurrency());
            String reason=!sameCurrency?"CURRENCY_CHANGED_NEEDS_REVIEW":sameCurrency && newer.getOriginalAmount().compareTo(older.getOriginalAmount())==0 && delta.signum()>0?"FX_CHANGE":
                newer.getOriginalAmount().compareTo(older.getOriginalAmount())>0?"ORIGINAL_AMOUNT_INCREASE":"NO_INCREASE";
            changes.add(map("subscription",s.getName(),"previousKrw",older.getAmount(),"currentKrw",newer.getAmount(),"differenceKrw",delta,
                "percentage",ratio(delta,older.getAmount()),"currency",newer.getCurrency(),"previousOriginalAmount",older.getOriginalAmount(),
                "currentOriginalAmount",newer.getOriginalAmount(),"previousExchangeRate",older.getExchangeRate(),"currentExchangeRate",newer.getExchangeRate(),
                "reason",reason,"note","요금제 변경·수수료·프로모션 종료는 영수증 확인 전 확정하지 않습니다."));
        }
        var summary=changes.stream().filter(c -> "ORIGINAL_AMOUNT_INCREASE".equals(c.get("reason")))
            .map(c -> c.get("subscription")+" "+won((BigDecimal)c.get("previousKrw"))+"원 → "+won((BigDecimal)c.get("currentKrw"))+"원")
            .collect(Collectors.joining(", "));
        return wrap(summary.isEmpty()?"확인된 구독료 인상 이력이 없어요.":"샘플 거래에서 "+summary+" 변화가 있어요. 해외 결제는 원화 금액과 원래 통화의 금액을 함께 봐야 해요.",
            map("transactionComparisons",changes,"observedPriceHistory",history));
    }
    private Report overlaps() {
        var context=ctx();var groups=new LinkedHashMap<String,List<String>>();var bundle=new ArrayList<Map<String,Object>>();
        for(var s:subs().stream().filter(FinanceService::active).toList()) {
            var c=context.get(s.getId());if(c==null)continue;
            groups.computeIfAbsent(c.getServiceFamily(),k->new ArrayList<>()).add(s.getName());
            if(c.getBundleDescription()!=null || c.isFamilyCovered())bundle.add(map("subscription",s.getName(),"bundle",c.getBundleDescription(),"familyBenefitPresent",c.isFamilyCovered()));
        }
        var overlapping=groups.entrySet().stream().filter(e->e.getValue().size()>1).map(e->map("category",e.getKey(),"services",e.getValue())).toList();
        return wrap("같은 용도의 구독 "+overlapping.size()+"개 묶음과 통신·가족 혜택 중복 후보 "+bundle.size()+"건이 있어요. 실제 중복 여부는 사용하는 콘텐츠와 이용권 적용 대상을 확인해야 해요.",
            map("categoryOverlapCandidates",overlapping,"bundleOrFamilyCandidates",bundle,"confirmedDuplicates",false));
    }
    private Report renewals() {
        var context=ctx();var list=new ArrayList<Map<String,Object>>();
        for(var s:subs().stream().filter(FinanceService::active).toList()) {
            var c=context.get(s.getId());if(c==null)continue;
            if(c.getTrialEndsOn()!=null) list.add(map("subscription",s.getName(),"kind","TRIAL_END","date",c.getTrialEndsOn(),"plannedAmount",s.getAmount()));
            if(c.getAnnualRenewalOn()!=null && s.getBillingCycle().contains("년"))list.add(map("subscription",s.getName(),"kind","ANNUAL_RENEWAL","date",c.getAnnualRenewalOn(),"plannedAmount",s.getAmount(),"amountPeriod",s.getBillingCycle()));
        }
        return wrap("무료체험 종료·연간 갱신 일정이 "+list.size()+"건 있어요. 저장된 계약 정보를 기준으로 안내하며 서비스에서 최종 조건을 확인해 주세요.",map("events",list,"cardSchedule",cardSchedule().data()));
    }
    private Report cancellations() {
        var context=ctx();var rows=new ArrayList<Map<String,Object>>();var saved=BigDecimal.ZERO;
        for(var s:subs()) {
            var c=context.get(s.getId());if(c==null)continue;
            var declined=txs().stream().filter(t->s.getId().equals(t.getSubscriptionId()) && "DECLINED".equals(t.getTransactionStatus())).toList();
            var confirmed="CANCELLED".equals(c.getContractStatus()) && c.getContractVerifiedOn()!=null && c.getCancelledOn()!=null;
            var laterCharge=confirmed && txs().stream().anyMatch(t->s.getId().equals(t.getSubscriptionId()) && approved(t) && !t.getAuthorizedAt().toLocalDate().isBefore(c.getCancelledOn()));
            if(confirmed && !laterCharge)saved=saved.add(monthly(s));
            rows.add(map("subscription",s.getName(),"contractStatus",c.getContractStatus(),"verifiedOn",c.getContractVerifiedOn(),
                "cancellationConfirmed",confirmed,"chargeAfterCancellationNeedsReview",laterCharge,"declinedTransactions",declined.stream().map(FinanceService::transactionView).toList(),
                "monthlyExpectedReleasedBudget",confirmed && !laterCharge?monthly(s):BigDecimal.ZERO));
        }
        var revolving=customerCards().stream().filter(FinanceCard::isOwned).findFirst().orElse(null);
        var interest=revolving==null?BigDecimal.ZERO:saved.min(revolving.getRevolvingBalance()).multiply(revolving.getRevolvingApr()).multiply(BigDecimal.valueOf(30)).divide(BigDecimal.valueOf(365),0,RoundingMode.HALF_UP);
        return wrap("계약 해지가 확인된 샘플 구독 기준 월 "+won(saved)+"원의 지출 감소를 예상할 수 있어요. 결제 거절만으로 해지라고 판단하지 않아요.",
            map("subscriptions",rows,"monthlyExpectedReleasedBudget",saved,"annualizedEstimate",saved.multiply(BigDecimal.valueOf(12)),
                "interestReductionIfReleasedBudgetRepaysRevolvingFor30Days",interest,"repaymentExecuted",false,"savingsTransferExecuted",false,
                "spendThresholdImpact",customerCards().stream().filter(FinanceCard::isOwned).map(c->map("card",c.getProductName(),
                    "previousMonthSpend",c.getPreviousMonthSpend(),"minimumSpend",c.getMinimumSpend(),"spendAfterHypotheticalReduction",c.getPreviousMonthSpend().subtract(subs().stream().filter(s->s.getStatus()==SubscriptionStatus.CANCELLED && context.containsKey(s.getId()) && c.getId().equals(context.get(s.getId()).getCardId())).map(this::monthly).reduce(BigDecimal.ZERO,BigDecimal::add)).max(BigDecimal.ZERO))).toList()));
    }
    private Report usage() {
        var context=ctx();var rows=new ArrayList<Map<String,Object>>();
        for(var s:subs().stream().filter(FinanceService::active).toList()) {
            var c=context.get(s.getId());var count=c==null?null:c.getUsageCount30Days();
            rows.add(map("subscription",s.getName(),"usageCount30Days",count,"usageEvidence",c==null?"이용 데이터 없음":c.getUsageEvidence(),
                "costPerUse",count==null || count==0?null:monthly(s).divide(BigDecimal.valueOf(count),0,RoundingMode.HALF_UP),
                "payPerUseReference",c==null?null:c.getPayPerUseAmount(),
                "breakEvenVisits",c==null || c.getPayPerUseAmount()==null || c.getPayPerUseAmount().signum()==0?null:monthly(s).divide(c.getPayPerUseAmount(),0,RoundingMode.CEILING),
                "estimatedPayPerUseCostForRecordedVisits",count==null || c==null || c.getPayPerUseAmount()==null?null:c.getPayPerUseAmount().multiply(BigDecimal.valueOf(count)),
                "assessment",count==null?"UNKNOWN":count==0?"NO_RECORDED_USAGE":"REVIEW_COST_PER_USE",
                "note","결제 내역이나 캘린더만으로 실제 시청·시설 이용 횟수를 알 수 없습니다."));
        }
        return wrap("이용 로그가 있는 구독만 이용 횟수와 1회당 비용을 계산했어요. 기록이 없으면 미사용으로 단정하지 않아요.",
            map("usage",rows,"localAlternatives",alternatives.findAll().stream().filter(a->a.getRegion().equals("ALL") || a.getRegion().equals(customer().getRegion())).toList(),
                "breakEvenNote","대체 1회권 가격·동일 서비스 범위가 연결되지 않은 항목은 손익분기 횟수를 확정할 수 없습니다."));
    }
    private Report recommendations() {
        var rows=cardEstimates();
        return wrap("보유 카드 "+customerCards().stream().filter(FinanceCard::isOwned).count()+"개와 샘플 후보 카드의 혜택을 비교했어요. 전월실적·할인한도·연회비를 반영한 추정치이며 실제 상품 추천이나 발급 승인이 아니에요.",
            map("cards",rows,"termsSource","가상 카드 상품 조건. 실제 우리카드 상품·현재 약관이 아닙니다.",
                "youthPlanAlternatives",ctx().entrySet().stream().filter(e->e.getValue().getYouthPlanAmount()!=null).map(e->map("subscriptionId",e.getKey(),"demoYouthAmount",e.getValue().getYouthPlanAmount(),"eligibility","샘플 연령 조건 외 실제 서비스 자격 확인 필요")).toList()));
    }
    private List<Map<String,Object>> cardEstimates() {
        var age=Period.between(customer().getBirthDate(),today()).getYears();var result=new ArrayList<Map<String,Object>>();var context=ctx();
        for(var card:customerCards()) {
            var projectedSpend=new TreeMap<YearMonth,BigDecimal>();
            for(var subscription:subs().stream().filter(FinanceService::active).toList()) {
                var c=context.get(subscription.getId());
                if(!"ALL".equals(card.getBenefitCategory()) && (c==null || !card.getBenefitCategory().equals(c.getServiceFamily())))continue;
                boolean yearly=subscription.getBillingCycle().contains("년") || subscription.getBillingCycle().contains("연");
                var due=subscription.getNextPaymentDate();
                while(due.isBefore(today()))due=yearly?due.plusYears(1):due.plusMonths(1);
                while(due.isBefore(today().plusYears(1))) {
                    projectedSpend.merge(YearMonth.from(due),subscription.getAmount(),BigDecimal::add);
                    due=yearly?due.plusYears(1):due.plusMonths(1);
                }
            }
            var conditionalAnnualDiscount=projectedSpend.values().stream().map(amount->amount.multiply(card.getDiscountRate()).min(card.getMonthlyCap()))
                .reduce(BigDecimal.ZERO,BigDecimal::add).setScale(0,RoundingMode.DOWN);
            var plannedCurrentMonthSpend=projectedSpend.getOrDefault(YearMonth.from(today()),BigDecimal.ZERO);
            var used=txs().stream().filter(FinanceService::approved).filter(t->card.getId().equals(t.getCardId()))
                .filter(t->t.getAuthorizedAt().toLocalDate().getYear()==today().getYear() && t.getAuthorizedAt().toLocalDate().getMonth()==today().getMonth())
                .filter(t->"ALL".equals(card.getBenefitCategory()) || card.getBenefitCategory().equals(t.getCategory())).map(FinanceTransaction::getAmount).reduce(BigDecimal.ZERO,BigDecimal::add)
                .multiply(card.getDiscountRate()).min(card.getMonthlyCap());
            var ageOk=age>=card.getMinimumAge() && age<=card.getMaximumAge();
            var regionOk="ALL".equals(card.getRequiredRegion()) || card.getRequiredRegion().equals(customer().getRegion());
            var jobOk="ANY".equals(card.getRequiredEmployment()) || card.getRequiredEmployment().equals(customer().getEmployment());
            var eligible=ageOk && regionOk && jobOk && (!card.isOwned() || "ACTIVE".equals(card.getCardStatus()));
            var spendMet=card.getPreviousMonthSpend().compareTo(card.getMinimumSpend())>=0;
            if(!spendMet)used=BigDecimal.ZERO;
            var possibleDiscount=plannedCurrentMonthSpend.multiply(card.getDiscountRate()).min(card.getMonthlyCap()).setScale(0,RoundingMode.DOWN);
            if(!eligible){conditionalAnnualDiscount=BigDecimal.ZERO;possibleDiscount=BigDecimal.ZERO;}
            var currentDiscount=card.isOwned() && spendMet && eligible?possibleDiscount.min(card.getMonthlyCap().subtract(used).max(BigDecimal.ZERO)):BigDecimal.ZERO;
            result.add(map("cardId",card.getId(),"name",card.getProductName(),"issuer",card.getIssuer(),"owned",card.isOwned(),"profileConditionsMet",eligible,
                "previousMonthSpend",card.getPreviousMonthSpend(),"minimumSpend",card.getMinimumSpend(),"minimumSpendMet",spendMet,
                "discountRate",card.getDiscountRate(),"monthlyCap",card.getMonthlyCap(),"estimatedUsedCapThisMonth",used,
                "annualFee",card.getAnnualFee(),"discountIfAllConditionsMet",possibleDiscount,"projectedEligiblePaymentsByMonth",projectedSpend,"incrementalDiscountAvailableOnHeldCard",currentDiscount,
                "annualNetBenefitIfConditionsMetEveryMonth",conditionalAnnualDiscount.subtract(card.getAnnualFee()),
                "issuanceEligibility","UNKNOWN_REQUIRES_ISSUER_SCREENING","futureSpendEligibility",card.isOwned()?(spendMet?"MET_LAST_MONTH":"NOT_MET_LAST_MONTH"):"UNKNOWN",
                "projectionScope","등록된 구독의 실제 청구 주기별 향후 1년 결제. 매월 실적 달성 가정.",
                "note","중복 할인·실적 제외·신규 발급 유예 등 실제 약관은 미연동. 계산은 샘플 약관에 한정합니다."));
        }
        return result;
    }
    private Report customerContext() {
        var c=customer();var age=Period.between(c.getBirthDate(),today()).getYears();
        var monthly=subs().stream().filter(FinanceService::active).map(this::monthly).reduce(BigDecimal.ZERO,BigDecimal::add);
        var peer=cohorts.findAll().stream().filter(b->age>=b.getMinimumAge() && age<=b.getMaximumAge())
            .filter(b->b.getRegion().equals(c.getRegion())).map(b->map("minimumAge",b.getMinimumAge(),"maximumAge",b.getMaximumAge(),
                "region",b.getRegion(),"monthlyAverage",b.getMonthlySubscriptionAverage(),"sampleCount",b.getSampleCount(),
                "differenceFromAverage",monthly.subtract(b.getMonthlySubscriptionAverage()),"source","가상 집계 데이터. 실제 이용자 통계가 아닙니다.")).toList();
        return wrap("저장된 연령·거주 지역·직업 정보로 안내 수준과 샘플 자격 조건을 맞출 수 있어요. 소득 대비 월 환산 구독 비중은 "+ratio(monthly,c.getMonthlyIncome())+"%예요.",
            map("age",age,"region",c.getRegion(),"employment",c.getEmployment(),"subscriptionToIncomePercent",ratio(monthly,c.getMonthlyIncome()),
                "preferredNoticeHour",c.getPreferredNoticeHour(),"peerComparisonAvailable",!peer.isEmpty(),"peerComparison",peer,"genderUsed",false));
    }
    private Report cardSchedule() {
        var rows=customerCards().stream().filter(FinanceCard::isOwned).map(c -> map("cardId",c.getId(),"name",c.getProductName(),
            "paymentOn",c.getPaymentOn(),"outstanding",c.getOutstanding(),"expiresOn",c.getExpiresOn(),"status",c.getCardStatus(),
            "points",c.getPointsBalance(),"pointsExpireOn",c.getPointsExpireOn(),"annualFee",c.getAnnualFee(),"annualFeeOn",c.getAnnualFeeOn())).toList();
        return wrap("보유 카드의 대금 결제일, 유효기간, 포인트 소멸일, 연회비 예정일을 조회했어요.",map("cards",rows));
    }
    public List<Notice> notices() {
        if(!customers.existsById(CUSTOMER))return List.of();
        var result=new ArrayList<Notice>();var context=ctx();
        for(var s:subs().stream().filter(FinanceService::active).toList()) {
            if(s.getNextPaymentDate().equals(today().plusDays(1)))result.add(new Notice("payment:"+s.getId()+":"+s.getNextPaymentDate(),"내일 "+s.getName()+" "+won(s.getAmount())+"원 결제 예정이에요. 카드 상태와 혜택 조건을 확인해보세요."));
            var c=context.get(s.getId());if(c==null)continue;
            if(soon(c.getTrialEndsOn(),3))result.add(new Notice("trial:"+s.getId()+":"+c.getTrialEndsOn(),s.getName()+" 무료체험이 "+c.getTrialEndsOn()+"에 끝날 예정이에요. 유료 전환 조건을 확인해보세요."));
            if(soon(c.getAnnualRenewalOn(),30))result.add(new Notice("renewal:"+s.getId()+":"+c.getAnnualRenewalOn(),s.getName()+" 갱신 예정일은 "+c.getAnnualRenewalOn()+"이에요. 계속 이용할지 확인해보세요."));
        }
        for(var card:customerCards().stream().filter(FinanceCard::isOwned).toList()) {
            if(soon(card.getPointsExpireOn(),7))result.add(new Notice("points:"+card.getId()+":"+card.getPointsExpireOn(),card.getProductName()+"의 샘플 포인트 "+won(card.getPointsBalance())+"점 소멸 예정일은 "+card.getPointsExpireOn()+"이에요."));
            if(soon(card.getExpiresOn(),30))result.add(new Notice("expiry:"+card.getId()+":"+card.getExpiresOn(),card.getProductName()+"의 유효기간이 "+card.getExpiresOn()+"까지예요. 정기결제에 사용하는 카드인지 확인해보세요."));
            if(soon(card.getPaymentOn(),3))result.add(new Notice("bill:"+card.getId()+":"+card.getPaymentOn(),card.getProductName()+"의 대금 결제 예정일은 "+card.getPaymentOn()+"이에요. 샘플 청구 예정액은 "+won(card.getOutstanding())+"원이에요."));
            if(soon(card.getAnnualFeeOn(),14))result.add(new Notice("annual-fee:"+card.getId()+":"+card.getAnnualFeeOn(),card.getProductName()+" 연회비 "+won(card.getAnnualFee())+"원 예정일은 "+card.getAnnualFeeOn()+"이에요."));
        }
        return result;
    }
    private static Map<String,Object> transactionView(FinanceTransaction t) {
        return map("id",t.getId(),"cardId",t.getCardId(),"subscriptionId",t.getSubscriptionId(),"merchantLabel",t.getMerchantLabel(),
            "amount",t.getAmount(),"authorizedAt",t.getAuthorizedAt(),"status",t.getTransactionStatus(),"declineReason",t.getDeclineReason());
    }
    private static boolean soon(LocalDate date,int days) {return date!=null && !date.isBefore(today()) && !date.isAfter(today().plusDays(days));}
    private BigDecimal monthly(Subscription s) {return s.monthlyEquivalent();}
    private static BigDecimal ratio(BigDecimal n,BigDecimal d) {return d.signum()==0?null:n.multiply(BigDecimal.valueOf(100)).divide(d,2,RoundingMode.HALF_UP);}
    private static LocalDate today(){return LocalDate.now(KST);}
    private static String won(BigDecimal value){return String.format(Locale.KOREA,"%,.0f",value);}
    private static Report wrap(String summary,Map<String,Object> data){return new Report("DEMO",LocalDateTime.now(KST),summary,data);}
    public static Map<String,Object> map(Object... kv){var map=new LinkedHashMap<String,Object>();for(int i=0;i<kv.length;i+=2)map.put((String)kv[i],kv[i+1]);return map;}
}
