package com.woori.wonnie.api;

import com.woori.wonnie.calendar.CalendarSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class CalendarWebhookController {
    private final CalendarSyncService sync;

    public CalendarWebhookController(CalendarSyncService sync) {
        this.sync = sync;
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
}
