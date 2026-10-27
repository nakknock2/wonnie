import { useEffect, useState } from 'react';
import { won } from '../../data/subscriptions.js';

export default function SubscriptionPauseSheet({ open, subscription, onClose, onComplete, onCancel, onAuthenticate }) {
  const [step, setStep] = useState('confirm');
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [authenticating, setAuthenticating] = useState(false);

  useEffect(() => { if (open) { setStep('confirm'); setError(''); setResult(null); setAuthenticating(false); } }, [open, subscription?.id]);

  const cancel = async () => {
    if (authenticating) return;
    setError('');
    try {
      setAuthenticating(true);
      const authenticated = await onAuthenticate?.();
      if (authenticated === false) return;
      setStep('working');
      const cancellation = await onCancel?.(subscription);
      setResult(cancellation);
      setStep('done');
    } catch (failure) {
      setError(failure.message);
      setStep('confirm');
    } finally {
      setAuthenticating(false);
    }
  };

  if (!open || !subscription) return null;
  return (
    <div className="ts is-open" aria-hidden="false">
      <button type="button" className="ts-dim" aria-label="닫기" onClick={step === 'confirm' && !authenticating ? onClose : undefined} />
      <section className="ts-panel" role="dialog" aria-modal="true" aria-labelledby="pause-sheet-title">
        <div className="ts-grabber" />
        {step === 'confirm' && (
          <>
            <h2 id="pause-sheet-title">{subscription.name} 구독을 해지할까요?</h2>
            <p className="pause-sheet__reason">{subscription.reason}</p>
            <div className="pause-sheet__service">
              <span style={{ background: subscription.color }}>{subscription.letter}</span>
              <div><strong>{subscription.name}</strong><small>월 {won(Number(subscription.price))}</small></div>
            </div>
            <p className="ts-mock">모의 해지예요 · 실제 구독 상태는 바뀌지 않아요</p>
            {error && <p className="sc-sheet-error">{error}</p>}
            <div className="ts-btns">
              <button type="button" className="ts-btn ts-btn--ghost" disabled={authenticating} onClick={onClose}>유지할게요</button>
              <button type="button" className="ts-btn pause-sheet__danger" disabled={authenticating} onClick={cancel}>Face ID로 해지하기</button>
            </div>
          </>
        )}
        {step === 'working' && (
          <div className="ts-center"><span className="cp-spinner" aria-hidden="true" /><p>해지를 처리하고 있어요</p></div>
        )}
        {step === 'done' && (
          <div className="ts-center">
            <span className="cp-done" aria-hidden="true"><svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="#fff" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round"><path d="M6 12.5l4 4L18 8" /></svg></span>
            <h3>{subscription.name} 구독이 해지되었어요</h3>
            <p>다음 결제부터 청구되지 않아요.</p>
            <button type="button" className="ts-btn ts-btn--full" onClick={() => onComplete(subscription, result)}>확인</button>
          </div>
        )}
      </section>
    </div>
  );
}
