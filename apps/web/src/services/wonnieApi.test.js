import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiDueLabel, mergeChatMessages, toChatMessage, toSubscriptionCard, wonnieApi } from './wonnieApi.js';

afterEach(() => vi.unstubAllGlobals());

describe('backend adapter', () => {
  it('stores both notification choices in the backend consent model', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ notifications: { decided: true, d1: true, priceChange: false } }) });
    vi.stubGlobal('fetch', fetch);
    await wonnieApi.updateNotificationConsents({ d1: true, price: false });
    expect(fetch).toHaveBeenCalledWith('/api/consents/notifications', expect.objectContaining({
      method: 'PUT', credentials: 'include', body: JSON.stringify({ d1: true, priceChange: false }),
    }));
  });
  it('sends the original question and optional topic with session credentials', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ userMessage: {}, assistantMessage: {} }) });
    vi.stubGlobal('fetch', fetch);
    await wonnieApi.sendMessage('얼마나 절약돼?', '구독 카드 혜택 비교');
    expect(fetch).toHaveBeenCalledWith('/api/assistant/messages', expect.objectContaining({
      method: 'POST', credentials: 'include', body: JSON.stringify({ message: '얼마나 절약돼?', topic: '구독 카드 혜택 비교' }),
    }));
  });
  it('surfaces an API error instead of returning prepared demo data', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 503, json: async () => ({ message: '상담 연결 오류' }) }));
    await expect(wonnieApi.sendMessage('추천해줘')).rejects.toThrow('상담 연결 오류');
  });
  it('rejects an HTTP 200 HTML fallback instead of passing null to React state', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 200,
      json: async () => { throw new SyntaxError('Unexpected token < in JSON'); },
    }));
    await expect(wonnieApi.conversation()).rejects.toThrow('/api 프록시 설정');
    await expect(wonnieApi.subscriptions()).rejects.toThrow('/api 프록시 설정');
  });
  it('merges persisted exchanges by server id without duplicating a polled message', () => {
    const message = { id: 'one', role: 'ASSISTANT', source: 'PRICE_INCREASE', content: '구독료 변동', createdAt: '2026-10-27T13:02:03.123456' };
    const normalized = toChatMessage(message);
    expect(normalized).toMatchObject({ id: 'server-one', from: 'assistant', kind: 'text', text: '구독료 변동', time: '13:02' });
    expect(mergeChatMessages([normalized], [toChatMessage(message)])).toHaveLength(1);
  });
  it('turns a structured calendar pause notice into the existing chat card message kind', () => {
    const metadata = JSON.stringify({ signalLabel: '시험 기간', recommendations: [{ id: 'sub-watcha', name: '왓챠', price: 7900 }] });
    expect(toChatMessage({ id: 'calendar-one', role: 'ASSISTANT', source: 'CALENDAR_PAUSE',
      content: '잠시 중단할 구독을 확인했어요.', metadata, createdAt: '2026-10-27T13:02:03' }))
      .toMatchObject({ kind: 'calendar-pause', metadata: { signalLabel: '시험 기간' } });
  });
  it('adapts live subscription count, amounts and existing brand appearance', () => {
    const result = toSubscriptionCard({ dataSource: 'DATABASE', data: { projectedMonthlyCostAfterTrials: 50000, subscriptions: [{
      id: 'sub-gym', name: '연간권', amount: 600000, monthlyEquivalent: 50000, billingCycle: '매년', nextPaymentDate: '2026-11-01',
    }] } }, { subscriptions: [{ id: 'sub-gym', brandColor: '#123456', initial: 'G' }] });
    expect(result.monthlyTotal).toBe(50000);
    expect(result.subscriptions).toHaveLength(1);
    expect(result.subscriptions[0]).toMatchObject({ price: 600000, monthlyPrice: 50000, color: '#123456', letter: 'G' });
  });
  it('never treats a past scheduled payment date as proof of successful payment', () => {
    expect(apiDueLabel('2026-10-20', new Date('2026-10-27T12:00:00+09:00'))).toBe('결제 내역 확인 필요');
    expect(apiDueLabel('2026-10-28', new Date('2026-10-27T12:00:00+09:00'))).toBe('내일 결제');
  });
  it('shows active cost separately from the trial-inclusive projection', () => {
    const result = toSubscriptionCard({ dataSource: 'DATABASE', data: {
      confirmedMonthlyCost: 17000, projectedMonthlyCostAfterTrials: 24890,
      subscriptions: [{ id: 'sub-netflix', name: 'Netflix', amount: 17000, monthlyEquivalent: 17000, billingCycle: '매월', nextPaymentDate: '2026-11-01' }],
    } });
    expect(result.monthlyTotal).toBe(17000);
    expect(result.subscriptions).toHaveLength(1);
  });
});
