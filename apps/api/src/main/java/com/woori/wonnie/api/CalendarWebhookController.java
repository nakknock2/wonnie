package com.woori.wonnie.api;

import com.woori.wonnie.auth.CurrentCustomer;
import com.woori.wonnie.calendar.CalendarSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class CalendarWebhookController {
    private final CalendarSyncService sync;
    private final CurrentCustomer currentCustomer;

    public CalendarWebhookController(CalendarSyncService sync, CurrentCustomer currentCustomer) {
        this.sync = sync;
        this.currentCustomer = currentCustomer;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Goog-Channel-ID", required = false) String channelId,
            @RequestHeader(value = "X-Goog-Channel-Token", required = false) String channelToken,
            @RequestHeader(value = "X-Goog-Resource-State", required = false) String resourceState
    ) {
        return sync.acceptWebhook(channelId, channelToken, resourceState)
                ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> sync(Authentication authentication) {
        sync.sync(currentCustomer.id(), authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
