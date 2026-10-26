import type {
  AssistantResponse,
  AuthChallenge,
  DashboardResponse,
  SagaResult,
} from './types'

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

  getSuggestions: () => request<string[]>('/api/assistant/suggestions'),

  sendAssistantMessage: (message: string) =>
    request<AssistantResponse>('/api/assistant/messages', {
      method: 'POST',
      body: JSON.stringify({ message }),
    }),

  createAuthChallenge: (actionPlanId: string) =>
    request<AuthChallenge>(`/api/action-plans/${actionPlanId}/auth-challenge`, {
      method: 'POST',
    }),

  completeMockFaceId: (
    actionPlanId: string,
    payload: { challengeId: string; challenge: string },
  ) =>
    request<SagaResult>(`/api/action-plans/${actionPlanId}/mock-face-id`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
}
