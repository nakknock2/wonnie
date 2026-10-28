import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { StrictMode } from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import App from './App.jsx';

let history;
let failSend;
let unread;
let requestLog;
let requestStatuses;
let restoreEventSource;
let checkoutCardIssued;
let consentState;
const eventSources = [];

class FakeEventSource {
  constructor(url, options) {
    this.url = url;
    this.options = options;
    this.listeners = new Map();
    this.closed = false;
    eventSources.push(this);
  }
  addEventListener(type, listener) {
    const listeners = this.listeners.get(type) || [];
    listeners.push(listener);
    this.listeners.set(type, listeners);
  }
  emit(type, payload) {
    (this.listeners.get(type) || []).forEach((listener) => listener({ data: JSON.stringify(payload) }));
  }
  close() { this.closed = true; }
}

const stored = (id, role, content) => ({ id, role, content, source: role === 'USER' ? 'USER_INPUT' : 'ASSISTANT_REPLY', createdAt: '2026-10-27T13:00:00' });
const response = (payload, status = 200) => ({ ok: status < 400, status, json: async () => payload });
const enableFakeEventSource = () => {
  const previous = Object.getOwnPropertyDescriptor(window, 'EventSource');
  Object.defineProperty(window, 'EventSource', { configurable: true, writable: true, value: FakeEventSource });
  restoreEventSource = () => {
    if (previous) Object.defineProperty(window, 'EventSource', previous);
    else Reflect.deleteProperty(window, 'EventSource');
  };
};
const replyEnvelope = (eventId, type, requestId, messageId, data) => ({
  version: 1, eventId, type, occurredAt: '2026-10-27T08:00:00Z',
  conversationId: 'conversation', requestId, messageId, data,
});

beforeEach(() => {
  history = []; failSend = false; unread = 0; requestLog = []; requestStatuses = new Map(); restoreEventSource = null; eventSources.length = 0; checkoutCardIssued = false;
  consentState = { subscriptionDataAgreed: false, notifications: { decided: false, d1: false, priceChange: false } };
  Object.defineProperty(HTMLElement.prototype, 'scrollTo', { value: vi.fn(), configurable: true });
  vi.stubGlobal('fetch', vi.fn(async (path, options = {}) => {
    requestLog.push({ path, options });
    if (path === '/api/consents') return response(consentState);
    if (path === '/api/consents/subscription-data') {
      consentState = { ...consentState, subscriptionDataAgreed: true };
      return response(consentState);
    }
    if (path === '/api/consents/notifications') {
      const value = JSON.parse(options.body);
      consentState = { ...consentState, notifications: { decided: true, d1: value.d1, priceChange: value.priceChange } };
      return response(consentState);
    }
    if (path === '/api/assistant/unread-count') return response({ unreadCount: unread });
    if (path === '/api/assistant/conversation') return response({ conversationId: 'conversation', messages: history, unreadCount: unread });
    if (path === '/api/assistant/conversation/read') { unread = 0; return response({ unreadCount: 0 }); }
    if (path === '/api/assistant/requests') {
      if (failSend) return response({ message: 'MCP 상담 연결을 확인해 주세요.' }, 503);
      const { requestId } = JSON.parse(options.body);
      return response({ requestId, conversationId: 'conversation', userMessageId: 'user-new', assistantMessageId: 'reply-new', status: 'RUNNING' }, 202);
    }
    if (path.startsWith('/api/assistant/requests/')) {
      const requestId = path.split('/')[4];
      return response(requestStatuses.get(requestId) || { requestId, status: 'RUNNING', lastSequence: 0, partialAnswer: '' });
    }
    if (path === '/api/finance/subscriptions') return response({ dataSource: 'DATABASE', data: {
      projectedMonthlyCostAfterTrials: 77000, subscriptions: [
        { id: 'sub-server-one', name: '서버 구독 A', amount: 17000, monthlyEquivalent: 17000, billingCycle: '매월', nextPaymentDate: '2026-11-02' },
        { id: 'sub-server-two', name: '서버 구독 B', amount: 720000, monthlyEquivalent: 60000, billingCycle: '매년', nextPaymentDate: '2026-11-12' },
      ],
    } });
    if (path === '/api/dashboard') return response({ subscriptions: [{ id: 'sub-server-one', brandColor: '#111111', initial: 'A' }] });
    if (path.startsWith('/api/subscription-checkout/options')) {
      const selected = path.includes('plan=STANDARD') ? { code: 'STANDARD', name: '스탠다드', price: 13500, description: 'Full HD', recommended: true } : null;
      const card = { id: checkoutCardIssued ? 'card-issued' : 'prd-daily', productId: 'prd-daily', name: '카드의정석2 DAILY', issuer: '우리카드',
        owned: checkoutCardIssued, maskedNumber: checkoutCardIssued ? '****4321' : null, annualFee: 25000, discountRate: 50,
        potentialDiscount: 5000, estimatedDiscount: 5000, minimumSpend: 500000, previousMonthSpend: 0,
        benefitReady: true, benefitNote: '구독 할인', summary: '정기결제 50% 할인', imageKey: 'woori-daily' };
      return response({ serviceName: '넷플릭스', plans: [
        { code: 'AD_STANDARD', name: '광고형 스탠다드', price: 7000, description: '광고 포함', recommended: false },
        { code: 'STANDARD', name: '스탠다드', price: 13500, description: 'Full HD', recommended: true },
        { code: 'PREMIUM', name: '프리미엄', price: 17000, description: '4K', recommended: false },
      ], selectedPlan: selected, ownedCards: checkoutCardIssued ? [card] : [], recommendedCards: checkoutCardIssued ? [] : [card], mockTransaction: true });
    }
    if (path === '/api/subscription-checkout/cards') { checkoutCardIssued = true; return response({ cardId: 'card-issued', productId: 'prd-daily', name: '카드의정석2 DAILY', maskedNumber: '****4321', annualFee: 25000, newlyIssued: true }); }
    if (path === '/api/subscription-checkout/subscriptions') return response({
      subscriptionId: 'sub-checkout', serviceName: '넷플릭스', plan: { code: 'STANDARD', name: '스탠다드', price: 13500 },
      cardId: 'card-issued', cardName: '카드의정석2 DAILY', approvedAmount: 13500, listPrice: 13500, cardDiscount: 5000,
      discountedPrice: 8500, monthlySaved: 5000, approvedOn: '2026-10-27', status: 'APPROVED', created: true,
      recommendation: { name: '워니 절약 적금', type: 'DEPOSIT', monthlyAmount: 5000, yearlyAmount: 60000 },
      subscriptions: [{ id: 'sub-checkout', name: '넷플릭스', plan: '스탠다드', price: 13500, billingCycle: 'MONTHLY', nextPaymentDate: '2026-11-27', status: 'ACTIVE', color: '#e50914', letter: 'N', cardName: '카드의정석2 DAILY', maskedNumber: '****4321' }],
    });
    throw new Error(`Unexpected endpoint: ${path}`);
  }));
});
afterEach(() => {
  cleanup();
  restoreEventSource?.();
  vi.useRealTimers();
  vi.unstubAllGlobals();
  Reflect.deleteProperty(HTMLElement.prototype, 'scrollTo');
});

const openChat = async () => {
  fireEvent.click(screen.getByRole('button', { name: /AI 상담 챗봇 워니 열기|워니 주요 알림: 구독 관련 중요한 소식/ }));
  return screen.findByRole('dialog', { name: 'WONNIE와 대화중' });
};

const sendQuestion = async (chat, text) => {
  const input = within(chat).getByRole('textbox', { name: '메시지 입력' });
  fireEvent.change(input, { target: { value: text } });
  await act(async () => {
    fireEvent.click(within(chat).getByRole('button', { name: '보내기' }));
    await Promise.resolve();
  });
  const submissions = requestLog.filter((entry) => entry.path === '/api/assistant/requests');
  return JSON.parse(submissions.at(-1).options.body);
};

describe('merged final frontend backend connection', () => {
  it('shows one waiting bubble until a streamed reply contains visible text', async () => {
    enableFakeEventSource();
    render(<App />);
    const chat = await openChat();
    const conversation = within(chat).getByRole('region', { name: '대화 내용' });
    vi.useFakeTimers();
    const request = await sendQuestion(chat, '내 카드 혜택 확인해줘');
    const source = eventSources[0];
    const waitingLabel = 'WONNIE가 답변을 준비하고 있어요';

    act(() => source.emit('reply.started', replyEnvelope(
      'empty-started', 'reply.started', request.requestId, 'reply-new', { userMessageId: 'user-new' },
    )));
    expect(conversation.querySelectorAll('.wc-message.assistant')).toHaveLength(1);
    expect(within(conversation).getByLabelText(waitingLabel)).toBeInTheDocument();

    act(() => source.emit('reply.delta', replyEnvelope(
      'blank-delta', 'reply.delta', request.requestId, 'reply-new', { sequence: 1, delta: ' \n ' },
    )));
    await act(async () => { await vi.advanceTimersByTimeAsync(80); });
    expect(conversation.querySelectorAll('.wc-message.assistant')).toHaveLength(1);
    expect(within(conversation).getByLabelText(waitingLabel)).toBeInTheDocument();

    act(() => source.emit('reply.delta', replyEnvelope(
      'visible-delta', 'reply.delta', request.requestId, 'reply-new', { sequence: 2, delta: '확인한 구독 목록' },
    )));
    await act(async () => { await vi.advanceTimersByTimeAsync(240); });
    expect(within(conversation).getByText('확인한 구독 목록')).toBeInTheDocument();
    expect(conversation.querySelectorAll('.wc-message.assistant')).toHaveLength(1);
    expect(within(conversation).queryByLabelText(waitingLabel)).not.toBeInTheDocument();

    const userMessage = stored('user-new', 'USER', '내 카드 혜택 확인해줘');
    const assistantMessage = stored('reply-new', 'ASSISTANT', '확인한 구독 목록입니다.');
    history = [userMessage, assistantMessage];
    await act(async () => source.emit('reply.completed', replyEnvelope(
      'empty-completed', 'reply.completed', request.requestId, 'reply-new',
      { exchange: { userMessage, assistantMessage }, unreadCount: 0 },
    )));
    expect(within(conversation).getByText('확인한 구독 목록입니다.')).toBeInTheDocument();
    expect(conversation.querySelectorAll('.wc-message.assistant')).toHaveLength(1);
    expect(within(conversation).queryByLabelText(waitingLabel)).not.toBeInTheDocument();
    expect(requestLog.filter((entry) => entry.path === '/api/assistant/requests')).toHaveLength(1);
  });

  it('keeps the home and chat layout while loading persisted history and sending to the API', async () => {
    enableFakeEventSource();
    history = [stored('previous', 'ASSISTANT', '서버에 저장된 이전 대화')];
    render(<App />);
    expect(document.querySelector('.phone-screen .top-bg')).not.toBeNull();
    const chat = await openChat();
    expect(await within(chat).findByText('서버에 저장된 이전 대화')).toBeInTheDocument();
    const input = within(chat).getByRole('textbox', { name: '메시지 입력' });
    fireEvent.change(input, { target: { value: '내 카드 혜택 비교해줘' } });
    fireEvent.click(within(chat).getByRole('button', { name: '보내기' }));
    await waitFor(() => expect(requestLog.some((r) => r.path === '/api/assistant/requests')).toBe(true));
    const request = JSON.parse(requestLog.find((r) => r.path === '/api/assistant/requests').options.body);
    expect(request).toMatchObject({ message: '내 카드 혜택 비교해줘' });
    expect(request.requestId).toMatch(/^[0-9a-f-]{36}$/i);
    const source = eventSources[0];
    act(() => source.emit('reply.started', replyEnvelope('started-1', 'reply.started', request.requestId, 'reply-new', { userMessageId: 'user-new' })));
    act(() => source.emit('reply.delta', replyEnvelope('delta-1', 'reply.delta', request.requestId, 'reply-new', { sequence: 1, delta: '서버에서 확인한 ' })));
    expect(within(chat).queryByText('서버에서 확인한')).not.toBeInTheDocument();
    expect(await within(chat).findByText('서버에서 확인한')).toBeInTheDocument();
    const userMessage = stored('user-new', 'USER', '내 카드 혜택 비교해줘');
    const assistantMessage = stored('reply-new', 'ASSISTANT', '서버에서 확인한 상담 답변입니다.');
    history = [stored('previous', 'ASSISTANT', '서버에 저장된 이전 대화'), userMessage, assistantMessage];
    act(() => source.emit('reply.completed', replyEnvelope('completed-1', 'reply.completed', request.requestId, 'reply-new', {
      exchange: { userMessage, assistantMessage }, unreadCount: 0,
    })));
    expect(await within(chat).findByText('서버에서 확인한 상담 답변입니다.')).toBeInTheDocument();
    expect(within(chat).getAllByText('내 카드 혜택 비교해줘')).toHaveLength(1);
    expect(requestLog.find((r) => r.path === '/api/assistant/requests').options.body).toContain('내 카드 혜택 비교해줘');
    expect(chat.querySelector('.wc-composer .wc-input-shell')).not.toBeNull();
  });
  it('uses the existing character alert and marks a fetched server notice as read', async () => {
    unread = 2;
    history = [{ ...stored('notice', 'ASSISTANT', '새 구독료 인상 알림'), source: 'PRICE_INCREASE' }];
    render(<App />);
    await waitFor(() => expect(screen.getByRole('button', { name: /워니 주요 알림: 구독 관련 중요한 소식/ })).toBeInTheDocument());
    const chat = await openChat();
    expect(await within(chat).findByText('새 구독료 인상 알림')).toBeInTheDocument();
    await waitFor(() => expect(requestLog.some((r) => r.path === '/api/assistant/conversation/read')).toBe(true));
  });
  it('restores a failed question and uses the existing toast without a fake answer', async () => {
    failSend = true;
    render(<App />);
    const chat = await openChat();
    const input = within(chat).getByRole('textbox', { name: '메시지 입력' });
    fireEvent.change(input, { target: { value: '구독료 분석해줘' } });
    fireEvent.click(within(chat).getByRole('button', { name: '보내기' }));
    expect(await within(chat).findByText('MCP 상담 연결을 확인해 주세요.')).toBeInTheDocument();
    expect(input).toHaveValue('구독료 분석해줘');
    expect(history).toHaveLength(0);
    expect(within(chat).queryByText('서버에서 확인한 상담 답변입니다.')).not.toBeInTheDocument();
  });
  it('restores the input when the asynchronous reply stream fails', async () => {
    enableFakeEventSource();
    render(<App />);
    const chat = await openChat();
    const input = within(chat).getByRole('textbox', { name: '메시지 입력' });
    fireEvent.change(input, { target: { value: '구독료 분석해줘' } });
    fireEvent.click(within(chat).getByRole('button', { name: '보내기' }));
    await waitFor(() => expect(requestLog.some((r) => r.path === '/api/assistant/requests')).toBe(true));
    const request = JSON.parse(requestLog.find((r) => r.path === '/api/assistant/requests').options.body);
    act(() => eventSources[0].emit('reply.failed', replyEnvelope(
      'failed-1', 'reply.failed', request.requestId, 'reply-new',
      { code: 'ASSISTANT_UNAVAILABLE', message: '답변 생성에 실패했어요.', retryable: true },
    )));
    expect(await within(chat).findByText('답변 생성에 실패했어요.')).toBeInTheDocument();
    expect(input).toHaveValue('구독료 분석해줘');
    expect(within(chat).queryByText('서버에서 확인한 상담 답변입니다.')).not.toBeInTheDocument();
  });
  it('recovers a missing stream delta from the request snapshot after SSE reconnect', async () => {
    enableFakeEventSource();
    render(<App />);
    const chat = await openChat();
    const input = within(chat).getByRole('textbox', { name: '메시지 입력' });
    fireEvent.change(input, { target: { value: '카드 결제 내역 확인해줘' } });
    fireEvent.click(within(chat).getByRole('button', { name: '보내기' }));
    await waitFor(() => expect(requestLog.some((r) => r.path === '/api/assistant/requests')).toBe(true));
    const request = JSON.parse(requestLog.find((r) => r.path === '/api/assistant/requests').options.body);
    const source = eventSources[0];
    act(() => {
      source.emit('reply.started', replyEnvelope('recover-started', 'reply.started', request.requestId, 'reply-recovery', { userMessageId: 'user-recovery' }));
      source.emit('reply.delta', replyEnvelope('recover-delta-1', 'reply.delta', request.requestId, 'reply-recovery', { sequence: 1, delta: '첫 조각 ' }));
    });
    expect(await within(chat).findByText('첫 조각')).toBeInTheDocument();

    requestStatuses.set(request.requestId, {
      requestId: request.requestId, conversationId: 'conversation', userMessageId: 'user-recovery',
      assistantMessageId: 'reply-recovery', status: 'RUNNING', lastSequence: 2,
      partialAnswer: '첫 조각 복구된 조각 ', failureCode: null, exchange: null,
    });
    act(() => source.emit('connected', {
      version: 1, eventId: 'reconnected-active', type: 'connected', occurredAt: '2026-10-27T08:00:00Z',
      conversationId: 'conversation', requestId: null, messageId: null,
      data: { connectionId: 'connection-recovered', unreadCount: 0 },
    }));
    expect(await within(chat).findByText('첫 조각 복구된 조각')).toBeInTheDocument();
    act(() => source.emit('reply.delta', replyEnvelope('recover-delta-3', 'reply.delta', request.requestId, 'reply-recovery', { sequence: 3, delta: '마지막 조각' })));
    expect(await within(chat).findByText('첫 조각 복구된 조각 마지막 조각')).toBeInTheDocument();

    const userMessage = stored('user-recovery', 'USER', '카드 결제 내역 확인해줘');
    const assistantMessage = stored('reply-recovery', 'ASSISTANT', '첫 조각 복구된 조각 마지막 조각');
    history = [userMessage, assistantMessage];
    act(() => source.emit('reply.completed', replyEnvelope('recover-completed', 'reply.completed', request.requestId, 'reply-recovery', {
      exchange: { userMessage, assistantMessage }, unreadCount: 0,
    })));
    expect(await within(chat).findByText('첫 조각 복구된 조각 마지막 조각')).toBeInTheDocument();
  });
  it('recovers a completed answer when the SSE connection delivers no request events', async () => {
    enableFakeEventSource();
    render(<App />);
    const chat = await openChat();
    vi.useFakeTimers();
    const request = await sendQuestion(chat, '무응답 복구 질문');
    const userMessage = stored('user-silent', 'USER', '무응답 복구 질문');
    const assistantMessage = stored('reply-silent', 'ASSISTANT', '상태 조회로 복구한 최종 답변');
    history = [userMessage, assistantMessage];
    requestStatuses.set(request.requestId, {
      requestId: request.requestId, status: 'COMPLETED', lastSequence: 3,
      partialAnswer: '', exchange: { userMessage, assistantMessage },
    });

    await act(async () => { await vi.advanceTimersByTimeAsync(15_000); });

    expect(within(chat).getByText('상태 조회로 복구한 최종 답변')).toBeInTheDocument();
    expect(requestLog.filter((entry) => entry.path === `/api/assistant/requests/${request.requestId}`)).toHaveLength(1);
  });
  it('recovers completion after one delta arrives and the SSE stream becomes silent', async () => {
    enableFakeEventSource();
    render(<App />);
    const chat = await openChat();
    vi.useFakeTimers();
    const request = await sendQuestion(chat, '부분 답변 복구 질문');
    const source = eventSources[0];
    act(() => source.emit('reply.delta', replyEnvelope(
      'silent-delta-1', 'reply.delta', request.requestId, 'reply-new', { sequence: 1, delta: '도착한 일부 ' },
    )));
    await act(async () => { await vi.advanceTimersByTimeAsync(200); });
    expect(within(chat).getByText('도착한 일부')).toBeInTheDocument();

    const userMessage = stored('user-partial', 'USER', '부분 답변 복구 질문');
    const assistantMessage = stored('reply-new', 'ASSISTANT', '도착한 일부 뒤의 최종 답변');
    history = [userMessage, assistantMessage];
    requestStatuses.set(request.requestId, {
      requestId: request.requestId, status: 'COMPLETED', lastSequence: 3,
      partialAnswer: '', exchange: { userMessage, assistantMessage },
    });
    await act(async () => { await vi.advanceTimersByTimeAsync(15_000); });

    expect(within(chat).getByText('도착한 일부 뒤의 최종 답변')).toBeInTheDocument();
  });
  it('waits for SSE before polling and backs off repeated running checks', async () => {
    enableFakeEventSource();
    render(<App />);
    const chat = await openChat();
    vi.useFakeTimers();
    const request = await sendQuestion(chat, '느린 답변 질문');
    const statusRequests = () => requestLog.filter(
      (entry) => entry.path === `/api/assistant/requests/${request.requestId}`,
    );

    await act(async () => { await vi.advanceTimersByTimeAsync(14_999); });
    expect(statusRequests()).toHaveLength(0);
    await act(async () => { await vi.advanceTimersByTimeAsync(1); });
    expect(statusRequests()).toHaveLength(1);

    await act(async () => { await vi.advanceTimersByTimeAsync(4_999); });
    expect(statusRequests()).toHaveLength(1);
    await act(async () => { await vi.advanceTimersByTimeAsync(1); });
    expect(statusRequests()).toHaveLength(2);

    await act(async () => { await vi.advanceTimersByTimeAsync(9_999); });
    expect(statusRequests()).toHaveLength(2);
    await act(async () => { await vi.advanceTimersByTimeAsync(1); });
    expect(statusRequests()).toHaveLength(3);
  });
  it('cancels recovery polling after a normal SSE completion and accepts the next question', async () => {
    enableFakeEventSource();
    render(<App />);
    const chat = await openChat();
    vi.useFakeTimers();
    const first = await sendQuestion(chat, '정상 완료 질문');
    const userMessage = stored('user-normal', 'USER', '정상 완료 질문');
    const assistantMessage = stored('reply-normal', 'ASSISTANT', '정상 SSE 최종 답변');
    history = [userMessage, assistantMessage];
    act(() => eventSources[0].emit('reply.completed', replyEnvelope(
      'normal-completed', 'reply.completed', first.requestId, 'reply-normal',
      { exchange: { userMessage, assistantMessage }, unreadCount: 0 },
    )));
    expect(within(chat).getByText('정상 SSE 최종 답변')).toBeInTheDocument();

    await act(async () => { await vi.advanceTimersByTimeAsync(20_000); });
    expect(requestLog.filter((entry) => entry.path.startsWith('/api/assistant/requests/'))).toHaveLength(0);
    await sendQuestion(chat, '다음 질문');
    expect(requestLog.filter((entry) => entry.path === '/api/assistant/requests')).toHaveLength(2);
  });
  it('cleans up recovery polling after cancellation and component unmount', async () => {
    enableFakeEventSource();
    const view = render(<App />);
    const chat = await openChat();
    vi.useFakeTimers();
    const cancelled = await sendQuestion(chat, '취소 상태 질문');
    requestStatuses.set(cancelled.requestId, {
      requestId: cancelled.requestId, status: 'CANCELLED', lastSequence: 0,
      partialAnswer: '', exchange: null,
    });
    await act(async () => { await vi.advanceTimersByTimeAsync(15_000); });
    expect(within(chat).getByText('답변 생성을 취소했어요. 질문을 입력창에서 다시 확인해 주세요.')).toBeInTheDocument();
    const cancelledChecks = requestLog.filter((entry) => entry.path === `/api/assistant/requests/${cancelled.requestId}`).length;
    await act(async () => { await vi.advanceTimersByTimeAsync(35_000); });
    expect(requestLog.filter((entry) => entry.path === `/api/assistant/requests/${cancelled.requestId}`)).toHaveLength(cancelledChecks);

    const active = await sendQuestion(chat, '언마운트 질문');
    view.unmount();
    await act(async () => { await vi.advanceTimersByTimeAsync(35_000); });
    expect(requestLog.filter((entry) => entry.path === `/api/assistant/requests/${active.requestId}`)).toHaveLength(0);
  });
  it('fills the original subscription card from server results after consent', async () => {
    render(<StrictMode><App /></StrictMode>);
    const consent = await screen.findByRole('dialog', { name: /매달 새는/ }, { timeout: 2500 });
    fireEvent.click(within(consent).getByRole('button', { name: '전체 동의' }));
    fireEvent.click(within(consent).getByRole('button', { name: '동의하고 내 구독 찾기' }));
    const chat = await screen.findByRole('dialog', { name: 'WONNIE와 대화중' });
    expect(await within(chat).findByText('카드 결제에서 찾은 구독 2개')).toBeInTheDocument();
    expect(within(chat).getByText('매달 77,000원')).toBeInTheDocument();
    expect(within(chat).getByText('서버 구독 A')).toBeInTheDocument();
    expect(within(chat).getByText('서버 구독 B')).toBeInTheDocument();
    expect(within(chat).getByText('매년 720,000원')).toBeInTheDocument();
    expect(chat.querySelectorAll('.wc-card .wc-rows .wc-row')).toHaveLength(2);
    expect(requestLog.some((entry) => entry.path === '/api/consents/subscription-data')).toBe(true);
  });
  it('restores saved consent choices and does not ask again', async () => {
    consentState = { subscriptionDataAgreed: true, notifications: { decided: true, d1: true, priceChange: false } };
    render(<App />);
    await waitFor(() => expect(requestLog.some((entry) => entry.path === '/api/consents')).toBe(true));
    await new Promise((resolve) => setTimeout(resolve, 1300));
    expect(screen.queryByRole('dialog', { name: /매달 새는/ })).not.toBeInTheDocument();
  });
  it('stores notification selections before requesting device permission', async () => {
    consentState = { subscriptionDataAgreed: false, notifications: { decided: false, d1: false, priceChange: false } };
    render(<App />);
    const consent = await screen.findByRole('dialog', { name: /매달 새는/ }, { timeout: 2500 });
    fireEvent.click(within(consent).getByRole('button', { name: '전체 동의' }));
    fireEvent.click(within(consent).getByRole('button', { name: '동의하고 내 구독 찾기' }));
    const chat = await screen.findByRole('dialog', { name: 'WONNIE와 대화중' });
    const d1 = await within(chat).findByRole('switch', { name: /결제 하루 전 알림/ }, { timeout: 4000 });
    fireEvent.click(d1);
    fireEvent.click(within(chat).getByRole('button', { name: '선택한 알림 받기 (1)' }));
    await waitFor(() => expect(requestLog.some((entry) => entry.path === '/api/consents/notifications')).toBe(true));
    const saved = requestLog.find((entry) => entry.path === '/api/consents/notifications');
    expect(JSON.parse(saved.options.body)).toEqual({ d1: true, priceChange: false });
    expect(await screen.findByRole('alertdialog', { name: /우리WON뱅킹/ })).toBeInTheDocument();
  });
  it('keeps consent available for retry when saving receives HTML instead of API JSON', async () => {
    vi.stubGlobal('fetch', vi.fn(async (path) => path === '/api/consents'
      ? response(consentState)
      : ({ ok: true, status: 200, json: async () => { throw new SyntaxError('Unexpected token < in JSON'); } })));
    render(<StrictMode><App /></StrictMode>);
    const consent = await screen.findByRole('dialog', { name: /매달 새는/ }, { timeout: 2500 });
    fireEvent.click(within(consent).getByRole('button', { name: '전체 동의' }));
    fireEvent.click(within(consent).getByRole('button', { name: '동의하고 내 구독 찾기' }));
    expect(await within(consent).findByText(/API 응답 형식이 올바르지 않아요/)).toBeInTheDocument();
    expect(document.querySelector('.phone-screen')).not.toBeNull();
    expect(screen.queryByRole('dialog', { name: 'WONNIE와 대화중' })).not.toBeInTheDocument();
    expect(within(consent).getByRole('button', { name: '동의하고 내 구독 찾기' })).toBeEnabled();
  });

  it('issues a recommended Woori card and completes a mock Netflix subscription from chat', async () => {
    render(<App showDemoPanel={false} />);
    const chat = await openChat();
    const input = within(chat).getByRole('textbox', { name: '메시지 입력' });
    fireEvent.change(input, { target: { value: '넷플릭스 구독해줘' } });
    fireEvent.click(within(chat).getByRole('button', { name: '보내기' }));

    fireEvent.click(await within(chat).findByRole('button', { name: /스탠다드 Full HD/ }, { timeout: 2500 }));
    fireEvent.click(await within(chat).findByRole('button', { name: /카드의정석2 DAILY.*바로 발급하기/ }, { timeout: 2500 }));
    const issueSheet = await screen.findByRole('dialog', { name: /약관 동의만 하면/ });
    fireEvent.click(within(issueSheet).getByRole('button', { name: /필수 약관 모두 동의/ }));
    fireEvent.click(within(issueSheet).getByRole('button', { name: '모두 동의하고 카드 발급' }));

    const paymentSheet = await screen.findByRole('dialog', { name: /결제 내용을/ }, { timeout: 5000 });
    fireEvent.click(within(paymentSheet).getByRole('button', { name: /필수 약관 모두 동의/ }));
    fireEvent.click(within(paymentSheet).getByRole('button', { name: '13,500원 결제 승인' }));

    expect(await within(chat).findByText('결제가 완료됐어요. 현재 구독 목록도 함께 정리했어요.', {}, { timeout: 6000 })).toBeInTheDocument();
    expect(within(chat).getByText('넷플릭스 스탠다드')).toBeInTheDocument();
    expect(within(chat).getByText('1개 · 월 환산 13,500원')).toBeInTheDocument();
    expect(await within(chat).findByText(/카드의정석2 DAILY 카드로 결제해 정가보다 5,000원 아꼈어요/, {}, { timeout: 3000 })).toBeInTheDocument();
    expect(within(chat).getByRole('button', { name: '응, 넣어줘' })).toBeInTheDocument();
    expect(requestLog.some((entry) => entry.path === '/api/subscription-checkout/cards')).toBe(true);
    expect(requestLog.some((entry) => entry.path === '/api/subscription-checkout/subscriptions')).toBe(true);
  }, 12000);

  it('receives proactive notices over SSE, deduplicates them, and reloads after reconnect', async () => {
    enableFakeEventSource();

    render(<App showDemoPanel={false} />);
    await waitFor(() => expect(eventSources).toHaveLength(1));
    const source = eventSources[0];
    expect(source.url).toBe('/api/assistant/events');
    expect(source.options).toEqual({ withCredentials: true });

    const envelope = (eventId, type, data, messageId = null) => ({
      version: 1, eventId, type, occurredAt: '2026-10-27T08:00:00Z',
      conversationId: 'conversation', requestId: null, messageId, data,
    });
    act(() => source.emit('connected', envelope('connected-1', 'connected', { connectionId: 'connection-1', unreadCount: 0 })));
    expect(requestLog.some((entry) => entry.path === '/api/assistant/conversation')).toBe(false);

    const notice = { ...stored('sse-notice', 'ASSISTANT', 'SSE로 온 구독료 알림'), source: 'PRICE_INCREASE' };
    history = [notice];
    unread = 1;
    const created = envelope('notice-event-1', 'message.created', { message: notice }, notice.id);
    act(() => {
      source.emit('message.created', created);
      source.emit('message.created', created); // Redelivery with the same eventId is ignored.
      source.emit('unread-count.changed', envelope('unread-event-1', 'unread-count.changed', { unreadCount: 1 }));
    });

    await waitFor(() => expect(screen.getByRole('button', { name: /워니 주요 알림: 구독 관련 중요한 소식/ })).toBeInTheDocument());
    const chat = await openChat();
    expect(await within(chat).findByText('SSE로 온 구독료 알림')).toBeInTheDocument();
    expect(within(chat).getAllByText('SSE로 온 구독료 알림')).toHaveLength(1);
    await waitFor(() => expect(requestLog.some((entry) => entry.path === '/api/assistant/conversation/read')).toBe(true));
    const conversationRequestsAfterOpen = requestLog.filter((entry) => entry.path === '/api/assistant/conversation').length;
    const readRequestsAfterOpen = requestLog.filter((entry) => entry.path === '/api/assistant/conversation/read').length;
    expect(conversationRequestsAfterOpen).toBe(1);
    expect(readRequestsAfterOpen).toBe(1);

    act(() => window.dispatchEvent(new Event('focus')));
    expect(requestLog.filter((entry) => entry.path === '/api/assistant/conversation')).toHaveLength(conversationRequestsAfterOpen);
    expect(requestLog.filter((entry) => entry.path === '/api/assistant/conversation/read')).toHaveLength(readRequestsAfterOpen);

    const recovered = stored('reconnected-notice', 'ASSISTANT', '재연결 후 다시 불러온 알림');
    history = [notice, recovered];
    unread = 1;
    act(() => source.emit('connected', envelope('connected-2', 'connected', { connectionId: 'connection-2', unreadCount: 1 })));
    expect(await within(chat).findByText('재연결 후 다시 불러온 알림')).toBeInTheDocument();
    await waitFor(() => expect(requestLog.filter((entry) => entry.path === '/api/assistant/conversation')).toHaveLength(2));
    await waitFor(() => expect(requestLog.filter((entry) => entry.path === '/api/assistant/conversation/read')).toHaveLength(2));
  });
});
