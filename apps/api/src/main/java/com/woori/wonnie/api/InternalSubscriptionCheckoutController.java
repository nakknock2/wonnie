package com.woori.wonnie.api;

import com.woori.wonnie.subscription.SubscriptionCheckoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/subscription-checkout")
public class InternalSubscriptionCheckoutController {
    public static final String CUSTOMER_HEADER = "X-Wonnie-Customer-Id";
    private final SubscriptionCheckoutService checkout;

    public InternalSubscriptionCheckoutController(SubscriptionCheckoutService checkout) {
        this.checkout = checkout;
    }

    @GetMapping("/options")
    public SubscriptionCheckoutService.CheckoutOptions options(
            @RequestHeader(CUSTOMER_HEADER) String customerId,
            @RequestParam String serviceName,
            @RequestParam(required = false) String planCode) {
        return checkout.options(customerId, serviceName, planCode);
    }

    @PostMapping("/cards")
    public SubscriptionCheckoutService.IssuedCard issue(
            @RequestHeader(CUSTOMER_HEADER) String customerId,
            @Valid @RequestBody SubscriptionCheckoutController.CardIssueRequest request) {
        return checkout.issueCard(customerId, request.productId(), request.allTermsAgreed());
    }

    @PostMapping("/subscriptions")
    public SubscriptionCheckoutService.CheckoutResult subscribe(
            @RequestHeader(CUSTOMER_HEADER) String customerId,
            @Valid @RequestBody SubscriptionCheckoutController.SubscribeRequest request) {
        return checkout.subscribe(customerId, request.requestId(), request.service(), request.planCode(),
                request.cardId(), request.allTermsAgreed());
    }
}
