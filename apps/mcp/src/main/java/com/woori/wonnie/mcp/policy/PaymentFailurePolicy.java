package com.woori.wonnie.mcp.policy;

import java.time.LocalDate;
import java.util.List;

public record PaymentFailurePolicy(
        String serviceName,
        List<String> aliases,
        String billingChannel,
        LocalDate verifiedOn,
        String failureHandling,
        String retryAndTermination,
        String billingDateAfterRecovery,
        List<Source> sources
) {
    public record Source(String title, String url) {
    }
}
