package com.woori.wonnie.assistant;

import com.woori.wonnie.finance.FinanceService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;

@Service
@Profile("!openai")
public class RuleBasedAssistantResponder implements AssistantResponder {
    private final FinanceService finance;
    public RuleBasedAssistantResponder(FinanceService finance) { this.finance=finance; }
    @Override
    public String respond(String customerId,String message,List<AssistantMessage> history) {
        var text=message.toLowerCase(Locale.ROOT);
        if(text.contains("캘린더") || text.contains("일정 등록")) return "현재는 카드·구독 상담을 제공하고 있어요. 캘린더는 아직 연동하지 않았어요.";
        String topic;
        String merchant=null;
        if(has(text,"apple","bill","결제명","어떤 앱")) {topic="merchant";merchant=text.contains("apple")?"APPLE.COM/BILL":message;}
        else if(has(text,"중복","겹치","가족","통신 결합")) topic="overlaps";
        else if(has(text,"인상","올랐","오른","가격","환율"))topic="price-changes";
        else if(has(text,"추천","혜택","청년","어떤 카드"))topic="card-recommendations";
        else if(has(text,"해지","취소","적금","리볼빙"))topic="cancellations";
        else if(has(text,"이용","사용량","도서관","손익"))topic="usage";
        else if(has(text,"무료","체험","갱신"))topic="renewals";
        else if(has(text,"소멸","만료","연회비","카드대금"))topic="card-schedule";
        else if(has(text,"잔액","한도","실적","내일","결제 전","포인트"))topic="payment-readiness";
        else if(has(text,"소득","또래","말투","알림 시간"))topic="customer-context";
        else if(has(text,"구독","총액","월간","보고서","리포트"))topic="subscriptions";
        else return "안녕하세요! 카드·구독 내역을 바탕으로 구독 총액, 가격 인상, 중복 구독, 카드 혜택과 결제 일정을 안내해드려요. 예를 들어 ‘구독료가 오른 내역 알려줘’라고 물어보세요.";
        var report=finance.report(topic,merchant);
        var answer=new StringBuilder(report.summary());
        if(topic.equals("subscriptions")) {
            for(var row:rows(report.data().get("subscriptions"))) answer.append("\n· ").append(row.get("name")).append(": ").append(row.get("amount")).append("원 / ").append(row.get("billingCycle"));
        }
        if(topic.equals("card-schedule")) {
            for(var row:rows(report.data().get("cards"))) answer.append("\n· ").append(row.get("name")).append(": 대금 ").append(row.get("paymentOn")).append(", 만료 ").append(row.get("expiresOn")).append(", 포인트 소멸 ").append(row.get("pointsExpireOn"));
        }
        if(topic.equals("card-recommendations")) {
            for(var row:rows(report.data().get("cards"))) answer.append("\n· ").append(row.get("name")).append(": 조건 충족 가정 연간 순혜택 ").append(row.get("annualNetBenefitIfConditionsMetEveryMonth")).append("원, 전월실적 ").append(Boolean.TRUE.equals(row.get("minimumSpendMet"))?"충족":"미달/미확인");
        }
        return answer.toString();
    }
    @SuppressWarnings("unchecked")
    private static List<java.util.Map<String,Object>> rows(Object data) {return (List<java.util.Map<String,Object>>)data;}
    private static boolean has(String value,String... terms) { for(var term:terms)if(value.contains(term))return true; return false; }
}
