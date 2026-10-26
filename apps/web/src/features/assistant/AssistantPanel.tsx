import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  ArrowUp,
  CalendarRange,
  CheckCircle2,
  Mic,
  ShieldCheck,
  Sparkles,
  X,
} from 'lucide-react'
import { api } from '../../shared/api/client'
import { assistantKeys, dashboardKeys } from '../../shared/api/query-keys'
import type { ActionPlan, AssistantResponse } from '../../shared/api/types'
import { formatDateRange } from '../../shared/lib/format'
import { FaceIdModal, type FaceIdState } from './FaceIdModal'

interface AssistantPanelProps {
  open: boolean
  onClose: () => void
}

interface Message {
  id: string
  role: 'assistant' | 'user'
  text: string
  actionPlan?: ActionPlan
}

const welcomeMessage: Message = {
  id: 'welcome',
  role: 'assistant',
  text: '안녕하세요. 구독 지출을 살펴보고, 필요한 기간에는 결제를 잠시 쉬도록 도와드릴게요.',
}

export const AssistantPanel = ({ open, onClose }: AssistantPanelProps) => {
  const queryClient = useQueryClient()
  const [input, setInput] = useState('')
  const [messages, setMessages] = useState<Message[]>([welcomeMessage])
  const [faceIdState, setFaceIdState] = useState<FaceIdState>('idle')
  const [activePlan, setActivePlan] = useState<ActionPlan | null>(null)
  const [isListening, setIsListening] = useState(false)
  const listRef = useRef<HTMLDivElement>(null)

  const suggestions = useQuery({
    queryKey: assistantKeys.suggestions(),
    queryFn: api.getSuggestions,
    enabled: open,
  })

  const sendMessage = useMutation({
    mutationFn: api.sendAssistantMessage,
    onSuccess: (response: AssistantResponse) => {
      setMessages((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          role: 'assistant',
          text: response.reply,
          actionPlan: response.actionPlan,
        },
      ])
    },
  })

  const authenticate = useMutation({
    mutationFn: async (plan: ActionPlan) => {
      const challenge = await api.createAuthChallenge(plan.id)
      await new Promise((resolve) => window.setTimeout(resolve, 1_700))
      setFaceIdState('success')
      await new Promise((resolve) => window.setTimeout(resolve, 650))
      return api.completeMockFaceId(plan.id, {
        challengeId: challenge.challengeId,
        challenge: challenge.challenge,
      })
    },
    onSuccess: (result) => {
      setMessages((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          role: 'assistant',
          text: result.message,
        },
      ])
      void queryClient.invalidateQueries({ queryKey: dashboardKeys.all })
      window.setTimeout(() => {
        setFaceIdState('idle')
        setActivePlan(null)
      }, 700)
    },
    onError: () => setFaceIdState('failure'),
  })

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, sendMessage.isPending])

  const submit = (value: string) => {
    const trimmed = value.trim()
    if (!trimmed || sendMessage.isPending) return
    setMessages((current) => [
      ...current,
      { id: crypto.randomUUID(), role: 'user', text: trimmed },
    ])
    setInput('')
    sendMessage.mutate(trimmed)
  }

  const simulateVoice = () => {
    if (isListening) return
    setIsListening(true)
    window.setTimeout(() => {
      setIsListening(false)
      submit('시험 기간 동안 넷플릭스 결제를 잠시 중지해줘')
    }, 1_350)
  }

  const startAuthentication = (plan: ActionPlan) => {
    setActivePlan(plan)
    setFaceIdState('scanning')
    authenticate.mutate(plan)
  }

  if (!open) return null

  return (
    <>
      <div className="fixed inset-0 z-40 bg-[#081a28]/35 backdrop-blur-[2px]" onClick={onClose} />
      <section className="fixed inset-x-0 bottom-0 z-50 mx-auto flex h-[88vh] max-w-[480px] flex-col overflow-hidden rounded-t-[32px] bg-[#f6f9fb] shadow-2xl">
        <header className="flex items-center justify-between border-b border-woori-line/70 bg-white px-5 py-4">
          <div className="flex items-center gap-3">
            <span className="flex size-10 items-center justify-center rounded-2xl bg-gradient-to-br from-woori-blue to-violet-500 text-white">
              <Sparkles size={19} />
            </span>
            <div>
              <h2 className="m-0 text-[16px] font-bold">우리 AI 매니저</h2>
              <p className="m-0 mt-0.5 text-[11px] text-positive">● 준비됐어요</p>
            </div>
          </div>
          <button className="icon-button size-10" aria-label="대화창 닫기" onClick={onClose}>
            <X size={19} />
          </button>
        </header>

        <div ref={listRef} className="flex-1 overflow-y-auto px-4 py-5">
          <div className="space-y-4">
            {messages.map((message) => (
              <div key={message.id} className={message.role === 'user' ? 'flex justify-end' : ''}>
                <div
                  className={`max-w-[88%] rounded-[22px] px-4 py-3 text-[14px] leading-6 ${
                    message.role === 'user'
                      ? 'rounded-br-md bg-woori-blue text-white'
                      : 'rounded-bl-md border border-woori-line/70 bg-white text-woori-ink shadow-sm'
                  }`}
                >
                  <p className="m-0">{message.text}</p>
                  {message.actionPlan && (
                    <div className="mt-3 rounded-2xl bg-woori-canvas p-3.5 text-woori-ink">
                      <div className="flex items-center gap-2 text-[13px] font-bold">
                        <CalendarRange size={16} className="text-woori-blue" />
                        {message.actionPlan.subscriptionName} 결제 중지
                      </div>
                      <p className="mb-0 mt-2 text-[12px] text-woori-muted">
                        {formatDateRange(message.actionPlan.startsOn, message.actionPlan.endsOn)}
                      </p>
                      <p className="mb-0 mt-2 text-[11px] leading-4 text-warning">
                        {message.actionPlan.warning}
                      </p>
                      <button
                        className="btn-primary mt-3 w-full text-[13px]"
                        onClick={() => startAuthentication(message.actionPlan!)}
                        disabled={authenticate.isPending}
                      >
                        <ShieldCheck size={17} />
                        Face ID로 승인
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}

            {sendMessage.isPending && (
              <div className="w-fit rounded-[22px] rounded-bl-md border border-woori-line/70 bg-white px-4 py-3 shadow-sm">
                <div className="flex gap-1.5">
                  {[0, 1, 2].map((item) => (
                    <span
                      key={item}
                      className="size-2 animate-bounce rounded-full bg-woori-blue/65"
                      style={{ animationDelay: `${item * 120}ms` }}
                    />
                  ))}
                </div>
              </div>
            )}
          </div>

          {messages.length === 1 && (
            <div className="mt-6">
              <p className="mb-3 text-[12px] font-bold text-woori-muted">이렇게 물어보세요</p>
              <div className="space-y-2">
                {suggestions.data?.map((suggestion) => (
                  <button
                    key={suggestion}
                    className="flex w-full items-center gap-2 rounded-2xl border border-woori-line bg-white px-4 py-3 text-left text-[13px] font-medium text-woori-ink shadow-sm transition active:scale-[0.99]"
                    onClick={() => submit(suggestion)}
                  >
                    <Sparkles size={15} className="shrink-0 text-woori-blue" />
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        <form
          className="border-t border-woori-line/70 bg-white p-4 pb-[max(16px,env(safe-area-inset-bottom))]"
          onSubmit={(event) => {
            event.preventDefault()
            submit(input)
          }}
        >
          {isListening && (
            <div className="mb-3 flex items-center justify-center gap-2 text-[12px] font-semibold text-woori-blue">
              <span className="size-2 animate-ping rounded-full bg-woori-blue" />
              듣고 있어요…
            </div>
          )}
          <div className="flex items-center gap-2 rounded-[22px] border border-woori-line bg-woori-canvas p-2 pl-4 focus-within:border-woori-blue/50 focus-within:ring-4 focus-within:ring-woori-sky">
            <input
              className="min-w-0 flex-1 border-0 bg-transparent text-[14px] outline-none placeholder:text-woori-muted/65"
              placeholder="궁금한 내용을 말해보세요"
              value={input}
              onChange={(event) => setInput(event.target.value)}
            />
            <button
              type="button"
              className={`icon-button size-10 border-0 ${isListening ? 'bg-red-50 text-red-500' : ''}`}
              aria-label="음성으로 입력"
              onClick={simulateVoice}
            >
              <Mic size={18} />
            </button>
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

      {faceIdState !== 'idle' && activePlan && (
        <FaceIdModal
          state={faceIdState}
          onClose={() => {
            setFaceIdState('idle')
            setActivePlan(null)
          }}
        />
      )}
    </>
  )
}
