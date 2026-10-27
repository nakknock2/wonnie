import { BASE_DATE, CUSTOMER, addDays, mdw } from '../data/subscriptions.js';
import { STEPS } from './useDemo.js';
import { ScanFace } from 'lucide-react';
import './DemoPanel.css';

// 폰 바깥 시연 패널: 데모 고객 · 데모 날짜 · 진행 체크리스트 · 진행 버튼
export default function DemoPanel({ demo, canRemind, onRemind, canPrice, onPrice, onAdvance, onRestart, onScenarios, onFaceId, faceIdActive }) {
  const current = STEPS.findIndex((s) => !s.done(demo));
  return (
    <aside className="dp" aria-label="데모 진행 패널">
      <div className="dp-head">
        <span className="dp-tag">DEMO</span>
        <strong>{CUSTOMER.name}({CUSTOMER.age}세)</strong>
        <span className="dp-date">📅 {mdw(addDays(BASE_DATE, demo.day))}</span>
      </div>

      <ol className="dp-steps">
        {STEPS.map((s, i) => (
          <li key={s.key} className={s.done(demo) ? 'is-done' : i === current ? 'is-now' : ''}>
            <span className="dp-dot">{s.done(demo) ? '✓' : i + 1}</span>
            {s.label}
          </li>
        ))}
      </ol>

      <div className="dp-btns">
        <button type="button" className="dp-face-id" onClick={onFaceId} disabled={faceIdActive}
          aria-label="Face ID 테스트" title="Face ID 화면 시연 · 실제 얼굴 인증이나 결제를 실행하지 않아요">
          <ScanFace size={20} aria-hidden="true" /><b>Face ID 테스트</b>
        </button>
        <button type="button" onClick={onRemind} disabled={!canRemind} title="결제 하루 전 알림에 동의 + 내일 결제 건이 있을 때만">
          <span>🔔</span><b>D-1 알림 보내기</b>
        </button>
        <button type="button" onClick={onPrice} disabled={!canPrice} title="구독료 인상·변경 알림에 동의했을 때만">
          <span>📈</span><b>구독료 인상 알림</b>
        </button>
        <button type="button" onClick={onAdvance} title="데모 날짜를 하루 넘겨요">
          <span>📅</span><b>하루 넘기기</b>
        </button>
        <button type="button" className="dp-alt" onClick={onScenarios} title="팀 시나리오별 절약 제안(6종) 시연">
          <span>🧩</span><b>절약 제안 시나리오</b>
        </button>
        <button type="button" className="dp-ghost" onClick={onRestart} title="처음 상태로">
          <span>↺</span><b>처음부터</b>
        </button>
      </div>
      {demo.notif.asked && (
        <p className="dp-notif">
          알림 동의: 결제 하루 전 <b className={demo.notif.d1 ? 'on' : ''}>{demo.notif.d1 ? 'ON' : 'OFF'}</b>
          {' · '}구독료 인상 <b className={demo.notif.price ? 'on' : ''}>{demo.notif.price ? 'ON' : 'OFF'}</b>
        </p>
      )}
      <p className="dp-note">동의한 알림만, 중요한 일이 있는 날 13~14시에 1회 · 매일 발송 없음. 실제 해지·이체 없는 모의 시연이에요</p>
    </aside>
  );
}
