import { WalletIcon, AiIcon, BellIcon, MenuIcon } from './Icons.jsx';

// 상단 헤더: 일반홈/쉬운 토글 + 아이콘 버튼 4개
export default function Header({
  easyMode,
  onToggleMode,
  onAction,
  hasNotice,
  menuOpen,
  onToggleMenu,
  onCloseMenu,
  user,
  onLogout,
}) {
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
        <div className="header-menu-wrap">
          <button
            className="icon-btn"
            aria-label="전체 메뉴"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            onClick={onToggleMenu}
          >
            <MenuIcon />
          </button>

          {menuOpen && (
            <ul className="popover header-account-popover" role="menu" onMouseLeave={onCloseMenu}>
              <li className="header-account-popover__profile">
                {user?.picture ? (
                  <img className="header-account-popover__avatar" src={user.picture} alt="" />
                ) : (
                  <span className="header-account-popover__avatar header-account-popover__avatar--fallback">
                    {(user?.name || 'W').slice(0, 1)}
                  </span>
                )}
                <span className="header-account-popover__user">
                  <strong>{user?.name || 'WONNIE 사용자'}</strong>
                  {user?.email && <small>{user.email}</small>}
                </span>
              </li>
              <li className="header-account-popover__divider" aria-hidden="true" />
              <li>
                <button className="header-account-popover__logout" role="menuitem" onClick={onLogout}>
                  로그아웃
                </button>
              </li>
            </ul>
          )}
        </div>
      </div>
    </header>
  );
}
