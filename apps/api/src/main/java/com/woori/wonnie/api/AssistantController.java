package com.woori.wonnie.api;

import com.woori.wonnie.api.ApiDtos.AssistantRequest;
import com.woori.wonnie.api.ApiDtos.AssistantConversationResponse;
import com.woori.wonnie.api.ApiDtos.AssistantMessageExchange;
import com.woori.wonnie.api.ApiDtos.AssistantRequestAcceptedResponse;
import com.woori.wonnie.api.ApiDtos.AssistantRequestStatusResponse;
import com.woori.wonnie.api.ApiDtos.AssistantStreamRequest;
import com.woori.wonnie.api.ApiDtos.UnreadCountResponse;
import com.woori.wonnie.assistant.AssistantService;
import com.woori.wonnie.assistant.AssistantSseConnectionManager;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantSseConnectionManager sseConnectionManager;

    public AssistantController(
            AssistantService assistantService,
            AssistantSseConnectionManager sseConnectionManager
    ) {
        this.assistantService = assistantService;
        this.sseConnectionManager = sseConnectionManager;
    }

    @GetMapping("/conversation")
    public AssistantConversationResponse conversation() {
        return assistantService.conversation(com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID);
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> events() {
        var customerId = com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID;
        var snapshot = assistantService.connectionSnapshot(customerId);
        var emitter = sseConnectionManager.connect(
                customerId, snapshot.conversationId(), snapshot.unreadCount());
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache, no-store, no-transform")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    @PostMapping("/messages")
    public AssistantMessageExchange message(@Valid @RequestBody AssistantRequest request) {
        return assistantService.send(com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID, request.message(), request.topic());
    }

    @PostMapping("/requests")
    public ResponseEntity<AssistantRequestAcceptedResponse> submit(@Valid @RequestBody AssistantStreamRequest request) {
        var submission = assistantService.submit(
                com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID,
                request.requestId(), request.message(), request.topic());
        if (submission.created()) {
            return ResponseEntity.accepted().body(submission.response());
        }
        return ResponseEntity.ok(submission.response());
    }

    @GetMapping("/requests/{requestId}")
    public AssistantRequestStatusResponse requestStatus(@PathVariable UUID requestId) {
        return assistantService.requestStatus(
                com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID, requestId);
    }

    @PostMapping("/requests/{requestId}/cancel")
    public ResponseEntity<AssistantRequestStatusResponse> cancelRequest(@PathVariable UUID requestId) {
        var status = assistantService.cancelRequest(
                com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID, requestId);
        if ("CANCELLING".equals(status.status())) return ResponseEntity.accepted().body(status);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(
                assistantService.unreadCount(com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID));
    }

    @PostMapping("/conversation/read")
    public UnreadCountResponse markRead() {
        var customerId = com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID;
        assistantService.markRead(customerId);
        return new UnreadCountResponse(assistantService.unreadCount(customerId));
    }
}
