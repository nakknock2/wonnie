import { ArrowDownLeft, ArrowUpRight, CreditCard, Plus } from 'lucide-react'
import { formatShortDate, formatWon } from '../../shared/lib/format'

interface BalanceHeroProps {
  totalMonthlyCost: number
  activeCount: number
  nextPaymentName: string
  nextPaymentDate: string
}

const quickActions = [
  { label: '이체', icon: ArrowUpRight },
  { label: '받기', icon: ArrowDownLeft },
  { label: '카드', icon: CreditCard },
  { label: '추가', icon: Plus },
]

export const BalanceHero = ({
  totalMonthlyCost,
  activeCount,
  nextPaymentName,
  nextPaymentDate,
}: BalanceHeroProps) => (
  <section className="mx-4 overflow-hidden rounded-[28px] bg-gradient-to-br from-[#087acb] via-[#0a69b6] to-[#074c88] p-5 text-white shadow-[0_20px_50px_rgb(7_76_136_/_24%)]">
    <div className="flex items-start justify-between">
      <div>
        <p className="m-0 text-[13px] font-medium text-white/70">이번 달 구독 생활비</p>
        <p className="mb-1 mt-2 text-[30px] font-bold tracking-[-0.04em]">
          {formatWon(totalMonthlyCost)}
        </p>
        <p className="m-0 text-[12px] text-white/70">활성 구독 {activeCount}개</p>
      </div>
      <span className="rounded-full bg-white/14 px-3 py-1.5 text-[11px] font-semibold backdrop-blur">
        구독지갑
      </span>
    </div>

    <div className="my-5 h-px bg-white/15" />

    <div className="flex items-center justify-between rounded-2xl bg-white/10 px-4 py-3">
      <div>
        <p className="m-0 text-[11px] text-white/65">다음 결제</p>
        <p className="mb-0 mt-1 text-[14px] font-semibold">{nextPaymentName}</p>
      </div>
      <p className="m-0 text-[13px] font-semibold">{formatShortDate(nextPaymentDate)}</p>
    </div>

    <div className="mt-5 grid grid-cols-4 gap-2">
      {quickActions.map(({ label, icon: Icon }) => (
        <button
          key={label}
          className="flex flex-col items-center gap-2 border-0 bg-transparent text-white"
        >
          <span className="flex size-10 items-center justify-center rounded-2xl bg-white/14">
            <Icon size={18} />
          </span>
          <span className="text-[12px] font-medium">{label}</span>
        </button>
      ))}
    </div>
  </section>
)
