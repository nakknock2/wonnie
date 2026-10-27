package com.woori.wonnie.api;

import com.woori.wonnie.api.ApiDtos.SubscriptionPriceObservationAcceptedResponse;
import com.woori.wonnie.api.ApiDtos.SubscriptionPriceObservationRequest;
import com.woori.wonnie.subscription.SubscriptionPriceObservationIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/subscription-price-observations")
public class SubscriptionPriceObservationController {
    private final SubscriptionPriceObservationIngestionService ingestionService;

    public SubscriptionPriceObservationController(SubscriptionPriceObservationIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionPriceObservationAcceptedResponse> observe(
            @Valid @RequestBody SubscriptionPriceObservationRequest request
    ) {
        var status = ingestionService.accept(request);
        var response = new SubscriptionPriceObservationAcceptedResponse(request.eventId(), status);
        return "QUEUED".equals(status)
                ? ResponseEntity.accepted().body(response)
                : ResponseEntity.ok(response);
    }
}
