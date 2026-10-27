import { useCallback, useEffect, useRef, useState } from 'react';
import { mergeChatMessages, toChatMessage, wonnieApi } from '../services/wonnieApi.js';
import { ASSISTANT_EVENT_TYPES } from '../services/assistantEvents.ts';

const TYPEWRITER_INTERVAL_MS = 16;

const newRequestId = () => globalThis.crypto?.randomUUID?.()
  || 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (letter) => {
    const random = Math.floor(Math.random() * 16);
    return (letter === 'x' ? random : (random & 0x3) | 0x8).toString(16);
  });

export default function useWonnieBackend(open) {
  const [messages, setMessages] = useState([]);
  const [pendingMessage, setPendingMessage] = useState(null);
  const [pendingAssistantMessage, setPendingAssistantMessage] = useState(null);
  const [sending, setSending] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [error, setError] = useState(null);
  const [restoreInput, setRestoreInput] = useState(null);
  const sendingRef = useRef(false);
  const mounted = useRef(true);
  const openRef = useRef(open);
  const activeRequest = useRef(null);
  const seenEventIds = useRef(new Set());
  const eventRevision = useRef(0);
  const refreshSequence = useRef(0);
  openRef.current = open;

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  const refresh = useCallback(async (signal) => {
    const sequence = ++refreshSequence.current;
    const eventRevisionAtStart = eventRevision.current;
    const conversation = await wonnieApi.conversation(signal);
    if (!mounted.current || signal?.aborted || sequence !== refreshSequence.current) return conversation;
    if (!Array.isArray(conversation.messages)) throw new Error('저장된 대화의 응답 형식을 확인해 주세요.');
    // Validate/convert outside React's state updater so malformed responses do not poison chat state.
    const incoming = conversation.messages.map(toChatMessage);
    setMessages((current) => mergeChatMessages(current, incoming));
    if (eventRevision.current === eventRevisionAtStart) setUnreadCount(conversation.unreadCount);
    setError(null);
    if (openRef.current) {
      // Only mark data read after a successful history load, and keep read errors separate.
      try {
        const result = await wonnieApi.markRead(signal);
        if (mounted.current && !signal?.aborted && sequence === refreshSequence.current
            && eventRevision.current === eventRevisionAtStart) setUnreadCount(result.unreadCount);
      } catch { /* Retain the unread count and retry on the next refresh. */ }
    }
    return conversation;
  }, []);

  const settleRequest = useCallback((requestId) => {
    const job = activeRequest.current;
    if (!job || job.requestId !== requestId) return false;
    activeRequest.current = null;
    sendingRef.current = false;
    if (mounted.current) {
      setSending(false);
      setPendingMessage(null);
      setPendingAssistantMessage(null);
    }
    return true;
  }, []);

  const failRequest = useCallback((job, message) => {
    if (!job || activeRequest.current !== job) return;
    activeRequest.current = null;
    sendingRef.current = false;
    if (mounted.current) {
      setSending(false);
      setPendingMessage(null);
      setPendingAssistantMessage(null);
      setRestoreInput(job.message);
      setError(message);
    }
  }, []);

  const applyCompletedExchange = useCallback((requestId, exchange, nextUnreadCount) => {
    if (!exchange?.userMessage || !exchange?.assistantMessage) return false;
    try {
      const incoming = [toChatMessage(exchange.userMessage), toChatMessage(exchange.assistantMessage)];
      setMessages((current) => mergeChatMessages(current, incoming));
    } catch {
      return false;
    }
    if (Number.isInteger(nextUnreadCount) && nextUnreadCount >= 0) setUnreadCount(nextUnreadCount);
    settleRequest(requestId);
    // Reconcile notices and read state. A failed refresh must not retry a completed message.
    void refresh().catch(() => {});
    return true;
  }, [refresh, settleRequest]);

  const recoverRequest = useCallback(async (requestId) => {
    const job = activeRequest.current;
    if (!job || job.requestId !== requestId) return;
    try {
      const status = await wonnieApi.assistantRequestStatus(requestId);
      if (activeRequest.current !== job) return;
      job.userMessageId = status.userMessageId || job.userMessageId;
      job.assistantMessageId = status.assistantMessageId || job.assistantMessageId;
      if (Number.isInteger(status.lastSequence) && status.lastSequence >= job.lastSequence) {
        job.lastSequence = status.lastSequence;
        const partialAnswer = typeof status.partialAnswer === 'string' ? status.partialAnswer : '';
        if (['RUNNING', 'CANCELLING', 'COMPLETING'].includes(status.status)) {
          job.typingQueue = [];
          job.renderedAnswer = partialAnswer;
          setPendingAssistantMessage({
            id: `pending-${requestId}-assistant`, requestId, from: 'assistant', kind: 'text',
            text: partialAnswer, createdAt: job.createdAt + 1, time: job.time,
          });
        }
      }
      if (status.status === 'COMPLETED') {
        if (!applyCompletedExchange(requestId, status.exchange)) {
          void refresh().catch(() => {}).finally(() => settleRequest(requestId));
        }
      } else if (status.status === 'FAILED') {
        failRequest(job, '답변을 생성하지 못했어요. 질문을 확인한 뒤 다시 시도해 주세요.');
      } else if (status.status === 'CANCELLED') {
        failRequest(job, '답변 생성을 취소했어요. 질문을 입력창에서 다시 확인해 주세요.');
      }
    } catch (statusError) {
      if (statusError?.status === 404 && activeRequest.current === job) {
        // The process-local job registry may have been lost during a server restart.
        void refresh().catch(() => {}).finally(() => {
          failRequest(job, '요청 상태를 복구하지 못했어요. 질문을 확인한 뒤 다시 시도해 주세요.');
        });
      }
    }
  }, [applyCompletedExchange, failRequest, refresh, settleRequest]);

  useEffect(() => {
    const controller = new AbortController();
    let source = null;
    let fallbackRequested = false;
    let typingTimer = null;

    const finishBufferedReply = (job) => {
      const completion = job.pendingCompletion;
      if (!completion || activeRequest.current !== job) return;
      job.pendingCompletion = null;
      if (!applyCompletedExchange(job.requestId, completion.exchange, completion.unreadCount)) {
        void refresh().catch(() => {}).finally(() => settleRequest(job.requestId));
      }
    };

    const renderNextCharacter = (job) => {
      typingTimer = null;
      if (controller.signal.aborted || activeRequest.current !== job) return;
      const nextCharacter = job.typingQueue?.shift();
      if (nextCharacter !== undefined) {
        job.renderedAnswer = (job.renderedAnswer || '') + nextCharacter;
        setPendingAssistantMessage({
          id: 'pending-' + job.requestId + '-assistant', requestId: job.requestId,
          from: 'assistant', kind: 'text', text: job.renderedAnswer,
          createdAt: job.createdAt + 1, time: job.time,
        });
      }
      if (job.typingQueue?.length) {
        typingTimer = setTimeout(() => renderNextCharacter(job), TYPEWRITER_INTERVAL_MS);
      } else {
        finishBufferedReply(job);
      }
    };

    const enqueueTypedDelta = (job, delta) => {
      if (!Array.isArray(job.typingQueue)) job.typingQueue = [];
      job.typingQueue.push(...Array.from(delta));
      if (typingTimer === null) {
        typingTimer = setTimeout(() => renderNextCharacter(job), TYPEWRITER_INTERVAL_MS);
      }
    };

    const recover = async () => {
      if (controller.signal.aborted) return;
      try {
        await refresh(controller.signal);
      } catch (syncError) {
        if (!controller.signal.aborted && openRef.current) setError(syncError.message);
      }
      const job = activeRequest.current;
      if (!controller.signal.aborted && job) void recoverRequest(job.requestId);
    };

    const loadUnreadSnapshot = async () => {
      if (controller.signal.aborted) return;
      const eventRevisionAtStart = eventRevision.current;
      try {
        const result = await wonnieApi.unreadCount(controller.signal);
        if (mounted.current && !controller.signal.aborted
            && eventRevision.current === eventRevisionAtStart) setUnreadCount(result.unreadCount);
      } catch { /* EventSource reconnects automatically; chat open/focus also retries via REST. */ }
    };
    const loadUnreadFallback = () => {
      if (fallbackRequested) return;
      fallbackRequested = true;
      return loadUnreadSnapshot();
    };

    const syncOnFocus = () => {
      if (openRef.current) void recover();
      else {
        void loadUnreadSnapshot();
        const job = activeRequest.current;
        if (job) void recoverRequest(job.requestId);
      }
    };
    window.addEventListener('focus', syncOnFocus);

    const EventSourceImpl = window.EventSource;
    if (typeof EventSourceImpl !== 'function') {
      void loadUnreadFallback();
      const job = activeRequest.current;
      if (job) void recoverRequest(job.requestId);
      return () => {
        controller.abort();
        window.removeEventListener('focus', syncOnFocus);
      };
    }

    try {
      source = new EventSourceImpl('/api/assistant/events', { withCredentials: true });
    } catch {
      void loadUnreadFallback();
      const job = activeRequest.current;
      if (job) void recoverRequest(job.requestId);
      return () => {
        controller.abort();
        window.removeEventListener('focus', syncOnFocus);
      };
    }

    const listen = (type, handler) => {
      const listener = (rawEvent) => {
        let envelope;
        try {
          envelope = JSON.parse(rawEvent.data);
          if (envelope.version !== 1 || envelope.type !== type
              || !ASSISTANT_EVENT_TYPES.includes(type) || typeof envelope.eventId !== 'string'
              || !envelope.data || typeof envelope.conversationId !== 'string'
              || (type.startsWith('reply.') && (typeof envelope.requestId !== 'string' || typeof envelope.messageId !== 'string'))) {
            throw new Error('SSE 이벤트 형식이 올바르지 않아요.');
          }
        } catch {
          void recover();
          return;
        }
        if (seenEventIds.current.has(envelope.eventId)) return;
        seenEventIds.current.add(envelope.eventId);
        eventRevision.current += 1;
        if (seenEventIds.current.size > 1024) {
          seenEventIds.current.delete(seenEventIds.current.values().next().value);
        }
        handler(envelope);
      };
      source.addEventListener(type, listener);
    };

    listen('connected', (envelope) => {
      const count = envelope.data.unreadCount;
      if (Number.isInteger(count) && count >= 0) setUnreadCount(count);
      void recover(); // Reload history and any in-flight reply after initial connect/reconnect.
    });

    listen('message.created', (envelope) => {
      const message = envelope.data.message;
      if (!message || typeof message.id !== 'string' || typeof message.content !== 'string'
          || typeof message.createdAt !== 'string' || !['USER', 'ASSISTANT'].includes(message.role)) {
        void recover();
        return;
      }
      let incoming;
      try {
        incoming = toChatMessage(message);
      } catch {
        void recover();
        return;
      }
      setMessages((current) => mergeChatMessages(current, [incoming]));
      if (openRef.current) void recover(); // Refresh also marks newly received notices read.
    });

    listen('unread-count.changed', (envelope) => {
      const count = envelope.data.unreadCount;
      if (Number.isInteger(count) && count >= 0) setUnreadCount(count);
      else void recover();
    });

    listen('reply.started', (envelope) => {
      const job = activeRequest.current;
      if (!job || job.requestId !== envelope.requestId) return;
      job.userMessageId = envelope.data.userMessageId;
      job.assistantMessageId = envelope.messageId;
      setPendingAssistantMessage((current) => current?.requestId === job.requestId ? current : {
        id: `pending-${job.requestId}-assistant`, requestId: job.requestId, from: 'assistant', kind: 'text',
        text: '', createdAt: job.createdAt + 1, time: job.time,
      });
    });

    listen('reply.delta', (envelope) => {
      const job = activeRequest.current;
      const { sequence, delta } = envelope.data;
      if (!job || job.requestId !== envelope.requestId) return;
      if (!Number.isInteger(sequence) || sequence < 1 || typeof delta !== 'string' || !delta) {
        void recoverRequest(job.requestId);
        return;
      }
      if (job.assistantMessageId && job.assistantMessageId !== envelope.messageId) {
        void recoverRequest(job.requestId);
        return;
      }
      job.assistantMessageId = envelope.messageId;
      if (sequence <= job.lastSequence) return;
      if (sequence !== job.lastSequence + 1) {
        void recoverRequest(job.requestId);
        return;
      }
      job.lastSequence = sequence;
      enqueueTypedDelta(job, delta);
    });

    listen('reply.completed', (envelope) => {
      const exchange = envelope.data.exchange;
      const job = activeRequest.current;
      if (job?.requestId === envelope.requestId && (job.typingQueue?.length || typingTimer !== null)) {
        job.pendingCompletion = { exchange, unreadCount: envelope.data.unreadCount };
      } else if (!applyCompletedExchange(envelope.requestId, exchange, envelope.data.unreadCount)) {
        if (job?.requestId === envelope.requestId) void recoverRequest(job.requestId);
        else void recover();
      }
    });

    listen('reply.failed', (envelope) => {
      const job = activeRequest.current;
      if (job?.requestId === envelope.requestId) {
        failRequest(job, typeof envelope.data.message === 'string'
          ? envelope.data.message : '답변을 생성하지 못했어요. 다시 시도해 주세요.');
      }
    });

    listen('reply.cancelled', (envelope) => {
      const job = activeRequest.current;
      if (job?.requestId === envelope.requestId) {
        failRequest(job, '답변 생성을 취소했어요. 질문을 입력창에서 다시 확인해 주세요.');
      }
    });

    source.onerror = () => {
      void loadUnreadFallback();
      const job = activeRequest.current;
      if (job) void recoverRequest(job.requestId);
    };

    return () => {
      controller.abort();
      if (typingTimer !== null) clearTimeout(typingTimer);
      source?.close();
      window.removeEventListener('focus', syncOnFocus);
    };
  }, [applyCompletedExchange, failRequest, recoverRequest, refresh]);

  useEffect(() => {
    if (!open) return undefined;
    const controller = new AbortController();
    void refresh(controller.signal).catch((refreshError) => {
      if (!controller.signal.aborted && mounted.current) setError(refreshError.message);
    });
    const job = activeRequest.current;
    if (job) void recoverRequest(job.requestId);
    return () => controller.abort();
  }, [open, recoverRequest, refresh]);

  const send = useCallback(async (text, topic) => {
    if (sendingRef.current) return false;
    const requestId = newRequestId();
    const job = {
      requestId,
      message: text,
      topic,
      lastSequence: 0,
      createdAt: Date.now(),
      time: new Intl.DateTimeFormat('ko-KR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date()),
      userMessageId: null,
      assistantMessageId: null,
    };
    activeRequest.current = job;
    sendingRef.current = true;
    setSending(true);
    setError(null);
    setRestoreInput(null);
    setPendingMessage({ id: `pending-${requestId}-user`, requestId, from: 'user', kind: 'text', text,
      createdAt: job.createdAt, time: job.time });
    setPendingAssistantMessage(null);
    try {
      const accepted = await wonnieApi.submitAssistantRequest(requestId, text, topic);
      if (!accepted || accepted.requestId !== requestId || typeof accepted.userMessageId !== 'string'
          || typeof accepted.assistantMessageId !== 'string') {
        throw new Error('상담 요청 접수 응답 형식을 확인해 주세요.');
      }
      if (activeRequest.current === job) {
        job.userMessageId = accepted.userMessageId;
        job.assistantMessageId = accepted.assistantMessageId;
        if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(accepted.status)) void recoverRequest(requestId);
      }
      return true;
    } catch (sendError) {
      if (activeRequest.current === job) {
        activeRequest.current = null;
        sendingRef.current = false;
        if (mounted.current) {
          setSending(false);
          setPendingMessage(null);
          setPendingAssistantMessage(null);
          setRestoreInput(text);
          setError(sendError.message);
        }
        throw sendError;
      }
      // A terminal SSE event can arrive before the POST acceptance response.
      return true;
    }
  }, [recoverRequest]);

  return {
    messages,
    pendingMessage,
    pendingAssistantMessage,
    sending,
    unreadCount,
    error,
    restoreInput,
    refresh,
    send,
  };
}
