import { describe, expect, it } from 'vitest';
import { ASSISTANT_EVENT_TYPES } from './assistantEvents';
import examples from '../../../../docs/examples/assistant-events-v1.json';

// The Java serialization test uses this same fixture to prevent contract drift.

describe('assistant SSE v1 wire contract', () => {
  it('covers every event name and keeps request/message correlation explicit', () => {
    expect(examples.map((event) => event.type)).toEqual([...ASSISTANT_EVENT_TYPES]);
    expect(new Set(examples.map((event) => event.eventId)).size).toBe(examples.length);
    for (const event of examples) {
      expect(event.version).toBe(1);
      expect(event.occurredAt).toMatch(/Z$/);
      if (event.type.startsWith('reply.')) {
        expect(event.requestId).toMatch(/^[0-9a-f-]{36}$/);
        expect(event.messageId).toBeTruthy();
      } else if (event.type !== 'message.created') {
        expect(event.requestId).toBeNull();
        expect(event.messageId).toBeNull();
      }
    }
  });

  it('uses the same saved message ID for the final answer and keeps delta whitespace', () => {
    const started = examples.find((event) => event.type === 'reply.started');
    const completed = examples.find((event) => event.type === 'reply.completed');
    expect(completed.requestId).toBe(started.requestId);
    expect(completed.messageId).toBe(started.messageId);
    expect(completed.data.exchange.assistantMessage.id).toBe(completed.messageId);
    expect(completed.data.exchange.userMessage.id).toBe(started.data.userMessageId);
    expect(examples.find((event) => event.type === 'reply.delta').data.delta).toBe('안녕하세요 🌱\n ');
  });
});
