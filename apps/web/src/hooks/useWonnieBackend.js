import { useCallback, useEffect, useRef, useState } from 'react';
import { mergeChatMessages, toChatMessage, wonnieApi } from '../services/wonnieApi.js';

export default function useWonnieBackend(open) {
  const [messages, setMessages] = useState([]);
  const [pendingMessage, setPendingMessage] = useState(null);
  const [sending, setSending] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [error, setError] = useState(null);
  const sendingRef = useRef(false);
  const mounted = useRef(true);
  const openRef = useRef(open);
  openRef.current = open;

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  const refresh = useCallback(async (signal) => {
    const conversation = await wonnieApi.conversation(signal);
    if (!mounted.current || signal?.aborted) return;
    if (!Array.isArray(conversation.messages)) throw new Error('저장된 대화의 응답 형식을 확인해 주세요.');
    // Validate/convert outside React's state updater so polling catches malformed responses.
    const incoming = conversation.messages.map(toChatMessage);
    setMessages((current) => mergeChatMessages(current, incoming));
    setUnreadCount(conversation.unreadCount);
    setError(null);
    if (openRef.current) {
      // Only mark data read after a successful history load, and keep read errors separate.
      try {
        const result = await wonnieApi.markRead();
        if (mounted.current) setUnreadCount(result.unreadCount);
      } catch { /* Retain the unread count and retry on the next refresh. */ }
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let loading = false;
    const poll = async () => {
      if (loading || controller.signal.aborted) return;
      loading = true;
      try {
        if (openRef.current) await refresh(controller.signal);
        else {
          const result = await wonnieApi.unreadCount(controller.signal);
          if (!controller.signal.aborted) setUnreadCount(result.unreadCount);
        }
      } catch (pollError) {
        if (!controller.signal.aborted && openRef.current) setError(pollError.message);
      } finally { loading = false; }
    };
    void poll();
    const timer = setInterval(poll, 15000);
    window.addEventListener('focus', poll);
    return () => {
      controller.abort();
      clearInterval(timer);
      window.removeEventListener('focus', poll);
    };
  }, [open, refresh]);

  const send = useCallback(async (text, topic) => {
    if (sendingRef.current) return false;
    sendingRef.current = true;
    setSending(true);
    setError(null);
    setPendingMessage({ id: 'pending-user-message', from: 'user', kind: 'text', text,
      createdAt: Date.now(), time: new Intl.DateTimeFormat('ko-KR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date()) });
    try {
      const exchange = await wonnieApi.sendMessage(text, topic);
      if (!mounted.current) return true;
      const incoming = [toChatMessage(exchange.userMessage), toChatMessage(exchange.assistantMessage)];
      setMessages((current) => mergeChatMessages(current, incoming));
      // Synchronize proactive messages too. A failed refresh does not turn a successful send into a retry.
      void refresh().catch(() => {});
      return true;
    } catch (sendError) {
      if (mounted.current) setError(sendError.message);
      throw sendError;
    } finally {
      sendingRef.current = false;
      if (mounted.current) { setSending(false); setPendingMessage(null); }
    }
  }, [refresh]);

  return { messages, pendingMessage, sending, unreadCount, error, refresh, send };
}
