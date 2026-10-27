import { CalendarClock, ChevronRight } from 'lucide-react'
import type { SubscriptionSummary } from '../../shared/api/types'
import { formatShortDate, formatWon } from '../../shared/lib/format'

interface SubscriptionListProps {
  subscriptions: SubscriptionSummary[]
  compact?: boolean
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
          <p className="text-caption mb-0 mt-1">등록된 구독과 최근 결제 금액이에요</p>
        </div>
        {compact && (
          <button className="flex items-center border-0 bg-transparent text-[13px] font-semibold text-woori-blue">
            전체보기 <ChevronRight size={16} />
          </button>
        )}
      </div>

      <div className="divide-y divide-woori-line/75">
        {visible.map((subscription) => {
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
                  <span className="status-pill bg-emerald-50 text-positive">이용 중</span>
                </div>
                <p className="mb-0 mt-1 flex items-center gap-1 text-[12px] text-woori-muted">
                  <CalendarClock size={13} />
                  {`${formatShortDate(subscription.nextPaymentDate)} 결제 예정`}
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
