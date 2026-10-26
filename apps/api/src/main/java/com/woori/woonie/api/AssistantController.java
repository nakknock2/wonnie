package com.woori.woonie.api;

import com.woori.woonie.api.ApiDtos.AssistantRequest;
import com.woori.woonie.api.ApiDtos.AssistantResponse;
import com.woori.woonie.assistant.AssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @GetMapping("/suggestions")
    public List<String> suggestions() {
        return assistantService.suggestions();
    }

    @PostMapping("/messages")
    public AssistantResponse message(@Valid @RequestBody AssistantRequest request) {
        return assistantService.respond(request.message());
    }
}
