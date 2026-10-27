/** Shared event names for the SSE v1 contract and EventSource client. */
export const ASSISTANT_EVENT_TYPES = [
  'connected',
  'message.created',
  'unread-count.changed',
  'reply.started',
  'reply.delta',
  'reply.completed',
  'reply.failed',
  'reply.cancelled',
] as const;

export type AssistantEventType = typeof ASSISTANT_EVENT_TYPES[number];

/** The existing REST message shape. createdAt is an offset-free Asia/Seoul timestamp. */
export interface StoredAssistantMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  source: string;
  content: string;
  createdAt: string;
}

export interface AssistantEventData {
  connected: { connectionId: string; unreadCount: number };
  'message.created': { message: StoredAssistantMessage };
  'unread-count.changed': { unreadCount: number };
  'reply.started': { userMessageId: string };
  'reply.delta': { sequence: number; delta: string };
  'reply.completed': {
    exchange: { userMessage: StoredAssistantMessage; assistantMessage: StoredAssistantMessage };
    unreadCount: number;
  };
  'reply.failed': { code: string; message: string; retryable: boolean };
  'reply.cancelled': { reason: string };
}

type Correlation<T extends AssistantEventType> =
  T extends `reply.${string}` ? { requestId: string; messageId: string }
    : T extends 'message.created' ? { requestId: string | null; messageId: string }
      : { requestId: null; messageId: null };

export type AssistantEvent<T extends AssistantEventType = AssistantEventType> = {
  [K in T]: {
    version: 1;
    eventId: string;
    type: K;
    /** RFC 3339 UTC timestamp, including the Z suffix. */
    occurredAt: string;
    conversationId: string;
    data: AssistantEventData[K];
  } & Correlation<K>
}[T];
