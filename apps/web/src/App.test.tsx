import { act, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { vi } from 'vitest'
import { App } from './App'

const dashboardData = {
  customerName: '김우리',
  totalMonthlyCost: 32000,
  activeCount: 2,
  nextPaymentName: 'Netflix',
  nextPaymentDate: '2026-11-02',
  subscriptions: [],
  calendarSignals: [],
}

const response = (payload: unknown) => ({
  ok: true,
  json: async () => payload,
}) as Response

vi.stubGlobal(
  'fetch',
  vi.fn().mockImplementation((path: RequestInfo | URL) =>
    Promise.resolve(String(path).endsWith('/api/assistant/unread-count')
      ? response({ unreadCount: 0 })
      : response(dashboardData)),
  ),
)

describe('App', () => {
  it('renders the subscription dashboard', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <App />
      </QueryClientProvider>,
    )

    expect(await screen.findByText('월 환산 구독비 예상')).toBeInTheDocument()
  })

  it('recovers automatically when the API starts after the web app', async () => {
    vi.useFakeTimers()
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    let dashboardCalls = 0
    vi.mocked(fetch).mockImplementation((path: RequestInfo | URL) => {
      const url = String(path)
      if (url.endsWith('/api/dashboard') && dashboardCalls++ === 0) {
        return Promise.reject(new TypeError('Failed to fetch'))
      }
      return Promise.resolve(url.endsWith('/api/assistant/unread-count')
        ? response({ unreadCount: 0 })
        : response(dashboardData))
    })

    try {
      render(
        <QueryClientProvider client={queryClient}>
          <App />
        </QueryClientProvider>,
      )

      await act(async () => { await vi.advanceTimersByTimeAsync(1) })
      expect(screen.getByText('API 서버에 연결할 수 없어요')).toBeInTheDocument()

      await act(async () => { await vi.advanceTimersByTimeAsync(3001) })
      expect(screen.getByText('월 환산 구독비 예상')).toBeInTheDocument()
      expect(screen.queryByText('API 서버에 연결할 수 없어요')).not.toBeInTheDocument()
    } finally {
      queryClient.clear()
      vi.useRealTimers()
    }
  })
})
