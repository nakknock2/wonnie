import { useEffect, useState } from 'react';
import { WATCHA, won, mdw, billDateOf } from '../../data/subscriptions.js';

/*
 * 구독 해지 페이지 (모의 화면)
 * 실제 서비스에서는 해당 OTT 의 해지 페이지(앱/웹)로 딥링크합니다.
 * 데모에서는 실제 회사 화면을 흉내 내지 않고, 은행 앱 안 '연결된 페이지'처럼 중립적으로 보여줘요.
 */
export default function CancelPage({ open, onClose, onCancelled, onAuthenticate }) {
  const [step, setStep] = useState('plan'); // plan → confirm → working → done
  const [reason, setReason] = useState(null);
  const [authenticating, setAuthenticating] = useState(false);

  useEffect(() => { if (open) { setStep('plan'); setReason(null); setAuthenticating(false); } }, [open]);

  useEffect(() => {
    if (step !== 'working') return undefined;
    const t = setTimeout(() => { setStep('done'); onCancelled(); }, 1100);
    return () => clearTimeout(t);
  }, [step, onCancelled]);

  const bill = billDateOf(WATCHA);
  const confirmCancellation = async () => {
    if (authenticating) return;
    setAuthenticating(true);
    try {
      const authenticated = await onAuthenticate?.();
      if (authenticated === false) return;
      setStep('working');
    } finally {
      setAuthenticating(false);
    }
  };

  return (
    <div className={`cp ${open ? 'is-open' : ''}`} role="dialog" aria-modal="true" aria-label={`${WATCHA.name} 구독 해지`} aria-hidden={!open}>
      <div className="cp-bar">
        <button type="button" className="cp-x" aria-label="닫기" disabled={authenticating} onClick={onClose} tabIndex={open ? 0 : -1}>
          <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"><path d="M4 4l12 12M16 4 4 16" /></svg>
        </button>
        <div className="cp-url">
          <svg viewBox="0 0 16 16" width="11" height="11" fill="none" stroke="currentColor" strokeWidth="1.6"><rect x="3" y="7" width="10" height="7" rx="1.5" /><path d="M5.5 7V5a2.5 2.5 0 0 1 5 0v2" /></svg>
          {WATCHA.name} · 구독 관리
        </div>
        <span style={{ width: 36 }} />
      </div>
      <div className="cp-demo">데모용 모의 화면이에요 · 실제 해지는 {WATCHA.name} 앱·웹에서 진행돼요</div>

      <div className="cp-body">
        {(step === 'plan' || step === 'confirm') && (
          <>
            <h2>구독 해지</h2>
            <div className="cp-plan">
              <span className="cp-logo">{WATCHA.letter}</span>
              <div>
                <strong>{WATCHA.name} {WATCHA.plan}</strong>
                <small>월 {won(WATCHA.price)} · {WATCHA.card}</small>
              </div>
            </div>
            <dl className="cp-info">
              <div><dt>다음 결제일</dt><dd>{mdw(bill)}</dd></div>
              <div><dt>다음 결제 금액</dt><dd>{won(WATCHA.price)}</dd></div>
              <div><dt>마지막 시청</dt><dd>{WATCHA.lastUsed}</dd></div>
            </dl>

            <p className="cp-q">해지하시는 이유를 알려주세요 (선택)</p>
            <div className="cp-reasons">
              {['잘 안 보게 돼요', '요금이 부담돼요', '다른 서비스를 써요', '기타'].map((r) => (
                <button key={r} type="button" className={reason === r ? 'is-on' : ''} onClick={() => setReason(r)} tabIndex={open ? 0 : -1}>{r}</button>
              ))}
            </div>
          </>
        )}

        {step === 'working' && (
          <div className="cp-center">
            <span className="cp-spinner" aria-hidden="true" />
            <p>해지를 처리하고 있어요</p>
          </div>
        )}

        {step === 'done' && (
          <div className="cp-center">
            <span className="cp-done" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="#fff" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round"><path d="M6 12.5l4 4L18 8" /></svg>
            </span>
            <h3>구독이 해지되었어요</h3>
            <p>{mdw(bill)} 결제부터 청구되지 않아요.</p>
          </div>
        )}
      </div>

      <div className="cp-foot">
        {step === 'plan' && <button type="button" className="cp-btn cp-btn--danger" onClick={() => setStep('confirm')} tabIndex={open ? 0 : -1}>구독 해지하기</button>}
        {step === 'done' && <button type="button" className="cp-btn" onClick={onClose}>워니로 돌아가기</button>}
      </div>

      {step === 'confirm' && (
        <div className="cp-modal" role="alertdialog" aria-labelledby="cp-confirm-title">
          <div className="cp-modal__box">
            <h3 id="cp-confirm-title">정말 해지할까요?</h3>
            <p>해지하면 {mdw(bill)} 결제({won(WATCHA.price)})가<br />청구되지 않아요.</p>
            <div className="cp-modal__btns">
              <button type="button" disabled={authenticating} onClick={() => setStep('plan')}>취소</button>
              <button type="button" className="is-danger" disabled={authenticating} onClick={confirmCancellation}>Face ID로 해지</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
