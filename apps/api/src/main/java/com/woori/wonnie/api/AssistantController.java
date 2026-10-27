package com.woori.wonnie.api;

import com.woori.wonnie.api.ApiDtos.AssistantRequest;
import com.woori.wonnie.api.ApiDtos.AssistantConversationResponse;
import com.woori.wonnie.api.ApiDtos.AssistantMessageExchange;
import com.woori.wonnie.api.ApiDtos.UnreadCountResponse;
import com.woori.wonnie.assistant.AssistantService;
import com.woori.wonnie.auth.CurrentCustomer;
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
    private final CurrentCustomer currentCustomer;

    public AssistantController(AssistantService assistantService, CurrentCustomer currentCustomer) {
        this.assistantService = assistantService;
        this.currentCustomer = currentCustomer;
    }

    @GetMapping("/conversation")
    public AssistantConversationResponse conversation() {
        return assistantService.conversation(currentCustomer.id());
    }

    @PostMapping("/messages")
    public AssistantMessageExchange message(@Valid @RequestBody AssistantRequest request) {
        return assistantService.send(currentCustomer.id(), request.message(), request.topic());
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(
                assistantService.unreadCount(currentCustomer.id()));
    }

    @PostMapping("/conversation/read")
    public UnreadCountResponse markRead() {
        var customerId = currentCustomer.id();
        assistantService.markRead(customerId);
        return new UnreadCountResponse(assistantService.unreadCount(customerId));
    }
}
