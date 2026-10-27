package com.woori.wonnie.assistant;

import com.woori.wonnie.api.ApiDtos.AssistantConversationResponse;
import com.woori.wonnie.api.ApiDtos.AssistantRequestAcceptedResponse;
import com.woori.wonnie.api.ApiDtos.AssistantRequestStatusResponse;
import com.woori.wonnie.api.ApiDtos.AssistantMessageExchange;
import com.woori.wonnie.api.ApiDtos.AssistantMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

@Service
public class AssistantService {
    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final AssistantConversationRepository conversationRepository;
    private final AssistantMessageRepository messageRepository;
    private final AssistantResponder responder;
    private final AssistantSseConnectionManager sseConnectionManager;
    private final AssistantRequestRegistry requestRegistry;
    private final Executor replyExecutor;
    private final org.springframework.transaction.support.TransactionTemplate transactions;

    public AssistantService(
            AssistantConversationRepository conversationRepository,
            AssistantMessageRepository messageRepository,
            AssistantResponder responder,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            AssistantSseConnectionManager sseConnectionManager,
            AssistantRequestRegistry requestRegistry,
            @Qualifier("assistantReplyExecutor") Executor replyExecutor
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.responder = responder;
        this.sseConnectionManager = sseConnectionManager;
        this.requestRegistry = requestRegistry;
        this.replyExecutor = replyExecutor;
        this.transactions = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
    }

    @Transactional
    public ConnectionSnapshot connectionSnapshot(String customerId) {
        var conversation = getOrCreateConversation(customerId);
        var unreadCount = messageRepository.countByConversationIdAndRoleAndReadAtIsNull(
                conversation.getId(), "ASSISTANT");
        return new ConnectionSnapshot(conversation.getId(), unreadCount);
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

    public RequestSubmission submit(String customerId, UUID requestId, String message, String topic) {
        if (requestId == null) throw new IllegalArgumentException("요청 ID가 필요해요.");
        if (message == null || message.isBlank() || message.length() > 4000) {
            throw new IllegalArgumentException("메시지는 1~4000자로 입력해 주세요.");
        }
        if (topic != null && topic.length() > 200) {
            throw new IllegalArgumentException("상담 주제는 200자 이내로 입력해 주세요.");
        }

        var context = transactions.execute(status -> {
            var conversation = getOrCreateConversation(customerId);
            var recent = new java.util.ArrayList<>(messageRepository
                    .findTop20ByConversationIdOrderByCreatedAtDesc(conversation.getId()));
            java.util.Collections.reverse(recent);
            return new SubmissionContext(conversation.getId(), recent);
        });
        var registration = requestRegistry.register(customerId, requestId, message, topic,
                context.conversationId(), context.history());
        var job = registration.job();
        if (registration.created()) {
            var task = new FutureTask<Void>(() -> {
                runReplyJob(job);
                return null;
            });
            requestRegistry.attachFuture(job, task);
            try {
                replyExecutor.execute(task);
            } catch (RejectedExecutionException exception) {
                requestRegistry.remove(job);
                throw new AssistantUnavailableException("REPLY_QUEUE", exception);
            }
        }
        var response = new AssistantRequestAcceptedResponse(
                requestId, job.conversationId(), job.userMessageId(), job.assistantMessageId(),
                requestRegistry.snapshot(job).state().name());
        return new RequestSubmission(response, registration.created());
    }

    public AssistantRequestStatusResponse requestStatus(String customerId, UUID requestId) {
        var job = requestRegistry.find(customerId, requestId);
        if (job == null) throw new AssistantRequestNotFoundException();
        var snapshot = requestRegistry.snapshot(job);
        AssistantMessageExchange exchange = null;
        if (snapshot.state() == AssistantRequestRegistry.State.COMPLETED) {
            exchange = transactions.execute(status -> {
                var userMessage = messageRepository.findById(job.userMessageId());
                var assistantMessage = messageRepository.findById(job.assistantMessageId());
                if (userMessage.isEmpty() || assistantMessage.isEmpty()) return null;
                return new AssistantMessageExchange(
                        toResponse(userMessage.get()), toResponse(assistantMessage.get()));
            });
        }
        return new AssistantRequestStatusResponse(
                requestId, job.conversationId(), job.userMessageId(), job.assistantMessageId(),
                snapshot.state().name(), snapshot.lastSequence(), snapshot.partialAnswer(),
                snapshot.failureCode(), exchange);
    }

    public AssistantRequestStatusResponse cancelRequest(String customerId, UUID requestId) {
        var job = requestRegistry.find(customerId, requestId);
        if (job == null) throw new AssistantRequestNotFoundException();
        var cancellation = requestRegistry.requestCancellation(job);
        if (cancellation.shouldCancelTask() && cancellation.future() != null) {
            cancellation.future().cancel(cancellation.state() == AssistantRequestRegistry.State.CANCELLING);
        }
        if (cancellation.confirmed()) {
            publishReplyEvent(job, job.assistantMessageId(),
                    new AssistantStreamEvent.ReplyCancelled("USER_REQUESTED"));
        }
        return requestStatus(customerId, requestId);
    }

    private void runReplyJob(AssistantRequestRegistry.Job job) {
        if (!requestRegistry.start(job)) return;
        try {
            publishReplyEvent(job, job.assistantMessageId(),
                    new AssistantStreamEvent.ReplyStarted(job.userMessageId()));
            if (requestRegistry.snapshot(job).state() == AssistantRequestRegistry.State.CANCELLING) {
                publishCancellationIfConfirmed(job);
                return;
            }
            var prompt = job.topic() == null || job.topic().isBlank()
                    ? job.message().strip()
                    : "화면에서 선택한 상담 주제: " + job.topic().strip() + "\n사용자 질문: " + job.message().strip();
            var reply = responder.respondStreaming(job.customerId(), prompt, job.history(), delta -> {
                if (delta == null || delta.isEmpty()) return;
                var nextSequence = requestRegistry.appendDelta(job, delta);
                if (nextSequence < 0) return;
                publishReplyEvent(job, job.assistantMessageId(),
                        new AssistantStreamEvent.ReplyDelta(nextSequence, delta));
            });
            if (!requestRegistry.beginCompletion(job)) {
                publishCancellationIfConfirmed(job);
                return;
            }
            if (reply == null || reply.isBlank() || reply.length() > 4000) {
                throw new IllegalStateException("상담 응답 형식 오류");
            }

            var completion = transactions.execute(status -> {
                var conversation = getOrCreateConversation(job.customerId());
                var userMessage = messageRepository.save(AssistantMessage.userMessage(
                        job.userMessageId(), conversation.getId(), job.customerId(), job.message().strip()));
                var assistantMessage = messageRepository.save(AssistantMessage.assistantReply(
                        job.assistantMessageId(), conversation.getId(), job.customerId(), reply,
                        userMessage.getCreatedAt().plusNanos(1_000)));
                conversation.touch();
                var exchange = new AssistantMessageExchange(toResponse(userMessage), toResponse(assistantMessage));
                var unreadCount = messageRepository.countByConversationIdAndRoleAndReadAtIsNull(
                        conversation.getId(), "ASSISTANT");
                return new CompletedReply(exchange, unreadCount);
            });
            requestRegistry.complete(job);
            publishReplyEvent(job, job.assistantMessageId(),
                    new AssistantStreamEvent.ReplyCompleted(completion.exchange(), completion.unreadCount()));
        } catch (RuntimeException exception) {
            var code = exception instanceof AssistantUnavailableException
                    ? "ASSISTANT_UNAVAILABLE" : "ASSISTANT_REPLY_FAILED";
            if (requestRegistry.confirmCancellation(job)) {
                publishReplyEvent(job, job.assistantMessageId(),
                        new AssistantStreamEvent.ReplyCancelled("USER_REQUESTED"));
                return;
            }
            requestRegistry.fail(job, code);
            log.warn("Assistant reply failed. requestId={} stage={}", job.requestId(),
                    exception instanceof AssistantUnavailableException unavailable ? unavailable.getStage() : "PERSISTENCE_OR_STREAM");
            publishReplyEvent(job, job.assistantMessageId(), new AssistantStreamEvent.ReplyFailed(
                    code, "답변을 생성하지 못했어요. 잠시 후 다시 시도해 주세요.", true));
        }
    }

    private void publishCancellationIfConfirmed(AssistantRequestRegistry.Job job) {
        if (requestRegistry.confirmCancellation(job)) {
            publishReplyEvent(job, job.assistantMessageId(),
                    new AssistantStreamEvent.ReplyCancelled("USER_REQUESTED"));
        }
    }

    private void publishReplyEvent(
            AssistantRequestRegistry.Job job,
            String messageId,
            AssistantStreamEvent.Data data
    ) {
        try {
            sseConnectionManager.publish(job.customerId(), AssistantStreamEvent.create(
                    job.conversationId(), job.requestId(), messageId, data));
        } catch (RuntimeException error) {
            // Request execution and persistence continue if an SSE client disconnects.
            log.debug("Assistant reply SSE delivery failed. requestId={}", job.requestId());
        }
    }

    @Transactional
    public void markRead(String customerId) {
        conversationRepository.findByCustomerId(customerId).ifPresent(conversation -> {
            var updatedCount = messageRepository.markAssistantMessagesRead(
                    conversation.getId(), LocalDateTime.now(java.time.ZoneOffset.UTC));
            if (updatedCount > 0) {
                var unreadCount = messageRepository.countByConversationIdAndRoleAndReadAtIsNull(
                        conversation.getId(), "ASSISTANT");
                var event = AssistantStreamEvent.create(conversation.getId(), null, null,
                        new AssistantStreamEvent.UnreadCountChanged(unreadCount));
                publishAfterCommit(customerId, List.of(event));
            }
        });
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
        var message = messageRepository.save(
                AssistantMessage.priceIncrease(conversation.getId(), customerId, content, sourceKey));
        conversation.touch();
        publishNoticeAfterCommit(customerId, conversation.getId(), message);
    }

    @Transactional
    public void addFinanceNotice(String customerId, String content, String sourceKey) {
        if (messageRepository.existsBySourceKey(sourceKey)) return;
        var conversation = getOrCreateConversation(customerId);
        var message = messageRepository.save(
                AssistantMessage.financeNotice(conversation.getId(), customerId, content, sourceKey));
        conversation.touch();
        publishNoticeAfterCommit(customerId, conversation.getId(), message);
    }

    @Transactional
    public void addCalendarPauseNotice(String customerId, String content, String metadata, String sourceKey) {
        if (messageRepository.existsBySourceKey(sourceKey)) return;
        var conversation = getOrCreateConversation(customerId);
        var message = messageRepository.save(AssistantMessage.calendarPauseRecommendation(
                conversation.getId(), customerId, content, metadata, sourceKey));
        conversation.touch();
        publishNoticeAfterCommit(customerId, conversation.getId(), message);
    }

    private void publishNoticeAfterCommit(String customerId, String conversationId, AssistantMessage message) {
        var messageResponse = toResponse(message);
        var unreadCount = messageRepository.countByConversationIdAndRoleAndReadAtIsNull(
                conversationId, "ASSISTANT");
        var events = List.of(
                AssistantStreamEvent.create(conversationId, null, messageResponse.id(),
                        new AssistantStreamEvent.MessageCreated(messageResponse)),
                AssistantStreamEvent.create(conversationId, null, null,
                        new AssistantStreamEvent.UnreadCountChanged(unreadCount)));
        publishAfterCommit(customerId, events);
    }

    private void publishAfterCommit(String customerId, List<AssistantStreamEvent> events) {
        if (events.isEmpty()) return;
        Runnable publish = () -> events.forEach(event -> {
            try {
                sseConnectionManager.publish(customerId, event);
            } catch (RuntimeException error) {
                // The database commit is final; a delivery problem must not turn it into a failed request.
                log.warn("Failed to publish assistant SSE event after commit. eventId={}", event.eventId());
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }

    private AssistantConversation getOrCreateConversation(String customerId) {
        return conversationRepository.findByCustomerId(customerId)
                .orElseGet(() -> conversationRepository.save(AssistantConversation.openFor(customerId)));
    }

    private static AssistantMessageResponse toResponse(AssistantMessage message) {
        return new AssistantMessageResponse(
                message.getId(), message.getRole(), message.getSource(), message.getContent(), message.getMetadata(),
                message.getCreatedAt().atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul")).toLocalDateTime());
    }

    public record ConnectionSnapshot(String conversationId, long unreadCount) {}

    private record SubmissionContext(String conversationId, List<AssistantMessage> history) {}

    private record CompletedReply(AssistantMessageExchange exchange, long unreadCount) {}

    public record RequestSubmission(AssistantRequestAcceptedResponse response, boolean created) {}

}
