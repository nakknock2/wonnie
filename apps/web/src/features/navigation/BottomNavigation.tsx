import { CreditCard, Home, UserRound } from 'lucide-react'

export type AppTab = 'home' | 'subscriptions' | 'profile'

interface BottomNavigationProps {
  active: AppTab
  onChange: (tab: AppTab) => void
}

const items = [
  { id: 'home' as const, label: '홈', icon: Home },
  { id: 'subscriptions' as const, label: '구독', icon: CreditCard },
  { id: 'profile' as const, label: '내 정보', icon: UserRound },
]

export const BottomNavigation = ({ active, onChange }: BottomNavigationProps) => (
  <nav className="fixed inset-x-0 bottom-0 z-30 mx-auto max-w-[480px] border-t border-woori-line/75 bg-white/95 px-5 pb-[max(10px,env(safe-area-inset-bottom))] pt-2 backdrop-blur-xl">
    <div className="grid grid-cols-3">
      {items.map(({ id, label, icon: Icon }) => {
        const selected = active === id
        return (
          <button
            key={id}
            className={`flex flex-col items-center gap-1 border-0 bg-transparent py-1 text-[11px] font-semibold ${
              selected ? 'text-woori-blue' : 'text-woori-muted'
            }`}
            onClick={() => onChange(id)}
          >
            <span className={`rounded-xl px-3 py-1 ${selected ? 'bg-woori-sky' : ''}`}>
              <Icon size={20} strokeWidth={selected ? 2.5 : 1.8} />
            </span>
            {label}
          </button>
        )
      })}
    </div>
  </nav>
)
