import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { AlertCircle, LogOut, RefreshCw, Sparkles, X } from 'lucide-react'
import { AssistantPanel } from './features/assistant/AssistantPanel'
import { AppHeader } from './features/dashboard/AppHeader'
import { BalanceHero } from './features/dashboard/BalanceHero'
import { useDashboard } from './features/dashboard/use-dashboard'
import {
  BottomNavigation,
  type AppTab,
} from './features/navigation/BottomNavigation'
import { SubscriptionList } from './features/subscriptions/SubscriptionList'
import { api } from './shared/api/client'
import { assistantKeys } from './shared/api/query-keys'
import type { AuthUser } from './shared/api/types'

interface AppProps {
  user?: AuthUser
}

export const App = ({ user }: AppProps) => {
  const dashboard = useDashboard()
  const unread = useQuery({
    queryKey: assistantKeys.unread(),
    queryFn: api.getAssistantUnreadCount,
    refetchInterval: 60_000,
  })
  const [activeTab, setActiveTab] = useState<AppTab>('home')
  const [assistantOpen, setAssistantOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  const logout = () => {
    fetch('/logout', { method: 'POST', credentials: 'include' }).finally(() => {
      window.location.assign('/')
    })
  }

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
          <p className="text-caption mb-4 mt-2">
            API 서버가 실행 중인지 확인해 주세요. 연결되면 화면이 자동으로 복구돼요. 3초마다 다시 확인하고 있어요.
          </p>
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
      <AppHeader customerName={user?.name ?? data.customerName} onMenu={() => setMenuOpen(true)} />

      {menuOpen && (
        <>
          <button className="fixed inset-0 z-40 bg-slate-950/25" aria-label="메뉴 닫기" onClick={() => setMenuOpen(false)} />
          <aside className="fixed inset-y-0 left-0 z-50 w-[min(86vw,360px)] bg-white p-6 shadow-2xl" aria-label="계정 메뉴">
            <div className="flex items-center justify-between">
              <h2 className="m-0 text-[20px] font-bold">내 계정</h2>
              <button className="icon-button" aria-label="메뉴 닫기" onClick={() => setMenuOpen(false)}><X size={20} /></button>
            </div>
            <div className="mt-8 flex items-center gap-3 rounded-2xl bg-sky-50 p-4">
              {user?.picture ? <img className="size-12 rounded-full" src={user.picture} alt="" /> : <div className="grid size-12 place-items-center rounded-full bg-woori-blue text-lg font-bold text-white">{(user?.name ?? data.customerName).slice(0, 1)}</div>}
              <div className="min-w-0">
                <p className="m-0 truncate text-[15px] font-bold">{user?.name ?? data.customerName}</p>
                {user?.email && <p className="m-0 mt-1 truncate text-[12px] text-woori-muted">{user.email}</p>}
              </div>
            </div>
            <button className="mt-8 flex w-full items-center gap-2 rounded-xl border border-red-100 px-4 py-3 text-left text-[14px] font-semibold text-red-600" onClick={logout}>
              <LogOut size={17} /> 로그아웃
            </button>
          </aside>
        </>
      )}

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
                <strong className="block text-[14px]">카드·구독 상담, 위니에게 물어보세요</strong>
                <span className="mt-1 block text-[12px] text-woori-muted">
                  구독료 변동부터 보유 카드 혜택까지 살펴봐요
                </span>
              </span>
            </button>
            <SubscriptionList subscriptions={data.subscriptions} compact />
          </div>
        </>
      )}

      {activeTab === 'subscriptions' && (
        <div className="mx-4 space-y-5">
          <div className="px-1 pb-1 pt-2">
            <h1 className="m-0 text-[26px] font-bold tracking-[-0.04em]">내 구독</h1>
            <p className="text-caption mb-0 mt-2">최근 결제 금액이 오르면 위니가 알려드려요.</p>
          </div>
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
            <p className="text-caption mb-0 mt-1">샘플 고객 데이터</p>
          </section>
        </div>
      )}

      <button
        className="fixed bottom-24 right-[max(20px,calc((100vw-480px)/2+20px))] z-20 flex size-15 items-center justify-center rounded-full border-4 border-white bg-gradient-to-br from-woori-blue to-violet-600 text-[25px] shadow-[var(--shadow-float)] transition active:scale-95"
        aria-label="위니 금융 비서 열기"
        onClick={() => setAssistantOpen(true)}
      >
        🌱
        {!!unread.data?.unreadCount && (
          <span className="absolute right-0 top-0 size-3 rounded-full border-2 border-white bg-red-500" aria-label="읽지 않은 알림" />
        )}
      </button>

      <BottomNavigation active={activeTab} onChange={setActiveTab} />
      <AssistantPanel open={assistantOpen} onClose={() => setAssistantOpen(false)} />
    </main>
  )
}
