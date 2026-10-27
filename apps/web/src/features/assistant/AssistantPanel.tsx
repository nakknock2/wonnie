import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowUp, X } from 'lucide-react'
import { api } from '../../shared/api/client'
import { assistantKeys } from '../../shared/api/query-keys'
import type { AssistantMessage } from '../../shared/api/types'

interface AssistantPanelProps {
  open: boolean
  onClose: () => void
}

const starterQuestions = [
  '이번 달 구독 총액과 내역 알려줘',
  'APPLE.COM/BILL이 무슨 결제야?',
  '구독료가 오른 내역 알려줘',
  '겹치는 구독이 있어?',
  '구독 혜택이 좋은 보유 카드는?',
  '카드 만료와 포인트 소멸일 알려줘',
]

const formatTime = (value: string) =>
  new Intl.DateTimeFormat('ko-KR', { hour: 'numeric', minute: '2-digit' }).format(new Date(value))

const MessageBubble = ({ message }: { message: AssistantMessage }) => {
  const isUser = message.role === 'USER'

  return (
    <div className={isUser ? 'flex justify-end' : 'flex items-end gap-2'}>
      {!isUser && (
        <span className="mb-1 flex size-8 shrink-0 items-center justify-center rounded-full bg-woori-sky text-[17px]" aria-hidden="true">
          🌱
        </span>
      )}
      <div
        className={`max-w-[84%] rounded-[22px] px-4 py-3 text-[14px] leading-6 ${
          isUser
            ? 'rounded-br-md bg-woori-blue text-white'
            : 'rounded-bl-md border border-woori-line/70 bg-white text-woori-ink shadow-sm'
        }`}
      >
        {message.source === 'PRICE_INCREASE' || message.source === 'FINANCE_NOTICE' && (
          <span className="mb-2 inline-flex rounded-full bg-blue-50 px-2.5 py-1 text-[11px] font-bold text-woori-blue">
            카드·구독 안내
          </span>
        )}
        <p className="m-0 whitespace-pre-wrap">{message.content}</p>
        <time className={`mt-1 block text-right text-[10px] ${isUser ? 'text-white/70' : 'text-woori-muted'}`}>
          {formatTime(message.createdAt)}
        </time>
      </div>
    </div>
  )
}

export const AssistantPanel = ({ open, onClose }: AssistantPanelProps) => {
  const queryClient = useQueryClient()
  const [input, setInput] = useState('')
  const [error, setError] = useState('')
  const listRef = useRef<HTMLDivElement>(null)

  const conversation = useQuery({
    queryKey: assistantKeys.conversation(),
    queryFn: api.getAssistantConversation,
    enabled: open,
  })

  const sendMessage = useMutation({
    mutationFn: api.sendAssistantMessage,
    onSuccess: async () => {
      setError('')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: assistantKeys.conversation() }),
        queryClient.invalidateQueries({ queryKey: assistantKeys.unread() }),
      ])
    },
    onError: (sendError: Error, sentMessage) => {
      setError(sendError.message)
      setInput(sentMessage)
    },
  })

  useEffect(() => {
    if (!open) return
    void api.markAssistantConversationRead().then(() =>
      queryClient.invalidateQueries({ queryKey: assistantKeys.unread() }),
    ).catch(() => undefined)
  }, [open, queryClient])

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' })
  }, [conversation.data?.messages?.length, sendMessage.isPending])

  const submit = (value: string) => {
    const trimmed = value.trim()
    if (!trimmed || sendMessage.isPending) return
    setInput('')
    setError('')
    sendMessage.mutate(trimmed)
  }

  if (!open) return null

  const messages = conversation.data?.messages ?? []

  return (
    <>
      <div className="fixed inset-0 z-40 bg-[#081a28]/35 backdrop-blur-[2px]" onClick={onClose} />
      <section className="fixed inset-x-0 bottom-0 z-50 mx-auto flex h-[88vh] max-w-[480px] flex-col overflow-hidden rounded-t-[32px] bg-[#f6f9fb] shadow-2xl">
        <header className="flex items-center justify-between border-b border-woori-line/70 bg-white px-5 py-4">
          <div className="flex items-center gap-3">
            <span className="flex size-11 items-center justify-center rounded-full bg-woori-sky text-[23px]" aria-hidden="true">
              🌱
            </span>
            <div>
              <h2 className="m-0 text-[16px] font-bold">위니 금융 비서</h2>
              <p className="m-0 mt-0.5 text-[11px] text-positive">● 샘플 카드·구독 데이터로 상담해요</p>
            </div>
          </div>
          <button className="icon-button size-10" aria-label="대화창 닫기" onClick={onClose}>
            <X size={19} />
          </button>
        </header>

        <div ref={listRef} className="flex-1 overflow-y-auto px-4 py-5">
          <div className="space-y-4">
            {conversation.isPending && (
              <p className="text-center text-[13px] text-woori-muted">대화 내용을 불러오고 있어요.</p>
            )}
            {conversation.isError && (
              <p className="rounded-2xl bg-red-50 p-3 text-[13px] text-red-700">
                대화 내용을 불러오지 못했어요. 잠시 후 다시 열어주세요.
              </p>
            )}
            {messages.length === 0 && !conversation.isPending && !conversation.isError && (
              <div className="space-y-4">
                <div className="flex items-end gap-2">
                  <span className="mb-1 flex size-8 shrink-0 items-center justify-center rounded-full bg-woori-sky text-[17px]" aria-hidden="true">🌱</span>
                  <div className="max-w-[84%] rounded-[22px] rounded-bl-md border border-woori-line/70 bg-white px-4 py-3 text-[14px] leading-6 text-woori-ink shadow-sm">
                    안녕하세요! 카드·구독 내역을 바탕으로 구독 총액, 결제명, 가격 변동, 중복 구독과 카드 혜택을 안내해드릴게요.
                  </div>
                </div>
                <div className="ml-10 flex flex-wrap gap-2">
                  {starterQuestions.map((question) => (
                    <button
                      key={question}
                      type="button"
                      className="rounded-full border border-woori-line bg-white px-3 py-2 text-[12px] font-semibold text-woori-ink"
                      onClick={() => submit(question)}
                    >
                      {question}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {messages.map((message) => <MessageBubble key={message.id} message={message} />)}

            {sendMessage.isPending && (
              <div className="w-fit rounded-[22px] rounded-bl-md border border-woori-line/70 bg-white px-4 py-3 text-[14px] text-woori-muted shadow-sm">
                답변을 준비하고 있어요…
              </div>
            )}
            {error && <p role="alert" className="m-0 text-center text-[12px] text-red-600">{error}</p>}
          </div>
        </div>

        <form
          className="border-t border-woori-line/70 bg-white px-4 pb-[max(12px,env(safe-area-inset-bottom))] pt-3"
          onSubmit={(event) => {
            event.preventDefault()
            submit(input)
          }}
        >
          <p className="mb-2 mt-0 text-center text-[10px] text-woori-muted">
            구독 변경이나 해지는 해당 서비스에서 직접 진행해 주세요.
          </p>
          <div className="flex items-center gap-2 rounded-[22px] border border-woori-line bg-woori-canvas p-2 pl-4 focus-within:border-woori-blue/50 focus-within:ring-4 focus-within:ring-woori-sky">
            <input
              className="min-w-0 flex-1 border-0 bg-transparent text-[14px] outline-none placeholder:text-woori-muted/65"
              placeholder="위니에게 메시지를 입력하세요"
              maxLength={4000}
              value={input}
              onChange={(event) => setInput(event.target.value)}
            />
            <button
              type="submit"
              className="flex size-10 items-center justify-center rounded-full border-0 bg-woori-blue text-white disabled:opacity-35"
              aria-label="전송"
              disabled={!input.trim() || sendMessage.isPending}
            >
              <ArrowUp size={18} />
            </button>
          </div>
        </form>
      </section>
    </>
  )
}
