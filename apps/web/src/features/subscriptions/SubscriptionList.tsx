import { ChevronRight, PauseCircle, WalletCards } from 'lucide-react'
import type { SubscriptionSummary } from '../../shared/api/types'
import { formatShortDate, formatWon } from '../../shared/lib/format'

interface SubscriptionListProps {
  subscriptions: SubscriptionSummary[]
  compact?: boolean
}

const statusLabel: Record<SubscriptionSummary['status'], string> = {
  ACTIVE: '이용 중',
  PAYMENT_PAUSE_SCHEDULED: '중지 예정',
  PAYMENT_PAUSED: '결제 중지',
  RESUME_PENDING: '재개 예정',
  PAYMENT_FAILED: '결제 실패',
}

export const SubscriptionList = ({
  subscriptions,
  compact = false,
}: SubscriptionListProps) => {
  const visible = compact ? subscriptions.slice(0, 3) : subscriptions

  return (
    <section className="surface-card">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="section-title m-0">내 구독</h2>
          <p className="text-caption mb-0 mt-1">전용 지갑으로 결제를 관리해요</p>
        </div>
        {compact && (
          <button className="flex items-center border-0 bg-transparent text-[13px] font-semibold text-woori-blue">
            전체보기 <ChevronRight size={16} />
          </button>
        )}
      </div>

      <div className="divide-y divide-woori-line/75">
        {visible.map((subscription) => {
          const isPaused = subscription.status !== 'ACTIVE'
          return (
            <article key={subscription.id} className="flex items-center gap-3 py-3.5 first:pt-0 last:pb-0">
              <div
                className="flex size-12 shrink-0 items-center justify-center rounded-[18px] text-[17px] font-black text-white shadow-sm"
                style={{ background: subscription.brandColor }}
                aria-hidden="true"
              >
                {subscription.initial}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <h3 className="m-0 truncate text-[15px] font-bold">{subscription.name}</h3>
                  <span
                    className={`status-pill ${
                      isPaused
                        ? 'bg-orange-50 text-warning'
                        : 'bg-emerald-50 text-positive'
                    }`}
                  >
                    {statusLabel[subscription.status]}
                  </span>
                </div>
                <p className="mb-0 mt-1 flex items-center gap-1 text-[12px] text-woori-muted">
                  {isPaused ? <PauseCircle size={13} /> : <WalletCards size={13} />}
                  {isPaused
                    ? '자동 충전이 중지돼요'
                    : `${formatShortDate(subscription.nextPaymentDate)} 결제 예정`}
                </p>
              </div>
              <div className="text-right">
                <p className="m-0 text-[14px] font-bold">{formatWon(subscription.amount)}</p>
                <p className="mb-0 mt-1 text-[11px] text-woori-muted">{subscription.billingCycle}</p>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
