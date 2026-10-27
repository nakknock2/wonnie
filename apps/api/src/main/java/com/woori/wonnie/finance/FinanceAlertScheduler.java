package com.woori.wonnie.finance;

import com.woori.wonnie.assistant.AssistantService;
import com.woori.wonnie.consent.ConsentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class FinanceAlertScheduler {
    private final FinanceService finance;
    private final FinanceCustomerRepository customers;
    private final AssistantService assistant;
    private final ConsentService consents;
    public FinanceAlertScheduler(FinanceService finance, FinanceCustomerRepository customers,AssistantService assistant,
            ConsentService consents) {
        this.finance=finance;this.customers=customers;this.assistant=assistant;this.consents=consents;
    }
    @Scheduled(cron = "${app.finance.alert-cron:0 0 * * * *}", zone = "Asia/Seoul")
    public void scheduledScan() {
        customers.findById(FinanceService.CUSTOMER).filter(c->c.getPreferredNoticeHour()==LocalTime.now(ZoneId.of("Asia/Seoul")).getHour()).ifPresent(c->scan());
    }
    public int scan() {
        var notices=finance.notices().stream()
            .filter(n -> !n.key().startsWith("payment:")
                || consents.isAgreed(FinanceService.CUSTOMER, ConsentService.PUSH_D1))
            .toList();
        notices.forEach(n->assistant.addFinanceNotice(FinanceService.CUSTOMER,n.content(),"finance:"+n.key()));
        return notices.size();
    }
}
