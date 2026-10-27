package com.woori.wonnie.api;

import com.woori.wonnie.api.ApiDtos.AssistantRequest;
import com.woori.wonnie.api.ApiDtos.AssistantConversationResponse;
import com.woori.wonnie.api.ApiDtos.AssistantMessageExchange;
import com.woori.wonnie.api.ApiDtos.UnreadCountResponse;
import com.woori.wonnie.assistant.AssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @GetMapping("/conversation")
    public AssistantConversationResponse conversation() {
        return assistantService.conversation(com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID);
    }

    @PostMapping("/messages")
    public AssistantMessageExchange message(@Valid @RequestBody AssistantRequest request) {
        return assistantService.send(com.woori.wonnie.dashboard.DashboardService.DEMO_CUSTOMER_ID, request.message(), request.topic());
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
