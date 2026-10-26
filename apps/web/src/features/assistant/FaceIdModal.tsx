import { Check, ScanFace, X } from 'lucide-react'

export type FaceIdState = 'idle' | 'scanning' | 'success' | 'failure'

interface FaceIdModalProps {
  state: FaceIdState
  onClose: () => void
}

export const FaceIdModal = ({ state, onClose }: FaceIdModalProps) => (
  <div className="fixed inset-0 z-[70] flex items-center justify-center bg-[#071a2a]/55 px-6 backdrop-blur-sm">
    <div className="w-full max-w-[340px] rounded-[32px] bg-white px-6 pb-7 pt-5 text-center shadow-2xl">
      <div className="flex justify-end">
        <button
          className="icon-button size-9 border-0 bg-woori-canvas"
          aria-label="닫기"
          onClick={onClose}
        >
          <X size={17} />
        </button>
      </div>

      <div className="relative mx-auto mt-2 flex size-28 items-center justify-center">
        {state === 'scanning' && (
          <>
            <span className="absolute inset-0 animate-ping rounded-[32px] border-2 border-woori-blue/35" />
            <span className="absolute inset-2 animate-pulse rounded-[26px] bg-woori-sky" />
          </>
        )}
        <div
          className={`relative flex size-24 items-center justify-center rounded-[28px] transition-all duration-300 ${
            state === 'success'
              ? 'bg-emerald-50 text-positive'
              : 'bg-woori-sky text-woori-blue'
          }`}
        >
          {state === 'success' ? <Check size={48} strokeWidth={2.4} /> : <ScanFace size={52} strokeWidth={1.7} />}
          {state === 'scanning' && (
            <span className="absolute left-3 right-3 top-1/2 h-0.5 animate-[scan_1.2s_ease-in-out_infinite] bg-woori-blue shadow-[0_0_10px_#0877c9]" />
          )}
        </div>
      </div>

      <h2 className="mb-0 mt-5 text-[20px] font-bold tracking-[-0.03em]">
        {state === 'success' ? '인증했어요' : 'Face ID로 확인할게요'}
      </h2>
      <p className="mb-0 mt-2 text-[13px] leading-5 text-woori-muted">
        {state === 'success'
          ? '결제 중지 워크플로를 시작합니다.'
          : '화면을 바라봐 주세요. MVP용 모의 인증입니다.'}
      </p>
    </div>
  </div>
)
