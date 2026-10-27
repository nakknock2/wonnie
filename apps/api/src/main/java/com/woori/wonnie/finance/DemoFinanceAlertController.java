package com.woori.wonnie.finance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@ConditionalOnProperty(name = "app.finance.demo-data-enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/internal/finance-alerts")
public class DemoFinanceAlertController {
    private final FinanceAlertScheduler scheduler;
    public DemoFinanceAlertController(FinanceAlertScheduler scheduler){this.scheduler=scheduler;}
    @PostMapping("/scan")
    public Map<String,Object> scan(){return Map.of("dataSource","DEMO","matchedNotices",scheduler.scan());}
}
