import { useEffect, useState } from 'react';
import { ACCOUNT, SAVINGS, won } from '../../data/subscriptions.js';

// 적금 모의 이체 바텀시트 (실제 돈은 움직이지 않아요)
export default function TransferSheet({ open, amount, balance, onClose, onDone }) {
  const [step, setStep] = useState('confirm'); // confirm → sending → done

  useEffect(() => { if (open) setStep('confirm'); }, [open]);
  useEffect(() => {
    if (step !== 'sending') return undefined;
    const t = setTimeout(() => setStep('done'), 1200);
    return () => clearTimeout(t);
  }, [step]);

  return (
    <div className={`ts ${open ? 'is-open' : ''}`} aria-hidden={!open}>
      <div className="ts-dim" onClick={step === 'confirm' ? onClose : undefined} />
      <section className="ts-panel" role="dialog" aria-modal="true" aria-labelledby="ts-title">
        <div className="ts-grabber" />
        {step === 'confirm' && (
          <>
            <h2 id="ts-title">아낀 돈, 적금으로 옮길게요</h2>
            <div className="ts-amount">{won(amount)}</div>
            <div className="ts-route">
              <div className="ts-acct">
                <small>보내는 계좌</small>
                <strong>{ACCOUNT.name}</strong>
                <span>{ACCOUNT.number} · 잔액 {won(balance)}</span>
              </div>
              <span className="ts-arrow" aria-hidden="true">↓</span>
              <div className="ts-acct ts-acct--to">
                <small>받는 계좌</small>
                <strong>{SAVINGS.name} 🌱</strong>
                <span>{SAVINGS.number}</span>
              </div>
            </div>
            <p className="ts-mock">모의 이체예요 · 실제 돈은 움직이지 않아요</p>
            <div className="ts-btns">
              <button type="button" className="ts-btn ts-btn--ghost" onClick={onClose} tabIndex={open ? 0 : -1}>취소</button>
              <button type="button" className="ts-btn" onClick={() => setStep('sending')} tabIndex={open ? 0 : -1}>{won(amount)} 보내기</button>
            </div>
          </>
        )}
        {step === 'sending' && (
          <div className="ts-center">
            <span className="cp-spinner" aria-hidden="true" />
            <p>적금으로 보내고 있어요</p>
          </div>
        )}
        {step === 'done' && (
          <div className="ts-center">
            <span className="cp-done" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="34" height="34" fill="none" stroke="#fff" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round"><path d="M6 12.5l4 4L18 8" /></svg>
            </span>
            <h3>{SAVINGS.name}에 {won(amount)}을 넣었어요</h3>
            <p>{ACCOUNT.name} 잔액 {won(balance - amount)}</p>
            <button type="button" className="ts-btn ts-btn--full" onClick={() => onDone(amount)}>확인</button>
          </div>
        )}
      </section>
    </div>
  );
}
