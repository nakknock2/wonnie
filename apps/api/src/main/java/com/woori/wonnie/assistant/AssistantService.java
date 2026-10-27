package com.woori.wonnie.assistant;

import com.woori.wonnie.api.ApiDtos.AssistantConversationResponse;
import com.woori.wonnie.api.ApiDtos.AssistantMessageExchange;
import com.woori.wonnie.api.ApiDtos.AssistantMessageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AssistantService {
    private final AssistantConversationRepository conversationRepository;
    private final AssistantMessageRepository messageRepository;
    private final AssistantResponder responder;
    private final org.springframework.transaction.support.TransactionTemplate transactions;

    public AssistantService(
            AssistantConversationRepository conversationRepository,
            AssistantMessageRepository messageRepository,
            AssistantResponder responder,
            org.springframework.transaction.PlatformTransactionManager transactionManager
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.responder = responder;
        this.transactions = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    }

    @Transactional
    public AssistantConversationResponse conversation(String customerId) {
        var conversation = getOrCreateConversation(customerId);
        var messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream().map(AssistantService::toResponse).toList();
        var unreadCount = messageRepository.countByConversationIdAndRoleAndReadAtIsNull(
                conversation.getId(), "ASSISTANT");
        return new AssistantConversationResponse(conversation.getId(), messages, unreadCount);
    }

    public AssistantMessageExchange send(String customerId, String message) {
        return send(customerId, message, null);
    }

    public AssistantMessageExchange send(String customerId, String message, String topic) {
        if (message == null || message.isBlank() || message.length() > 4000) {
            throw new IllegalArgumentException("메시지는 1~4000자로 입력해 주세요.");
        }
        var history = transactions.execute(status -> {
            var conversation = getOrCreateConversation(customerId);
            var recent = new java.util.ArrayList<>(messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversation.getId()));
            java.util.Collections.reverse(recent);
            return recent;
        });
        if (topic != null && topic.length() > 200) {
            throw new IllegalArgumentException("상담 주제는 200자 이내로 입력해 주세요.");
        }
        // The selected UI topic is context, not a source of verified financial facts.
        var prompt = topic == null || topic.isBlank() ? message.strip()
                : "화면에서 선택한 상담 주제: " + topic.strip() + "\n사용자 질문: " + message.strip();
        // External LLM/MCP calls run outside the database transaction.
        var reply = responder.respond(customerId, prompt, history);
        return transactions.execute(status -> {
            var conversation = getOrCreateConversation(customerId);
            var userMessage = messageRepository.save(AssistantMessage.userMessage(conversation.getId(), customerId, message.strip()));
            var assistantMessage = messageRepository.save(AssistantMessage.assistantReply(
                    conversation.getId(), customerId, reply, userMessage.getCreatedAt().plusNanos(1_000)));
            conversation.touch();
            return new AssistantMessageExchange(toResponse(userMessage), toResponse(assistantMessage));
        });
    }

    @Transactional
    public void markRead(String customerId) {
        conversationRepository.findByCustomerId(customerId).ifPresent(conversation ->
                messageRepository.markAssistantMessagesRead(
                        conversation.getId(), LocalDateTime.now(java.time.ZoneOffset.UTC)));
    }

    @Transactional(readOnly = true)
    public long unreadCount(String customerId) {
        return conversationRepository.findByCustomerId(customerId)
                .map(conversation -> messageRepository.countByConversationIdAndRoleAndReadAtIsNull(
                        conversation.getId(), "ASSISTANT"))
                .orElse(0L);
    }

    @Transactional
    public void addPriceIncreaseNotice(String customerId, String content, String sourceKey) {
        if (messageRepository.existsBySourceKey(sourceKey)) return;
        var conversation=getOrCreateConversation(customerId);
        messageRepository.save(AssistantMessage.priceIncrease(conversation.getId(),customerId,content,sourceKey));
        conversation.touch();
    }

    @Transactional
    public void addFinanceNotice(String customerId, String content, String sourceKey) {
        if (messageRepository.existsBySourceKey(sourceKey)) return;
        var conversation = getOrCreateConversation(customerId);
        messageRepository.save(AssistantMessage.financeNotice(conversation.getId(), customerId, content, sourceKey));
        conversation.touch();
    }

    private AssistantConversation getOrCreateConversation(String customerId) {
        return conversationRepository.findByCustomerId(customerId)
                .orElseGet(() -> conversationRepository.save(AssistantConversation.openFor(customerId)));
    }

    private static AssistantMessageResponse toResponse(AssistantMessage message) {
        return new AssistantMessageResponse(
                message.getId(), message.getRole(), message.getSource(), message.getContent(),
                message.getCreatedAt().atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime());
    }

}
