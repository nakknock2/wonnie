package com.woori.wonnie.assistant;
import java.util.List;
public interface AssistantResponder {
    String respond(String customerId, String message, List<AssistantMessage> history);
}
