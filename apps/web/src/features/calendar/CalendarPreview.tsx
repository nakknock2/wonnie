import { CalendarDays, Sparkles } from 'lucide-react'
import type { CalendarSignal } from '../../shared/api/types'
import { formatDateRange } from '../../shared/lib/format'

interface CalendarPreviewProps {
  signals: CalendarSignal[]
}

export const CalendarPreview = ({ signals }: CalendarPreviewProps) => (
  <section className="surface-card">
    <div className="mb-4 flex items-center gap-3">
      <span className="flex size-11 items-center justify-center rounded-2xl bg-violet-50 text-violet-600">
        <CalendarDays size={20} />
      </span>
      <div>
        <h2 className="section-title m-0">캘린더 인사이트</h2>
        <p className="text-caption mb-0 mt-1">Google 캘린더를 바탕으로 찾았어요</p>
      </div>
    </div>

    {signals.map((signal) => (
      <article key={signal.id} className="rounded-2xl bg-[#f7f4ff] p-4">
        <div className="flex items-start gap-3">
          <Sparkles className="mt-0.5 shrink-0 text-violet-600" size={18} />
          <div>
            <p className="m-0 text-[14px] font-bold text-violet-950">{signal.title}</p>
            <p className="mb-0 mt-1 text-[12px] text-violet-700/75">
              {formatDateRange(signal.startsOn, signal.endsOn)} · 신뢰도{' '}
              {Math.round(signal.confidence * 100)}%
            </p>
          </div>
        </div>
      </article>
    ))}
  </section>
)
