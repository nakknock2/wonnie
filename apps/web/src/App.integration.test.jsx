import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { StrictMode } from 'react';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import App from './App.jsx';

let history;
let failSend;
let unread;
let requestLog;
const stored = (id, role, content) => ({ id, role, content, source: role === 'USER' ? 'USER_INPUT' : 'ASSISTANT_REPLY', createdAt: '2026-10-27T13:00:00' });
const response = (payload, status = 200) => ({ ok: status < 400, status, json: async () => payload });

beforeEach(() => {
  history = []; failSend = false; unread = 0; requestLog = [];
  Object.defineProperty(HTMLElement.prototype, 'scrollTo', { value: vi.fn(), configurable: true });
  vi.stubGlobal('fetch', vi.fn(async (path, options = {}) => {
    requestLog.push({ path, options });
    if (path === '/api/assistant/unread-count') return response({ unreadCount: unread });
    if (path === '/api/assistant/conversation') return response({ conversationId: 'conversation', messages: history, unreadCount: unread });
    if (path === '/api/assistant/conversation/read') { unread = 0; return response({ unreadCount: 0 }); }
    if (path === '/api/assistant/messages') {
      if (failSend) return response({ message: 'MCP 상담 연결을 확인해 주세요.' }, 503);
      const { message } = JSON.parse(options.body);
      const userMessage = stored('user-new', 'USER', message);
      const assistantMessage = stored('reply-new', 'ASSISTANT', '서버에서 확인한 상담 답변입니다.');
      history = [...history, userMessage, assistantMessage];
      return response({ userMessage, assistantMessage });
    }
    if (path === '/api/finance/subscriptions') return response({ dataSource: 'DATABASE', data: {
      projectedMonthlyCostAfterTrials: 77000, subscriptions: [
        { id: 'sub-server-one', name: '서버 구독 A', amount: 17000, monthlyEquivalent: 17000, billingCycle: '매월', nextPaymentDate: '2026-11-02' },
        { id: 'sub-server-two', name: '서버 구독 B', amount: 720000, monthlyEquivalent: 60000, billingCycle: '매년', nextPaymentDate: '2026-11-12' },
      ],
    } });
    if (path === '/api/dashboard') return response({ subscriptions: [{ id: 'sub-server-one', brandColor: '#111111', initial: 'A' }] });
    throw new Error(`Unexpected endpoint: ${path}`);
  }));
});
afterEach(() => { cleanup(); vi.unstubAllGlobals(); Reflect.deleteProperty(HTMLElement.prototype, 'scrollTo'); });

const openChat = async () => {
  fireEvent.click(screen.getByRole('button', { name: /AI 상담 챗봇 워니 열기|워니 주요 알림: 구독 관련 중요한 소식/ }));
  return screen.findByRole('dialog', { name: 'WONNIE와 대화중' });
};

describe('merged final frontend backend connection', () => {
  it('keeps the home and chat layout while loading persisted history and sending to the API', async () => {
    history = [stored('previous', 'ASSISTANT', '서버에 저장된 이전 대화')];
    render(<App />);
    expect(document.querySelector('.phone-screen .top-bg')).not.toBeNull();
    const chat = await openChat();
    expect(await within(chat).findByText('서버에 저장된 이전 대화')).toBeInTheDocument();
    const input = within(chat).getByRole('textbox', { name: '메시지 입력' });
    fireEvent.change(input, { target: { value: '내 카드 혜택 비교해줘' } });
    fireEvent.click(within(chat).getByRole('button', { name: '보내기' }));
    expect(await within(chat).findByText('서버에서 확인한 상담 답변입니다.')).toBeInTheDocument();
    expect(within(chat).getAllByText('내 카드 혜택 비교해줘')).toHaveLength(1);
    expect(requestLog.find((r) => r.path === '/api/assistant/messages').options.body).toBe(JSON.stringify({ message: '내 카드 혜택 비교해줘' }));
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
  it('fills the original subscription card from server results after consent', async () => {
    render(<StrictMode><App /></StrictMode>);
    const consent = await screen.findByRole('dialog', { name: /매달 새는/ }, { timeout: 2500 });
    fireEvent.click(within(consent).getByRole('button', { name: '전체 동의' }));
    fireEvent.click(within(consent).getByRole('button', { name: '동의하고 내 구독 찾기' }));
    const chat = screen.getByRole('dialog', { name: 'WONNIE와 대화중' });
    expect(await within(chat).findByText('카드 결제에서 찾은 구독 2개')).toBeInTheDocument();
    expect(within(chat).getByText('매달 77,000원')).toBeInTheDocument();
    expect(within(chat).getByText('서버 구독 A')).toBeInTheDocument();
    expect(within(chat).getByText('서버 구독 B')).toBeInTheDocument();
    expect(within(chat).getByText('매년 720,000원')).toBeInTheDocument();
    expect(chat.querySelectorAll('.wc-card .wc-rows .wc-row')).toHaveLength(2);
  });
  it('keeps the consent/chat screen mounted when the proxy returns HTML instead of API JSON', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, status: 200,
      json: async () => { throw new SyntaxError('Unexpected token < in JSON'); },
    })));
    render(<StrictMode><App /></StrictMode>);
    const consent = await screen.findByRole('dialog', { name: /매달 새는/ }, { timeout: 2500 });
    fireEvent.click(within(consent).getByRole('button', { name: '전체 동의' }));
    fireEvent.click(within(consent).getByRole('button', { name: '동의하고 내 구독 찾기' }));
    const chat = screen.getByRole('dialog', { name: 'WONNIE와 대화중' });
    expect(await within(chat).findByText(/API 응답 형식이 올바르지 않아요/)).toBeInTheDocument();
    expect(document.querySelector('.phone-screen')).not.toBeNull();
    expect(within(chat).getByRole('textbox', { name: '메시지 입력' })).toBeInTheDocument();
    expect(within(chat).queryByText(/카드 결제에서 찾은 구독/)).not.toBeInTheDocument();
  });
});
