import type { AssistantConversation, AssistantMessageExchange, DashboardResponse, UnreadCount } from './types'

interface ApiErrorPayload {
  message?: string
}

const request = async <T>(path: string, options?: RequestInit): Promise<T> => {
  const response = await fetch(path, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  })

  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as ApiErrorPayload
    throw new Error(payload.message ?? '요청을 처리하지 못했어요.')
  }

  return response.json() as Promise<T>
}

export const api = {
  getDashboard: () => request<DashboardResponse>('/api/dashboard'),

  getAssistantConversation: () => request<AssistantConversation>('/api/assistant/conversation'),

  getAssistantUnreadCount: () => request<UnreadCount>('/api/assistant/unread-count'),

  markAssistantConversationRead: () =>
    request<UnreadCount>('/api/assistant/conversation/read', { method: 'POST' }),

  sendAssistantMessage: (message: string) =>
    request<AssistantMessageExchange>('/api/assistant/messages', {
      method: 'POST',
      body: JSON.stringify({ message }),
    }),
}
