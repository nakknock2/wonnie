import { useState } from 'react';
import { NOTIF_TYPES } from '../../data/subscriptions.js';

/*
 * 상담 화면 안 '알림 받기 동의' 카드
 *  - 알림 종류별로 따로 고르는 선택 동의(기본 꺼짐)
 *  - 받는 시간·안 보내는 것·철회 방법을 함께 안내
 *  - 동의 전문은 펼쳐서 확인
 */
const Toggle = ({ on }) => (
  <span className={`nc-toggle ${on ? 'is-on' : ''}`} aria-hidden="true"><i /></span>
);

export default function NotifConsent({ initial, locked, onSubmit }) {
  const [sel, setSel] = useState({ d1: !!initial?.d1, price: !!initial?.price });
  const [detail, setDetail] = useState(false);
  const count = Object.values(sel).filter(Boolean).length;

  return (
    <div className={`nc ${locked ? 'is-locked' : ''}`}>
      <div className="nc-head">
        <span className="nc-badge">선택 동의</span>
        <strong>워니 알림, 꼭 필요한 것만 보내드릴게요</strong>
        <small>받고 싶은 알림을 골라 주세요. 고르지 않으면 아무 알림도 보내지 않아요.</small>
      </div>

      <div className="nc-options">
        {NOTIF_TYPES.map((t) => (
          <button
            key={t.id}
            type="button"
            className={`nc-opt ${sel[t.id] ? 'is-on' : ''}`}
            role="switch"
            aria-checked={sel[t.id]}
            disabled={locked}
            onClick={() => setSel((s) => ({ ...s, [t.id]: !s[t.id] }))}
          >
            <span className="nc-opt__copy">
              <b>{t.title}</b>
              <small>{t.desc}</small>
              <em>{t.example}</em>
            </span>
            <Toggle on={sel[t.id]} />
          </button>
        ))}
      </div>

      <ul className="nc-facts">
        <li><span>⏰</span>받는 시간 <b>오후 1~2시, 하루 최대 1번</b></li>
        <li><span>🚫</span>매일 보내는 알림·광고 알림은 <b>없어요</b></li>
        <li><span>⚙️</span>"알림 설정"이라고 말하면 언제든 바꾸거나 끌 수 있어요</li>
      </ul>

      <button type="button" className="nc-detail-btn" onClick={() => setDetail((v) => !v)} aria-expanded={detail}>
        [선택] 알림 수신 및 정보 이용 동의 {detail ? '접기' : '전문 보기'}
      </button>
      {detail && (
        <dl className="nc-detail">
          <div><dt>목적</dt><dd>결제 예정·구독료 변경 안내</dd></div>
          <div><dt>이용 항목</dt><dd>구독 서비스명, 결제 금액·결제일, 카드 결제내역</dd></div>
          <div><dt>보유 기간</dt><dd>동의 철회 또는 서비스 해지 시까지</dd></div>
          <div><dt>거부 권리</dt><dd>동의하지 않아도 구독 조회는 그대로 이용할 수 있어요</dd></div>
        </dl>
      )}

      {!locked && (
        <div className="nc-btns">
          <button type="button" className="nc-btn nc-btn--ghost" onClick={() => onSubmit({ d1: false, price: false })}>
            알림 안 받을게요
          </button>
          <button type="button" className="nc-btn" disabled={count === 0} onClick={() => onSubmit(sel)}>
            {count === 0 ? '알림을 골라 주세요' : `선택한 알림 받기 (${count})`}
          </button>
        </div>
      )}
    </div>
  );
}

// 기기 알림 권한 요청 (iOS 시스템 팝업 형태의 모의 화면)
export function OsPermission({ open, onAnswer }) {
  if (!open) return null;
  return (
    <div className="osp" role="alertdialog" aria-modal="true" aria-labelledby="osp-title">
      <div className="osp-box">
        <div className="osp-body">
          <h3 id="osp-title">'우리WON뱅킹'에서 알림을 보내고자 합니다.</h3>
          <p>경고, 사운드 및 아이콘 배지가 알림에 포함될 수 있습니다. 설정에서 이를 구성할 수 있습니다.</p>
        </div>
        <div className="osp-btns">
          <button type="button" onClick={() => onAnswer(false)}>허용 안 함</button>
          <button type="button" className="is-strong" onClick={() => onAnswer(true)}>허용</button>
        </div>
      </div>
    </div>
  );
}
