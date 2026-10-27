package com.woori.wonnie.assistant;

import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Holds process-local SSE connections and delivers events to every active tab for a customer. */
@Component
public class AssistantSseConnectionManager {

    private static final long HEARTBEAT_SECONDS = 20;

    private final ConcurrentMap<String, ConcurrentMap<String, Connection>> connectionsByCustomer =
            new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(
            2, daemonThreadFactory());

    public SseEmitter connect(String customerId, String conversationId, long unreadCount) {
        var connectionId = UUID.randomUUID().toString();
        // 0 disables the MVC async timeout. Heartbeats keep intermediaries from treating the stream as idle.
        var emitter = new SseEmitter(0L);
        var connection = new Connection(customerId, conversationId, connectionId, emitter);

        connectionsByCustomer.compute(customerId, (key, current) -> {
            var customerConnections = current == null ? new ConcurrentHashMap<String, Connection>() : current;
            customerConnections.put(connectionId, connection);
            return customerConnections;
        });

        emitter.onCompletion(() -> unregister(connection));
        emitter.onTimeout(() -> unregister(connection));
        emitter.onError(error -> unregister(connection));

        var connected = AssistantStreamEvent.create(conversationId, null, null,
                new AssistantStreamEvent.Connected(connectionId, unreadCount));
        sendEvent(connection, connected);

        if (!connection.closed.get()) {
            try {
                var heartbeat = heartbeatExecutor.scheduleWithFixedDelay(
                        () -> sendHeartbeat(connection), HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
                connection.setHeartbeat(heartbeat);
            } catch (RuntimeException error) {
                fail(connection, error);
            }
        }
        return emitter;
    }

    /** Publishes one already-constructed event to all active connections for its customer. */
    public void publish(String customerId, AssistantStreamEvent event) {
        Objects.requireNonNull(event, "event");
        var customerConnections = connectionsByCustomer.get(customerId);
        if (customerConnections == null) return;

        for (var connection : customerConnections.values()) {
            if (connection.conversationId.equals(event.conversationId())) {
                sendEvent(connection, event);
            }
        }
    }

    private void sendEvent(Connection connection, AssistantStreamEvent event) {
        try {
            synchronized (connection.sendLock) {
                if (connection.closed.get()) return;
                connection.emitter.send(SseEmitter.event()
                        .id(event.eventId().toString())
                        .name(event.type().wireName())
                        .data(event, MediaType.APPLICATION_JSON));
            }
        } catch (IOException | RuntimeException error) {
            fail(connection, error);
        }
    }

    private void sendHeartbeat(Connection connection) {
        try {
            synchronized (connection.sendLock) {
                if (connection.closed.get()) return;
                connection.emitter.send(SseEmitter.event().comment("heartbeat"));
            }
        } catch (IOException | RuntimeException error) {
            fail(connection, error);
        }
    }

    private void fail(Connection connection, Throwable error) {
        unregister(connection);
        try {
            connection.emitter.completeWithError(error);
        } catch (RuntimeException ignored) {
            // The servlet container may have already completed the async response.
        }
    }

    private void unregister(Connection connection) {
        if (!connection.closed.compareAndSet(false, true)) return;
        connection.cancelHeartbeat();
        connectionsByCustomer.computeIfPresent(connection.customerId, (key, current) -> {
            current.remove(connection.connectionId, connection);
            return current.isEmpty() ? null : current;
        });
    }

    @PreDestroy
    void shutdown() {
        heartbeatExecutor.shutdownNow();
        connectionsByCustomer.values().stream()
                .flatMap(customerConnections -> customerConnections.values().stream())
                .toList()
                .forEach(connection -> {
                    unregister(connection);
                    try {
                        connection.emitter.complete();
                    } catch (IllegalStateException ignored) {
                        // The servlet container may already be closing the response.
                    }
                });
    }

    private static ThreadFactory daemonThreadFactory() {
        var threadNumber = new java.util.concurrent.atomic.AtomicInteger();
        return task -> {
            var thread = new Thread(task, "wonnie-sse-heartbeat-" + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static final class Connection {
        private final String customerId;
        private final String conversationId;
        private final String connectionId;
        private final SseEmitter emitter;
        private final Object sendLock = new Object();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicReference<ScheduledFuture<?>> heartbeatTask = new AtomicReference<>();

        private Connection(String customerId, String conversationId, String connectionId, SseEmitter emitter) {
            this.customerId = customerId;
            this.conversationId = conversationId;
            this.connectionId = connectionId;
            this.emitter = emitter;
        }

        private void setHeartbeat(ScheduledFuture<?> task) {
            if (!heartbeatTask.compareAndSet(null, task) || closed.get()) {
                task.cancel(false);
            }
        }

        private void cancelHeartbeat() {
            var task = heartbeatTask.getAndSet(null);
            if (task != null) task.cancel(false);
        }
    }
}
