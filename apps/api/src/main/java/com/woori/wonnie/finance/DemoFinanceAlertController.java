package com.woori.wonnie.finance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@ConditionalOnProperty(name = "app.finance.demo-alert-endpoint-enabled", havingValue = "true")
@RequestMapping("/api/internal/finance-alerts")
public class DemoFinanceAlertController {
    private final FinanceAlertScheduler scheduler;
    public DemoFinanceAlertController(FinanceAlertScheduler scheduler){this.scheduler=scheduler;}
    @PostMapping("/scan")
    public Map<String,Object> scan(){return Map.of("dataSource","DATABASE","matchedNotices",scheduler.scan());}
}
