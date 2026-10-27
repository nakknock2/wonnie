package com.woori.wonnie.finance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/finance")
public class InternalFinanceController {
    public static final String CUSTOMER_HEADER = "X-Wonnie-Customer-Id";
    private final FinanceService finance;

    public InternalFinanceController(FinanceService finance) {
        this.finance = finance;
    }

    @GetMapping("/{topic}")
    public FinanceService.Report report(
            @PathVariable String topic,
            @RequestHeader(CUSTOMER_HEADER) String customerId,
            @RequestParam(required = false, name = "name") String name) {
        if (customerId.isBlank() || customerId.length() > 100) {
            throw new IllegalArgumentException("Invalid customer id.");
        }
        if (name != null && name.length() > 200) {
            throw new IllegalArgumentException("Invalid merchant name.");
        }
        return finance.report(customerId, topic, name);
    }
}
