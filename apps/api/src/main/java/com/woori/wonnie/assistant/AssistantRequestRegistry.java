package com.woori.wonnie.assistant;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/** Process-local request deduplication and lifecycle state for active/recent reply jobs. */
@Component
public class AssistantRequestRegistry {

    private final ConcurrentMap<Key, Job> jobs = new ConcurrentHashMap<>();

    public Registration register(
            String customerId,
            UUID requestId,
            String message,
            String topic,
            String conversationId,
            List<AssistantMessage> history
    ) {
        var key = new Key(customerId, requestId);
        var candidate = new Job(customerId, requestId, fingerprint(message, topic), message, topic, conversationId,
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), List.copyOf(history));
        var existing = jobs.putIfAbsent(key, candidate);
        if (existing == null) return new Registration(candidate, true);
        if (!existing.sameInput(message, topic)) throw new AssistantRequestConflictException();
        return new Registration(existing, false);
    }

    public Job find(String customerId, UUID requestId) {
        return jobs.get(new Key(customerId, requestId));
    }

    public void remove(Job job) {
        jobs.remove(new Key(job.customerId(), job.requestId()), job);
    }

    public boolean attachFuture(Job job, Future<?> future) {
        synchronized (job) {
            job.future = future;
            return job.state.get() == State.ACCEPTED;
        }
    }

    public boolean start(Job job) {
        synchronized (job) {
            return job.state.compareAndSet(State.ACCEPTED, State.RUNNING);
        }
    }

    public long appendDelta(Job job, String delta) {
        synchronized (job) {
            if (job.state.get() != State.RUNNING) return -1;
            if (job.partialAnswer.length() + delta.length() > 4000) {
                throw new IllegalStateException("상담 응답 형식 오류");
            }
            job.partialAnswer.append(delta);
            return ++job.sequence;
        }
    }

    public Snapshot snapshot(Job job) {
        synchronized (job) {
            return new Snapshot(job.state.get(), job.sequence, job.partialAnswer.toString(), job.failureCode);
        }
    }

    public boolean beginCompletion(Job job) {
        synchronized (job) {
            return job.state.compareAndSet(State.RUNNING, State.COMPLETING);
        }
    }

    public void complete(Job job) {
        synchronized (job) {
            if (job.state.get() != State.COMPLETING) {
                throw new IllegalStateException("상담 요청이 완료 단계가 아니에요.");
            }
            job.state.set(State.COMPLETED);
            job.clearPayload();
            job.partialAnswer.setLength(0);
        }
    }

    public void fail(Job job, String failureCode) {
        synchronized (job) {
            if (isTerminal(job.state.get())) return;
            job.failureCode = failureCode;
            job.state.set(State.FAILED);
            job.clearPayload();
            job.partialAnswer.setLength(0);
        }
    }

    /** Atomically decides whether cancellation can still win against completion. */
    public Cancellation requestCancellation(Job job) {
        synchronized (job) {
            var state = job.state.get();
            if (state == State.ACCEPTED) {
                job.state.set(State.CANCELLED);
                job.clearPayload();
                job.partialAnswer.setLength(0);
                return new Cancellation(State.CANCELLED, job.future, true, true);
            }
            if (state == State.RUNNING) {
                job.state.set(State.CANCELLING);
                return new Cancellation(State.CANCELLING, job.future, true, false);
            }
            return new Cancellation(state, job.future, false, false);
        }
    }

    /** Called by the worker only after the responder has stopped producing output. */
    public boolean confirmCancellation(Job job) {
        synchronized (job) {
            if (job.state.get() != State.CANCELLING) return false;
            job.state.set(State.CANCELLED);
            job.clearPayload();
            job.partialAnswer.setLength(0);
            return true;
        }
    }

    private static boolean isTerminal(State state) {
        return state == State.COMPLETED || state == State.FAILED || state == State.CANCELLED;
    }

    public enum State { ACCEPTED, RUNNING, CANCELLING, COMPLETING, COMPLETED, FAILED, CANCELLED }

    public record Registration(Job job, boolean created) {}

    public record Snapshot(State state, long lastSequence, String partialAnswer, String failureCode) {}

    public record Cancellation(State state, Future<?> future, boolean shouldCancelTask, boolean confirmed) {}

    public static final class Job {
        private final String customerId;
        private final UUID requestId;
        private final String inputFingerprint;
        private volatile String message;
        private volatile String topic;
        private final String conversationId;
        private final String userMessageId;
        private final String assistantMessageId;
        private volatile List<AssistantMessage> history;
        private final AtomicReference<State> state = new AtomicReference<>(State.ACCEPTED);
        private final StringBuilder partialAnswer = new StringBuilder();
        private volatile Future<?> future;
        private volatile String failureCode;
        private long sequence;

        private Job(
                String customerId,
                UUID requestId,
                String inputFingerprint,
                String message,
                String topic,
                String conversationId,
                String userMessageId,
                String assistantMessageId,
                List<AssistantMessage> history
        ) {
            this.customerId = customerId;
            this.requestId = requestId;
            this.inputFingerprint = inputFingerprint;
            this.message = message;
            this.topic = topic;
            this.conversationId = conversationId;
            this.userMessageId = userMessageId;
            this.assistantMessageId = assistantMessageId;
            this.history = history;
        }

        public String customerId() { return customerId; }
        public UUID requestId() { return requestId; }
        public String message() { return message; }
        public String topic() { return topic; }
        public String conversationId() { return conversationId; }
        public String userMessageId() { return userMessageId; }
        public String assistantMessageId() { return assistantMessageId; }
        public List<AssistantMessage> history() { return history; }

        private boolean sameInput(String otherMessage, String otherTopic) {
            return inputFingerprint.equals(fingerprint(otherMessage, otherTopic));
        }

        private void clearPayload() {
            message = null;
            topic = null;
            history = List.of();
        }
    }

    private record Key(String customerId, UUID requestId) {}

    private static String fingerprint(String message, String topic) {
        var topicValue = topic == null ? "0" : "1" + topic;
        var input = message.length() + ":" + message + topicValue.length() + ":" + topicValue;
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
