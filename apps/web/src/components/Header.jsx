import { WalletIcon, AiIcon, BellIcon, MenuIcon } from './Icons.jsx';

// 상단 헤더: 일반홈/쉬운 토글 + 아이콘 버튼 4개
export default function Header({ easyMode, onToggleMode, onAction, hasNotice }) {
  return (
    <header className="app-header">
      <div className="mode-toggle" role="tablist" aria-label="홈 모드">
        <button
          role="tab"
          aria-selected={!easyMode}
          className={`mode-toggle__btn ${!easyMode ? 'is-active' : ''}`}
          onClick={() => onToggleMode(false)}
        >
          일반홈
        </button>
        <button
          role="tab"
          aria-selected={easyMode}
          className={`mode-toggle__btn ${easyMode ? 'is-active' : ''}`}
          onClick={() => onToggleMode(true)}
        >
          쉬운
        </button>
      </div>

      <div className="header-icons">
        <button className="icon-btn" aria-label="지갑" onClick={() => onAction('지갑')}>
          <WalletIcon />
        </button>
        <button className="icon-btn" aria-label="AI 상담" onClick={() => onAction('AI 상담')}>
          <AiIcon />
        </button>
        <button className="icon-btn" aria-label="알림" onClick={() => onAction('알림')}>
          <BellIcon />
          {hasNotice && <span className="badge-dot" />}
        </button>
        <button className="icon-btn" aria-label="전체 메뉴" onClick={() => onAction('전체 메뉴')}>
          <MenuIcon />
        </button>
      </div>
    </header>
  );
}
