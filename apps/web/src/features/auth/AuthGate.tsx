import { useEffect, useState, type ReactNode } from 'react'
import { LogIn, RefreshCw, ShieldCheck } from 'lucide-react'
import { api } from '../../shared/api/client'
import type { AuthUser } from '../../shared/api/types'

type AuthState = 'loading' | 'signed-out' | 'signed-in' | 'error'

interface AuthGateProps {
  children: (user: AuthUser) => ReactNode
}

const Splash = () => (
  <main className="grid min-h-screen place-items-center content-center gap-1 bg-[radial-gradient(circle_at_50%_0%,rgb(54_169_225_/_14%),transparent_34rem)] px-6">
    <div className="grid size-[72px] place-items-center rounded-[24px] bg-gradient-to-br from-sky-100 to-violet-100 text-[34px]" aria-hidden="true">🌱</div>
    <p className="m-0 mt-2 text-[13px] font-extrabold tracking-[.18em] text-woori-blue">WONNIE</p>
    <p className="m-0 mt-1 text-[13px] text-woori-muted">내 금융 생활을 가볍게</p>
    <span className="mt-7 size-5 animate-spin rounded-full border-2 border-woori-blue/20 border-t-woori-blue" aria-label="불러오는 중" />
  </main>
)

const Login = ({ onRetry }: { onRetry: () => void }) => (
  <main className="grid min-h-screen place-items-center bg-[radial-gradient(circle_at_50%_0%,rgb(54_169_225_/_14%),transparent_34rem)] px-6">
    <div className="w-full max-w-[380px] rounded-[28px] border border-blue-950/10 bg-white/95 px-[26px] pb-7 pt-[34px] text-center shadow-[0_24px_70px_rgb(33_87_140_/_12%)]">
      <div className="mx-auto grid size-[72px] place-items-center rounded-[24px] bg-gradient-to-br from-sky-100 to-violet-100 text-[34px]" aria-hidden="true">🌱</div>
      <p className="m-0 mt-2 text-[13px] font-extrabold tracking-[.18em] text-woori-blue">WONNIE</p>
      <h1 className="mb-0 mt-7 text-[22px] font-bold tracking-[-.04em]">WONNIE를 시작해 보세요</h1>
      <p className="mb-6 mt-2 text-[13px] leading-[1.65] text-woori-muted">Google 계정으로 로그인하고 캘린더와 함께 금융 생활을 관리해요.</p>
      <a className="flex min-h-[50px] w-full items-center justify-center gap-2 rounded-[14px] bg-[#1a73e8] text-[14px] font-bold text-white no-underline" href="/oauth2/authorization/google">
        <LogIn size={19} aria-hidden="true" />
        Google로 계속하기
      </a>
      <p className="mt-3 flex items-center justify-center gap-1 text-[11px] text-woori-muted"><ShieldCheck size={14} aria-hidden="true" /> 일정 권한은 로그인 동의 화면에서 직접 선택해요.</p>
      <button className="mt-[18px] text-[12px] text-woori-blue underline" type="button" onClick={onRetry}>로그인 상태 다시 확인</button>
    </div>
  </main>
)

export const AuthGate = ({ children }: AuthGateProps) => {
  const [state, setState] = useState<AuthState>('loading')
  const [user, setUser] = useState<AuthUser | null>(null)

  const check = () => {
    setState('loading')
    api.getCurrentUser().then((nextUser) => {
      setUser(nextUser)
      setState('signed-in')
    }).catch((error: unknown) => {
      setState(error instanceof Error && /401|403|unauthorized/i.test(error.message) ? 'signed-out' : 'error')
    })
  }

  useEffect(check, [])

  if (state === 'loading') return <Splash />
  if (state === 'signed-out') return <Login onRetry={check} />
  if (state === 'error') return <Login onRetry={check} />
  return user ? <>{children(user)}</> : <Splash />
}
