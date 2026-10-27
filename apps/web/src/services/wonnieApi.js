// Same-origin requests pass through Vite's /api proxy in development.
const request = async (path, { timeout = 15000, signal, ...options } = {}) => {
  const controller = new AbortController();
  const abort = () => controller.abort();
  if (signal?.aborted) abort();
  signal?.addEventListener('abort', abort, { once: true });
  const timer = setTimeout(abort, timeout);
  try {
    const response = await fetch(path, {
      ...options,
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', ...options.headers },
      signal: controller.signal,
    });
    const payload = response.status === 204 ? null : await response.json().catch(() => null);
    if (!response.ok) {
      const error = new Error(payload?.message || `요청을 처리하지 못했어요. (HTTP ${response.status})`);
      error.status = response.status;
      throw error;
    }
    if (response.status !== 204 && (payload === null || typeof payload !== 'object' || Array.isArray(payload))) {
      throw new Error('API 응답 형식이 올바르지 않아요. 프론트의 /api 프록시 설정을 확인해 주세요.');
    }
    return payload;
  } catch (error) {
    if (signal?.aborted) throw error;
    if (controller.signal.aborted) throw new Error('응답 확인에 시간이 걸리고 있어요. 대화 기록을 확인한 뒤 다시 시도해 주세요.');
    if (error instanceof TypeError) throw new Error('API 서버에 연결할 수 없어요. 서버 실행 상태를 확인해 주세요.');
    throw error;
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener('abort', abort);
  }
};

export const wonnieApi = {
  conversation: (signal) => request('/api/assistant/conversation', { signal }),
  unreadCount: (signal) => request('/api/assistant/unread-count', { signal }),
  markRead: (signal) => request('/api/assistant/conversation/read', { method: 'POST', signal }),
  sendMessage: (message, topic) => request('/api/assistant/messages', {
    method: 'POST', timeout: 120000,
    body: JSON.stringify({ message, ...(topic ? { topic } : {}) }),
  }),
  submitAssistantRequest: (requestId, message, topic) => request('/api/assistant/requests', {
    method: 'POST',
    body: JSON.stringify({ requestId, message, ...(topic ? { topic } : {}) }),
  }),
  assistantRequestStatus: (requestId, signal) => request(`/api/assistant/requests/${encodeURIComponent(requestId)}`, { signal }),
  cancelAssistantRequest: (requestId) => request(`/api/assistant/requests/${encodeURIComponent(requestId)}/cancel`, { method: 'POST' }),
  subscriptions: (signal) => request('/api/finance/subscriptions', { signal }),
  dashboard: (signal) => request('/api/dashboard', { signal }),
  subscriptionCheckoutOptions: (service, plan, signal) => {
    const params = new URLSearchParams({ service });
    if (plan) params.set('plan', plan);
    return request(`/api/subscription-checkout/options?${params}`, { signal });
  },
  issueCard: (productId) => request('/api/subscription-checkout/cards', {
    method: 'POST', body: JSON.stringify({ productId, allTermsAgreed: true }),
  }),
  createSubscription: ({ requestId, service, planCode, cardId }) => request('/api/subscription-checkout/subscriptions', {
    method: 'POST', body: JSON.stringify({ requestId, service, planCode, cardId, allTermsAgreed: true }),
  }),
};

const kstDate = (value) => {
  const normalized = String(value).replace(/(\.\d{3})\d+/, '$1');
  return new Date(/(?:Z|[+-]\d{2}:\d{2})$/.test(normalized) ? normalized : `${normalized}+09:00`);
};

export const toChatMessage = (message) => {
  let metadata = null;
  if (typeof message.metadata === 'string' && message.metadata) {
    try { metadata = JSON.parse(message.metadata); } catch { /* 일반 텍스트로 표시 */ }
  }
  return {
    id: `server-${message.id}`,
    from: message.role === 'USER' ? 'user' : 'assistant',
    kind: message.source === 'CALENDAR_PAUSE' && metadata ? 'calendar-pause' : 'text',
    text: message.content,
    metadata,
    source: message.source,
    createdAt: kstDate(message.createdAt).getTime(),
    time: new Intl.DateTimeFormat('ko-KR', { timeZone: 'Asia/Seoul', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
      .format(kstDate(message.createdAt)),
  };
};

export const mergeChatMessages = (...groups) => {
  const byId = new Map();
  groups.flat().forEach((message) => byId.set(message.id, message));
  return [...byId.values()].sort((a, b) => (a.createdAt ?? 0) - (b.createdAt ?? 0));
};

export const toSubscriptionCard = (report, dashboard) => {
  const appearance = new Map((dashboard?.subscriptions || []).map((sub) => [sub.id, sub]));
  return {
    monthlyTotal: Number(report.data.confirmedMonthlyCost ?? report.data.projectedMonthlyCostAfterTrials),
    dataSource: report.dataSource,
    subscriptions: report.data.subscriptions.map((sub) => {
      const visual = appearance.get(sub.id);
      const evidence = sub.contractEvidence;
      return {
        id: sub.id,
        name: sub.name,
        price: Number(sub.amount),
        monthlyPrice: Number(sub.monthlyEquivalent),
        billingCycle: sub.billingCycle,
        nextPaymentDate: sub.nextPaymentDate,
        plan: evidence?.planName || sub.billingCycle,
        letter: visual?.initial || sub.name.slice(0, 1),
        color: visual?.brandColor || '#2789ec',
        card: evidence?.cardId || '',
      };
    }),
  };
};

export const apiDueLabel = (dateString, reference = new Date()) => {
  if (!dateString) return '결제일 확인 필요';
  const today = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' }).format(reference);
  const difference = Math.round((Date.parse(`${dateString}T00:00:00+09:00`) - Date.parse(`${today}T00:00:00+09:00`)) / 86400000);
  if (difference === 0) return '오늘 결제';
  if (difference === 1) return '내일 결제';
  // A past expected date is not evidence of successful payment.
  if (difference < 0) return '결제 내역 확인 필요';
  const [, month, day] = dateString.split('-');
  return `${Number(month)}월 ${Number(day)}일`;
};
