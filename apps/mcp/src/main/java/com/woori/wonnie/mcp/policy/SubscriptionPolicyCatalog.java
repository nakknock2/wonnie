package com.woori.wonnie.mcp.policy;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class SubscriptionPolicyCatalog {

    private final List<PaymentFailurePolicy> policies;

    public SubscriptionPolicyCatalog(JsonMapper mapper) throws IOException {
        try (var input = new ClassPathResource("policies/subscription-payment-failure-policies.json").getInputStream()) {
            policies = List.copyOf(Arrays.asList(mapper.readValue(input, PaymentFailurePolicy[].class)));
        }
    }

    public PaymentFailurePolicy find(String serviceName, String billingChannel) {
        if (serviceName == null || serviceName.isBlank() || billingChannel == null || billingChannel.isBlank()) {
            throw new IllegalArgumentException("서비스명과 실제 결제 경로를 지정해 주세요.");
        }
        var normalized = normalize(serviceName);
        var policy = policies.stream()
                .filter(entry -> normalize(entry.serviceName()).equals(normalized)
                        || entry.aliases().stream().anyMatch(alias -> normalize(alias).equals(normalized)))
                .findFirst()
                .orElse(null);
        if (policy != null && policy.billingChannel().equalsIgnoreCase(billingChannel.trim())) {
            return policy;
        }
        // Direct billing policies must not be applied to app-store or partner subscriptions.
        return new PaymentFailurePolicy(
                policy == null ? serviceName : policy.serviceName(), List.of(), billingChannel.trim(), null,
                "해당 서비스·결제 경로의 처리 정책은 확인되지 않았습니다. 공식 고객센터에서 확인해 주세요.",
                "미확인. 자동 해지·이용 제한·재청구가 발생할 수 있습니다.",
                "미확인. 차단 해제 후 자동 복구나 결제일 변경을 보장하지 않습니다.", List.of());
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
