import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AssistantPanel } from './AssistantPanel'
import { api } from '../../shared/api/client'

vi.mock('../../shared/api/client', () => ({ api: {
  getAssistantConversation: vi.fn(),
  markAssistantConversationRead: vi.fn(),
  sendAssistantMessage: vi.fn(),
} }))

beforeEach(() => {
  Object.defineProperty(HTMLElement.prototype, 'scrollTo', { value: vi.fn(), configurable: true })
  vi.mocked(api.getAssistantConversation).mockResolvedValue({ conversationId: 'demo', unreadCount: 1, messages: [{
    id: 'notice', role: 'ASSISTANT', source: 'FINANCE_NOTICE', content: '샘플 카드 포인트 소멸 안내', createdAt: '2026-10-27T12:00:00',
  }] })
  vi.mocked(api.markAssistantConversationRead).mockResolvedValue({ unreadCount: 0 })
})
afterEach(() => { vi.clearAllMocks(); Reflect.deleteProperty(HTMLElement.prototype, 'scrollTo') })

const renderPanel = () => render(
  <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    <AssistantPanel open onClose={() => {}} />
  </QueryClientProvider>,
)

describe('AssistantPanel', () => {
  it('loads persisted financial notices and marks them read', async () => {
    renderPanel()
    expect(await screen.findByText('샘플 카드 포인트 소멸 안내')).toBeInTheDocument()
    expect(screen.getByText('카드·구독 안내')).toBeInTheDocument()
    await waitFor(() => expect(api.markAssistantConversationRead).toHaveBeenCalledOnce())
  })
  it('restores input after an upstream failure instead of losing the question', async () => {
    vi.mocked(api.sendAssistantMessage).mockRejectedValueOnce(new Error('상담 연결 오류'))
    renderPanel()
    fireEvent.change(screen.getByPlaceholderText('위니에게 메시지를 입력하세요'), { target: { value: '내 카드 혜택을 비교해줘' } })
    fireEvent.click(screen.getByLabelText('전송'))
    expect(await screen.findByRole('alert')).toHaveTextContent('상담 연결 오류')
    expect(screen.getByPlaceholderText('위니에게 메시지를 입력하세요')).toHaveValue('내 카드 혜택을 비교해줘')
    expect(api.sendAssistantMessage).toHaveBeenCalledWith('내 카드 혜택을 비교해줘', expect.anything())
  })
})
