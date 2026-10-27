import wonnieImg from '../../assets/wonnie-3d.png';
import wonnieFace from '../../assets/wonnie-3d-face.png';
import { engine } from '../../proposals/index.js';
import { CUSTOMER } from '../../data/subscriptions.js';

/*
 * 시나리오별 절약 제안 (3.agent-proposals 통합)
 *  - ProposalCard   : 오늘의 제안 카드 (근거 · 예상 금액 · 바로 물어볼 질문)
 *  - ProposalReply  : 엔진이 고른 예시 답변 (+ 비교표, 이어서 물어볼 질문)
 *  - ScenarioSheet  : 시나리오 직접 고르기 (폰 화면 안 바텀시트)
 */

function Chips({ labels, onPick, disabled, tab }) {
  if (!labels?.length) return null;
  return (
    <div className="wp-chips">
      {labels.map((label) => (
        <button key={label} type="button" disabled={disabled} onClick={() => onPick(label)} tabIndex={tab}>{label}</button>
      ))}
    </div>
  );
}

export function ProposalCard({ scenarioId, description, latest, busy, onAsk, onNext, onPick, tab }) {
  if (!scenarioId) {
    return (
      <article className="wp-card">
        <h2 className="wp-done">오늘의 제안을 모두 확인했어요</h2>
        <p className="wp-desc">유지하기로 한 제안은 다시 권하지 않을게요. 시나리오 직접 선택에서 언제든 다시 확인할 수 있어요.</p>
        <div className="wp-controls">
          <button type="button" onClick={onPick} tabIndex={tab}>시나리오 직접 선택</button>
        </div>
      </article>
    );
  }
  const s = engine.get(scenarioId);
  return (
    <div className="wp-wrap">
      <article className="wp-card" aria-label="오늘의 절약 제안" data-scenario={s.id}>
        <div className="wp-eyebrow"><span>{s.category}</span><span className="wp-pill">시연 데이터</span></div>
        <div className="wp-intro">
          <h2>{s.title}</h2>
          <img src={wonnieImg} alt="WONNIE" />
        </div>
        <p className="wp-desc">{description}</p>
        <div className="wp-evidence">{s.evidence.map((t) => <span key={t}>{t}</span>)}</div>
        <div className="wp-metric">
          <div><span>{s.metric.label}</span><strong>{s.metric.value}</strong></div>
          <small>{s.metric.detail}</small>
        </div>
        <Chips labels={s.prompts.map((p) => p.label)} onPick={onAsk} disabled={busy || !latest} tab={tab} />
      </article>
      {latest && (
        <>
          <div className="wp-controls">
            <button type="button" onClick={onNext} tabIndex={tab}>다른 제안 보기 ↻</button>
            <button type="button" onClick={onPick} tabIndex={tab}>시나리오 직접 선택</button>
          </div>
          <p className="wp-note">금액·카드·서비스 조건은 모두 시연용 예시입니다.</p>
        </>
      )}
    </div>
  );
}

export function ProposalReply({ answer, latest, busy, onAsk, tab }) {
  const s = engine.get(answer.scenarioId);
  return (
    <div className="wp-reply">
      <div className="wp-label"><img src={wonnieFace} alt="" />WONNIE · {s.category}</div>
      <div className="wp-bubble">
        <p>{answer.text}</p>
        {answer.rows.length > 0 && (
          <table className="wp-table">
            <caption>시연용 예시 비교</caption>
            <tbody>
              {answer.rows.map(([k, v]) => <tr key={k}><th scope="row">{k}</th><td>{v}</td></tr>)}
            </tbody>
          </table>
        )}
      </div>
      {latest && <Chips labels={answer.followUps} onPick={onAsk} disabled={busy} tab={tab} />}
    </div>
  );
}

export function ScenarioSheet({ open, activeId, mode, onClose, onPickSubs, onPickProposal }) {
  if (!open) return null;
  return (
    <div className="wp-sheet" role="dialog" aria-modal="true" aria-labelledby="wp-sheet-title">
      <button type="button" className="wp-sheet__dim" aria-label="닫기" onClick={onClose} />
      <div className="wp-sheet__panel">
        <div className="wp-sheet__grab" />
        <div className="wp-sheet__head">
          <h2 id="wp-sheet-title">시연할 시나리오를 골라보세요</h2>
          <button type="button" className="wp-sheet__x" aria-label="닫기" onClick={onClose}>×</button>
        </div>

        <div className="wp-list">
          <span className="wp-list__group">기본 데모</span>
          <button type="button" className={mode === 'subs' ? 'is-active' : ''} onClick={onPickSubs}>
            {CUSTOMER.name}({CUSTOMER.age}세) 구독 절약 데모
            <small>구독 찾기 → D-1 알림 → 해지 → 적금 이체</small>
          </button>
          <span className="wp-list__group">절약 제안 시나리오</span>
          {engine.scenarios.map((s, i) => (
            <button key={s.id} type="button" className={mode === 'proposal' && s.id === activeId ? 'is-active' : ''} onClick={() => onPickProposal(s.id)}>
              {i + 1}. {s.name}
              <small>{s.service}{engine.isMuted(s.id) ? ' · 자동 추천에서 제외됨' : ''}</small>
            </button>
          ))}
          <p className="wp-sheet__note">실제 금융·서비스 계정은 연결하지 않은 대화 시연이에요.</p>
        </div>
      </div>
    </div>
  );
}
