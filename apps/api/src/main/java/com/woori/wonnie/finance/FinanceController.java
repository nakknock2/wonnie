package com.woori.wonnie.finance;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    private final FinanceService finance;
    public FinanceController(FinanceService finance) { this.finance = finance; }
    @GetMapping("/{topic}")
    public FinanceService.Report report(@PathVariable String topic,
            @RequestParam(required = false, name = "name") String name) {
        if (name != null && name.length() > 200) throw new IllegalArgumentException("결제명은 200자 이내로 입력하세요.");
        return finance.report(topic, name);
    }
}
