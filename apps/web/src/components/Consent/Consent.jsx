import { useEffect, useState } from 'react';
import wonnieImg from '../../assets/wonnie-3d.png';
import { CUSTOMER } from '../../data/subscriptions.js';
import './Consent.css';

const ITEMS = [
  { id: 'card', required: true, title: '카드 결제내역 조회', desc: '우리카드 최근 3개월 결제내역에서 정기결제를 찾아요' },
  { id: 'use', required: true, title: '구독 분석을 위한 정보 이용', desc: '찾은 구독은 구독 현황과 동의한 주요 알림에만 써요' },
];

const Check = ({ on }) => (
  <span className={`cs-check ${on ? 'is-on' : ''}`} aria-hidden="true">
    <svg viewBox="0 0 20 20" width="14" height="14" fill="none" stroke="#fff" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round"><path d="M5 10.5l3.2 3L15 7" /></svg>
  </span>
);

// 동의 폼 (홈 위 바텀시트와 상담 화면 안 카드에서 같이 사용)
export function ConsentForm({ onAgree, onLater, compact = false, disabled = false, saving = false, error = null }) {
  const [checked, setChecked] = useState({ card: false, use: false });
  const all = ITEMS.every((i) => checked[i.id]);
  const requiredOk = ITEMS.filter((i) => i.required).every((i) => checked[i.id]);

  return (
    <div className={`cs-form ${compact ? 'is-compact' : ''}`}>
      <button type="button" className="cs-all" disabled={disabled}
        onClick={() => setChecked(Object.fromEntries(ITEMS.map((i) => [i.id, !all])))}>
        <Check on={all} />
        <strong>전체 동의</strong>
      </button>
      <p className="cs-note">먼저 내 구독을 찾아볼게요. 알림 받을지는 조회 후 워니가 따로 여쭤볼게요.</p>
      <ul className="cs-list">
        {ITEMS.map((i) => (
          <li key={i.id}>
            <button type="button" disabled={disabled} onClick={() => setChecked((c) => ({ ...c, [i.id]: !c[i.id] }))} aria-pressed={checked[i.id]}>
              <Check on={checked[i.id]} />
              <span>
                <b><em className={i.required ? 'req' : 'opt'}>[{i.required ? '필수' : '선택'}]</em> {i.title}</b>
                {!compact && <small>{i.desc}</small>}
              </span>
            </button>
          </li>
        ))}
      </ul>
      <button type="button" className="cs-agree" disabled={!requiredOk || disabled} onClick={() => onAgree(checked)}>
        {disabled && !saving ? '동의 완료' : saving ? '저장 중…' : '동의하고 내 구독 찾기'}
      </button>
      {error && <p role="alert" className="cs-error">{error}</p>}
      {onLater && !disabled && <button type="button" className="cs-later" onClick={onLater}>다음에 할게요</button>}
    </div>
  );
}

// 앱 진입 시 홈 위로 올라오는 동의 바텀시트
export default function ConsentSheet({ open, onAgree, onClose, saving = false, error = null }) {
  const [show, setShow] = useState(false);
  useEffect(() => {
    if (!open) { setShow(false); return undefined; }
    const t = setTimeout(() => setShow(true), 30);
    return () => clearTimeout(t);
  }, [open]);
  if (!open) return null;

  return (
    <div className={`cs-sheet ${show ? 'is-open' : ''}`} role="dialog" aria-modal="true" aria-labelledby="cs-title">
      <div className="cs-dim" onClick={onClose} />
      <section className="cs-panel">
        <div className="cs-grabber" />
        <div className="cs-hero">
          <div>
            <span className="cs-badge">워니 구독 지킴이</span>
            <h2 id="cs-title">{CUSTOMER.name}님, 매달 새는<br />구독료 찾아드릴까요?</h2>
            <p>구독을 한눈에 확인하고, 중요한 소식만 골라 받아보세요.</p>
          </div>
          <img src={wonnieImg} alt="" className="cs-mascot" />
        </div>
        <ConsentForm onAgree={onAgree} onLater={onClose} disabled={saving} saving={saving} error={error} />
      </section>
    </div>
  );
}
