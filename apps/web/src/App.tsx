import { useState } from 'react'
import { AlertCircle, MessageCircleMore, RefreshCw, Sparkles } from 'lucide-react'
import { AssistantPanel } from './features/assistant/AssistantPanel'
import { CalendarPreview } from './features/calendar/CalendarPreview'
import { AppHeader } from './features/dashboard/AppHeader'
import { BalanceHero } from './features/dashboard/BalanceHero'
import { useDashboard } from './features/dashboard/use-dashboard'
import {
  BottomNavigation,
  type AppTab,
} from './features/navigation/BottomNavigation'
import { SubscriptionList } from './features/subscriptions/SubscriptionList'

export const App = () => {
  const dashboard = useDashboard()
  const [activeTab, setActiveTab] = useState<AppTab>('home')
  const [assistantOpen, setAssistantOpen] = useState(false)

  if (dashboard.isPending) {
    return (
      <main className="app-shell flex items-center justify-center">
        <div className="text-center text-woori-blue">
          <RefreshCw className="mx-auto animate-spin" />
          <p className="mt-3 text-[13px] font-semibold">내 금융 생활을 불러오고 있어요</p>
        </div>
      </main>
    )
  }

  if (dashboard.isError || !dashboard.data) {
    return (
      <main className="app-shell flex items-center justify-center px-6">
        <div className="surface-card text-center">
          <AlertCircle className="mx-auto text-warning" />
          <h1 className="mb-0 mt-4 text-[18px] font-bold">API 서버에 연결할 수 없어요</h1>
          <p className="text-caption mb-4 mt-2">`pnpm dev`로 백엔드와 프론트엔드를 함께 실행해 주세요.</p>
          <button className="btn-primary" onClick={() => dashboard.refetch()}>
            다시 시도
          </button>
        </div>
      </main>
    )
  }

  const data = dashboard.data

  return (
    <main className="app-shell">
      <AppHeader customerName={data.customerName} />

      {activeTab === 'home' && (
        <>
          <BalanceHero
            totalMonthlyCost={data.totalMonthlyCost}
            activeCount={data.activeCount}
            nextPaymentName={data.nextPaymentName}
            nextPaymentDate={data.nextPaymentDate}
          />

          <div className="mx-4 mt-5 space-y-5">
            <button
              className="flex w-full items-center gap-3 rounded-[22px] border border-blue-100 bg-gradient-to-r from-blue-50 to-violet-50 p-4 text-left shadow-sm"
              onClick={() => setAssistantOpen(true)}
            >
              <span className="flex size-11 shrink-0 items-center justify-center rounded-2xl bg-white text-woori-blue shadow-sm">
                <Sparkles size={20} />
              </span>
              <span className="min-w-0 flex-1">
                <strong className="block text-[14px]">구독이 적절한지 AI에게 물어보세요</strong>
                <span className="mt-1 block text-[12px] text-woori-muted">
                  일정과 지출을 함께 살펴봐요
                </span>
              </span>
            </button>
            <SubscriptionList subscriptions={data.subscriptions} compact />
            <CalendarPreview signals={data.calendarSignals} />
          </div>
        </>
      )}

      {activeTab === 'subscriptions' && (
        <div className="mx-4 space-y-5">
          <div className="px-1 pb-1 pt-2">
            <h1 className="m-0 text-[26px] font-bold tracking-[-0.04em]">구독 지갑</h1>
            <p className="text-caption mb-0 mt-2">서비스마다 전용 결제수단을 따로 관리해요.</p>
          </div>
          <SubscriptionList subscriptions={data.subscriptions} />
        </div>
      )}

      {activeTab === 'calendar' && (
        <div className="mx-4 space-y-5">
          <div className="px-1 pb-1 pt-2">
            <h1 className="m-0 text-[26px] font-bold tracking-[-0.04em]">구독 캘린더</h1>
            <p className="text-caption mb-0 mt-2">결제 일정과 생활 일정을 한눈에 확인하세요.</p>
          </div>
          <CalendarPreview signals={data.calendarSignals} />
          <SubscriptionList subscriptions={data.subscriptions} />
        </div>
      )}

      {activeTab === 'profile' && (
        <div className="mx-4">
          <section className="surface-card mt-2 text-center">
            <div className="mx-auto flex size-16 items-center justify-center rounded-full bg-woori-sky text-[22px] font-bold text-woori-blue">
              김
            </div>
            <h1 className="mb-0 mt-4 text-[20px] font-bold">{data.customerName}</h1>
            <p className="text-caption mb-0 mt-1">Google OIDC 연결 준비됨</p>
          </section>
        </div>
      )}

      <button
        className="fixed bottom-24 right-[max(20px,calc((100vw-480px)/2+20px))] z-20 flex size-15 items-center justify-center rounded-full border-4 border-white bg-gradient-to-br from-woori-blue to-violet-600 text-white shadow-[var(--shadow-float)] transition active:scale-95"
        aria-label="우리 AI 매니저 열기"
        onClick={() => setAssistantOpen(true)}
      >
        <MessageCircleMore size={26} />
      </button>

      <BottomNavigation active={activeTab} onChange={setActiveTab} />
      <AssistantPanel open={assistantOpen} onClose={() => setAssistantOpen(false)} />
    </main>
  )
}
