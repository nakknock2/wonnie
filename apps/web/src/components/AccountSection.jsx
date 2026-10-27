import { useState } from 'react';
import { CloseIcon, CopyIcon, MoreIcon } from './Icons.jsx';

// 다른 은행 계좌 연결 안내 카드
export function OtherBankCard({ banks, onClose, onAdd }) {
  return (
    <div className="card other-bank">
      <button className="close-btn other-bank__close" aria-label="안내 닫기" onClick={onClose}>
        <CloseIcon />
      </button>

      <div className="bank-stack">
        {banks.map((b) => (
          <span key={b.id} className="bank-chip" style={{ background: b.bg, color: b.color }}>
            {b.label}
          </span>
        ))}
        <span className="bank-chip bank-chip--more">•••</span>
      </div>

      <p className="other-bank__text">
        다른 은행 계좌도 홈에서 확인할 수 있어요
        <br />
        필요한 계좌를 홈에 추가해 보세요
      </p>

      <button className="btn-primary" onClick={onAdd}>
        + 다른 은행 계좌 추가
      </button>
    </div>
  );
}

// 대표 계좌 카드
export function AccountCard({ account, hideBalance, onCopy, onTransfer, onMenu, onRevealOnce }) {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <div className="card account">
      <div className="account__logo" aria-hidden="true">
        {account.logoSrc ? (
          <img src={account.logoSrc} alt="" width="24" height="24" className="account__logo-img" />
        ) : (
          <svg viewBox="0 0 40 40" width="40" height="40">
            <circle cx="20" cy="20" r="18" fill="#2d82ef" />
            <path d="M11 26 L20 12 L29 26 Z" fill="#fff" />
            <path d="M16 26 L20 19.5 L24 26 Z" fill="#7fb6ff" />
          </svg>
        )}
      </div>

      <div className="account__body">
        <div className="account__top">
          <h3 className="account__name">{account.name}</h3>
          <div className="account__menu-wrap">
            <button className="icon-btn icon-btn--sm" aria-label="계좌 메뉴" onClick={() => setMenuOpen((v) => !v)}>
              <MoreIcon />
            </button>
            {menuOpen && (
              <ul className="popover" onMouseLeave={() => setMenuOpen(false)}>
                {['계좌 상세', '계좌 별명 변경', '홈 화면에서 숨기기'].map((m) => (
                  <li key={m}>
                    <button onClick={() => { setMenuOpen(false); onMenu(m); }}>{m}</button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>

        <button className="account__number" onClick={() => onCopy(account.number)}>
          {account.bank} {account.number}
          <CopyIcon />
        </button>

        <div className="account__tags">
          {account.tags.map((t) => (
            <span key={t} className="tag">{t}</span>
          ))}
        </div>

        <div className="account__bottom">
          <button className="account__balance" onClick={onRevealOnce}>
            {hideBalance ? (
              <span className="balance balance--hidden">잔액 숨김</span>
            ) : (
              <span className="balance">
                {account.balance.toLocaleString('ko-KR')}
                <small>원</small>
              </span>
            )}
          </button>
          <button className="btn-outline" onClick={onTransfer}>이체</button>
        </div>
      </div>
    </div>
  );
}
