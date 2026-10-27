package com.woori.wonnie.finance;

import com.woori.wonnie.dashboard.DashboardService;
import com.woori.wonnie.subscription.Subscription;
import com.woori.wonnie.subscription.SubscriptionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.finance.demo-data-enabled", havingValue = "true", matchIfMissing = true)
public class DemoFinanceDataLoader implements ApplicationRunner {
    private final FinanceCustomerRepository customers;
    private final FinanceCardRepository cards;
    private final FinanceTransactionRepository transactions;
    private final SubscriptionContextRepository contexts;
    private final MerchantDirectoryRepository merchants;
    private final FinanceAlternativeRepository alternatives;
    private final SubscriptionRepository subscriptions;
    private final FinanceCohortBenchmarkRepository cohorts;
    public DemoFinanceDataLoader(FinanceCustomerRepository customers, FinanceCardRepository cards,
            FinanceTransactionRepository transactions, SubscriptionContextRepository contexts,
            MerchantDirectoryRepository merchants, FinanceAlternativeRepository alternatives,
            SubscriptionRepository subscriptions, FinanceCohortBenchmarkRepository cohorts) {
        this.customers=customers; this.cards=cards; this.transactions=transactions; this.contexts=contexts;
        this.merchants=merchants; this.alternatives=alternatives; this.subscriptions=subscriptions; this.cohorts=cohorts;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customers.existsById(DashboardService.DEMO_CUSTOMER_ID)) return;
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        var customer = DashboardService.DEMO_CUSTOMER_ID;
        customers.save(new FinanceCustomer(customer,today.minusYears(26),"서울","EMPLOYEE",n(3000000),n(8000),19));
        cohorts.save(new FinanceCohortBenchmark("cohort-20s-seoul",20,29,"서울",n(45000),100));
        cards.saveAll(List.of(
            card("card-woori","데모 우리 구독카드","우리",true,"VIDEO",280000,300000,"0.10",10000,20000,12000,"ACTIVE",today),
            card("card-other","데모 타사 생활카드","타사",true,"ALL",450000,300000,"0.15",15000,15000,500000,"ACTIVE",today),
            card("card-stopped","데모 정지 카드","우리",true,"ALL",100000,200000,"0.05",5000,10000,0,"SUSPENDED",today),
            card("product-youth","데모 청년 구독카드","우리",false,"VIDEO",0,300000,"0.20",12000,10000,0,"CANDIDATE",today),
            card("product-seoul","데모 서울 생활카드","우리",false,"ALL",0,200000,"0.08",10000,12000,0,"CANDIDATE",today)));
        contexts.saveAll(List.of(
            new SubscriptionContext("sub-netflix","card-woori","VIDEO","스탠다드(샘플)",null,null,"ACTIVE",today,null,2,"샘플 서비스 이용 로그",null,false,n(12000),null),
            new SubscriptionContext("sub-youtube","card-other","VIDEO","프리미엄(샘플)",null,null,"ACTIVE",today,null,null,"이용 기록 미연동","통신요금에 동종 영상 혜택 포함(샘플)",true,null,null),
            new SubscriptionContext("sub-icloud","card-stopped","CLOUD","클라우드(샘플)",null,null,"UNKNOWN",null,null,null,"이용 기록 미연동",null,false,null,null),
            new SubscriptionContext("sub-millie","card-other","BOOK","무료체험(샘플)",today.plusDays(2),null,"TRIAL",today,null,0,"샘플 서비스 이용 로그",null,false,null,null),
            new SubscriptionContext("sub-cancelled","card-other","VIDEO","월간(샘플)",null,null,"CANCELLED",today.minusDays(2),today.minusDays(2),null,"이용 기록 미연동",null,false,null,null),
            new SubscriptionContext("sub-gym","card-other","GYM","연간권(샘플)",null,today.plusDays(20),"ACTIVE",today,null,3,"샘플 출입 로그",null,false,null,n(10000))));
        if (!subscriptions.existsById("sub-cancelled")) subscriptions.save(Subscription.cancelledDemo("sub-cancelled",customer,today));
        if (!subscriptions.existsById("sub-gym")) subscriptions.save(Subscription.membershipDemo(customer,today));
        transactions.saveAll(List.of(
            tx("tx-netflix-old","card-woori","sub-netflix","NETFLIX.COM","VIDEO",15000,"KRW","15000","1",today.minusDays(40),"APPROVED",null),
            tx("tx-netflix-new","card-woori","sub-netflix","NETFLIX.COM","VIDEO",17000,"KRW","17000","1",today.minusDays(10),"APPROVED",null),
            tx("tx-youtube-old","card-other","sub-youtube","GOOGLE YOUTUBE","VIDEO",12900,"KRW","12900","1",today.minusDays(35),"APPROVED",null),
            tx("tx-youtube-new","card-other","sub-youtube","GOOGLE YOUTUBE","VIDEO",14900,"KRW","14900","1",today.minusDays(5),"APPROVED",null),
            tx("tx-cloud-old","card-stopped","sub-icloud","APPLE.COM/BILL","CLOUD",4000,"USD","2.99","1337.79",today.minusDays(32),"APPROVED",null),
            tx("tx-cloud-new","card-stopped","sub-icloud","APPLE.COM/BILL","CLOUD",4400,"USD","2.99","1471.57",today.minusDays(2),"APPROVED",null),
            tx("tx-cloud-reject","card-stopped","sub-icloud","APPLE.COM/BILL","CLOUD",4400,"USD","2.99","1471.57",today.minusDays(1),"DECLINED","CARD_SUSPENDED"),
            tx("tx-mobile","card-other",null,"데모 통신사","LIVING",55000,"KRW","55000","1",today.minusDays(8),"APPROVED",null),
            tx("tx-electric","card-other",null,"데모 전기요금","LIVING",30000,"KRW","30000","1",today.minusDays(7),"APPROVED",null),
            tx("tx-bookstore","card-other",null,"데모 서점","BOOK",22000,"KRW","22000","1",today.minusDays(3),"APPROVED",null)));
        merchants.saveAll(List.of(
            new MerchantDirectory("merchant-apple","APPLE.COM/BILL","Apple 결제","iCloud+, Apple Music, 앱 내 구입 등",true,"Apple 구매 내역 또는 영수증을 확인해야 실제 서비스를 확정할 수 있습니다."),
            new MerchantDirectory("merchant-netflix","NETFLIX.COM","Netflix","Netflix",false,"샘플 가맹점 매핑입니다. 실제 결제의 구독 ID를 함께 확인하세요."),
            new MerchantDirectory("merchant-google","GOOGLE YOUTUBE","YouTube Premium","YouTube Premium",false,"샘플 가맹점 매핑입니다.")));
        alternatives.saveAll(List.of(new FinanceAlternative("alt-gym","GYM","서울","데모 동네 피트니스 월간권",n(45000),"가상 지역 시설 예시이며 실제 위치·요금 미연동"),new FinanceAlternative("alt-library","BOOK","서울","데모 지역 전자도서관",n(0),"거주지·회원 등록 조건을 확인해야 하는 가상 예시"),
            new FinanceAlternative("alt-youth","VIDEO","ALL","데모 청년 영상 요금제",n(12000),"만 19~34세라는 가상 조건. 실제 서비스 상품이 아닙니다.")));
    }
    private FinanceCard card(String id,String name,String issuer,boolean owned,String category,long spend,long min,
            String rate,long cap,long fee,long limit,String status,LocalDate today) {
        return new FinanceCard(id,DashboardService.DEMO_CUSTOMER_ID,name,issuer,owned,category,n(spend),n(min),
            new BigDecimal(rate),n(cap),n(fee),n(limit),n(75000),owned?n(40000):n(0),new BigDecimal("0.15"),status,
            today.plusDays(owned?20:365),today.plusDays(3),n(8000),today.plusDays(5),today.plusDays(14),
            id.equals("product-youth")?19:19,id.equals("product-youth")?34:100,id.equals("product-seoul")?"서울":"ALL",id.equals("product-seoul")?"EMPLOYEE":"ANY");
    }
    private FinanceTransaction tx(String id,String card,String subscription,String merchant,String category,long amount,
            String currency,String original,String rate,LocalDate date,String status,String decline) {
        return new FinanceTransaction(id,DashboardService.DEMO_CUSTOMER_ID,card,subscription,merchant,category,
            n(amount),new BigDecimal(original),currency,new BigDecimal(rate),date.atTime(12,0),status,decline);
    }
    private static BigDecimal n(long amount) { return BigDecimal.valueOf(amount); }
}
