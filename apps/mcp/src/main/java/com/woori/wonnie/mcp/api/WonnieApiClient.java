package com.woori.wonnie.mcp.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UriUtils;

@Component
public class WonnieApiClient {
    private static final String CUSTOMER_HEADER = "X-Wonnie-Customer-Id";
    private static final String TOKEN_HEADER = "X-Wonnie-Internal-Token";
    private final RestClient restClient;
    private final String internalToken;

    public WonnieApiClient(RestClient wonnieApiRestClient,
            @Value("${app.api.internal-token:local-development-token}") String internalToken) {
        this.restClient = wonnieApiRestClient;
        this.internalToken = internalToken;
    }

    public Map<String, Object> report(String topic) {
        return report(topic, null);
    }

    public Map<String, Object> report(String topic, String merchant) {
        var customerId = currentCustomerId();
        try {
            var result = restClient.get().uri(builder -> {
                builder.path("/api/internal/finance/{topic}");
                if (merchant != null) builder.queryParam("name", "{merchant}");
                return merchant == null ? builder.build(topic) : builder.build(topic, merchant);
            }).header(CUSTOMER_HEADER, customerId)
                    .header(TOKEN_HEADER, internalToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (result == null) throw new IllegalStateException("Wonnie API returned an empty response.");
            return result;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Finance API request failed with HTTP " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new IllegalStateException("Could not connect to the finance API.");
        }
    }

    public Map<String, Object> checkoutOptions(String serviceName, String planCode) {
        var customerId = currentCustomerId();
        try {
            var result = restClient.get().uri(builder -> {
                builder.path("/api/internal/subscription-checkout/options")
                        .queryParam("serviceName", "{serviceName}");
                if (planCode != null && !planCode.isBlank()) builder.queryParam("planCode", "{planCode}");
                return planCode == null || planCode.isBlank()
                        ? builder.build(serviceName) : builder.build(serviceName, planCode);
            }).header(CUSTOMER_HEADER, customerId).header(TOKEN_HEADER, internalToken)
                    .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (result == null) throw new IllegalStateException("Wonnie API returned an empty response.");
            return result;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Checkout API request failed with HTTP " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new IllegalStateException("Could not connect to the checkout API.");
        }
    }

    public Map<String, Object> issueCard(String productId, boolean allTermsAgreed) {
        return checkoutPost("/api/internal/subscription-checkout/cards",
                Map.of("productId", productId, "allTermsAgreed", allTermsAgreed));
    }

    public Map<String, Object> createSubscription(
            String requestId, String serviceName, String planCode, String cardId, boolean allTermsAgreed
    ) {
        return checkoutPost("/api/internal/subscription-checkout/subscriptions", Map.of(
                "requestId", requestId, "service", serviceName, "planCode", planCode,
                "cardId", cardId, "allTermsAgreed", allTermsAgreed));
    }

    public Map<String, Object> cancelSubscription(String subscriptionId, boolean finalApproval) {
        var encodedId = UriUtils.encodePathSegment(subscriptionId, StandardCharsets.UTF_8);
        return checkoutPost("/api/internal/subscription-checkout/subscriptions/" + encodedId + "/cancel",
                Map.of("finalApproval", finalApproval));
    }

    private Map<String, Object> checkoutPost(String path, Map<String, Object> body) {
        var customerId = currentCustomerId();
        try {
            var result = restClient.post().uri(path).header(CUSTOMER_HEADER, customerId)
                    .header(TOKEN_HEADER, internalToken).body(body).retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (result == null) throw new IllegalStateException("Wonnie API returned an empty response.");
            return result;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Checkout API request failed with HTTP " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new IllegalStateException("Could not connect to the checkout API.");
        }
    }

    private static String currentCustomerId() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new IllegalStateException("MCP request context is unavailable.");
        }
        HttpServletRequest request = servletAttributes.getRequest();
        var customerId = request.getHeader(CUSTOMER_HEADER);
        if (customerId == null || customerId.isBlank() || customerId.length() > 100) {
            throw new IllegalStateException("MCP customer context is missing.");
        }
        return customerId;
    }
}
