import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { vi } from 'vitest'
import { App } from './App'

vi.stubGlobal(
  'fetch',
  vi.fn().mockResolvedValue({
    ok: true,
    json: async () => ({
      customerName: '김우리',
      totalMonthlyCost: 32000,
      activeCount: 2,
      pausedCount: 0,
      nextPaymentName: 'Netflix',
      nextPaymentDate: '2026-11-02',
      subscriptions: [],
      calendarSignals: [],
    }),
  }),
)

describe('App', () => {
  it('renders the subscription wallet dashboard', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <App />
      </QueryClientProvider>,
    )

    expect(await screen.findByText('이번 달 구독 생활비')).toBeInTheDocument()
  })
})
