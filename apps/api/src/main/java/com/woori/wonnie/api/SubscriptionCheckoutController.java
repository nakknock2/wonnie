package com.woori.wonnie.api;

import com.woori.wonnie.auth.CurrentCustomer;
import com.woori.wonnie.subscription.SubscriptionCheckoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscription-checkout")
public class SubscriptionCheckoutController {
    private final CurrentCustomer currentCustomer;
    private final SubscriptionCheckoutService checkout;

    public SubscriptionCheckoutController(CurrentCustomer currentCustomer, SubscriptionCheckoutService checkout) {
        this.currentCustomer = currentCustomer;
        this.checkout = checkout;
    }

    @GetMapping("/options")
    public SubscriptionCheckoutService.CheckoutOptions options(
            @RequestParam @NotBlank @Size(max = 60) String service,
            @RequestParam(required = false) @Size(max = 40) String plan) {
        return checkout.options(currentCustomer.id(), service, plan);
    }

    @PostMapping("/cards")
    public SubscriptionCheckoutService.IssuedCard issue(@Valid @RequestBody CardIssueRequest request) {
        return checkout.issueCard(currentCustomer.id(), request.productId(), request.allTermsAgreed());
    }

    @PostMapping("/subscriptions")
    public SubscriptionCheckoutService.CheckoutResult subscribe(@Valid @RequestBody SubscribeRequest request) {
        return checkout.subscribe(currentCustomer.id(), request.requestId(), request.service(), request.planCode(),
                request.cardId(), request.allTermsAgreed());
    }

    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    public SubscriptionCheckoutService.CancellationResult cancel(
            @PathVariable @NotBlank String subscriptionId,
            @Valid @RequestBody CancelSubscriptionRequest request) {
        return checkout.cancel(currentCustomer.id(), subscriptionId, request.finalApproval());
    }

    public record CardIssueRequest(@NotBlank String productId, boolean allTermsAgreed) {}
    public record SubscribeRequest(@NotNull UUID requestId, @NotBlank String service, @NotBlank String planCode,
                                   @NotBlank String cardId, boolean allTermsAgreed) {}
    public record CancelSubscriptionRequest(boolean finalApproval) {}
}
