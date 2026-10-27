package com.woori.wonnie.api;

import com.woori.wonnie.auth.CurrentCustomer;
import com.woori.wonnie.consent.ConsentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consents")
public class ConsentController {
    private final ConsentService consents;
    private final CurrentCustomer currentCustomer;

    public ConsentController(ConsentService consents, CurrentCustomer currentCustomer) {
        this.consents = consents;
        this.currentCustomer = currentCustomer;
    }

    @GetMapping
    public ConsentService.ConsentState get() {
        return consents.state(currentCustomer.id());
    }

    @PutMapping("/subscription-data")
    public ConsentService.ConsentState agreeToSubscriptionData() {
        return consents.agreeToSubscriptionData(currentCustomer.id());
    }

    @PutMapping("/notifications")
    public ConsentService.ConsentState updateNotifications(@Valid @RequestBody NotificationConsentRequest request) {
        return consents.updateNotifications(currentCustomer.id(), request.d1(), request.priceChange());
    }

    public record NotificationConsentRequest(@NotNull Boolean d1, @NotNull Boolean priceChange) {
    }
}
