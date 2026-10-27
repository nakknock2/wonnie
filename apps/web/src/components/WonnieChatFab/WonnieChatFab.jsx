import { useEffect, useRef, useState } from 'react';
import Wonnie from '../Wonnie';
import { WONNIE } from '../../data/wonnieData.js';
import './WonnieChatFab.css';

/*
 * 라벨 스타일(variant)
 *  - 'extended' : 캐릭터 왼쪽 2줄 카드 ("궁금한 점이 있으면 / 워니에게 물어보세요")
 *  - 'compact'  : 캐릭터 왼쪽 1줄 작은 pill ("워니에게 물어보세요")
 *  - 'auto'     : extended(2줄 카드)로 시작 → 4초 뒤 또는 스크롤하면 캐릭터만 남기고 접힘, 마우스를 올리면 다시 펼침
 *  - 'mini'     : 캐릭터만 + 모서리에 작은 'AI' 배지
 *  - 'blue' | 'gradient' | 'bubble' : 캐릭터 아래 'AI 상담' pill
 */
export const FAB_LABEL_VARIANTS = ['extended', 'compact', 'auto', 'mini', 'blue', 'gradient', 'bubble'];

const SIDE_CARD = ['extended', 'compact', 'auto'];

// AI 상담 플로팅 버튼: 폰 화면 기준 absolute 로 탭바 위 우측 하단에 고정
// nudge: 구독 결제 알림이 왔을 때 true → 카드 없이 캐릭터만 남고, 빨간 배지 + 워니 자세 변화(종 들고 깡충), 누르면 onOpen('reminder')
export default function WonnieChatFab({ hidden = false, onOpen, variant = 'auto', showBubble = true, collapsed: forceCollapsed, nudge = false }) {
  const [bouncing, setBouncing] = useState(false);
  const [bubble, setBubble] = useState('show'); // show → hide → off
  const [autoCollapsed, setAutoCollapsed] = useState(false);
  const [hover, setHover] = useState(false);
  const timers = useRef([]);
  const btnRef = useRef(null);

  // 화면 진입 시 말풍선 3초 노출 후 사라짐
  useEffect(() => {
    timers.current.push(setTimeout(() => setBubble('hide'), 3000));
    timers.current.push(setTimeout(() => setBubble('off'), 3400));
    return () => timers.current.forEach(clearTimeout);
  }, []);

  // 'auto': 4초 뒤 또는 폰 화면 안에서 스크롤이 일어나면 접힘
  useEffect(() => {
    if (variant !== 'auto') return undefined;
    const t = setTimeout(() => setAutoCollapsed(true), 4000);
    const screen = btnRef.current?.parentElement;
    const onScroll = () => setAutoCollapsed(true);
    // scroll 이벤트는 버블링되지 않으므로 capture 로 폰 화면 안의 스크롤을 감지
    screen?.addEventListener('scroll', onScroll, { capture: true, passive: true });
    return () => {
      clearTimeout(t);
      screen?.removeEventListener('scroll', onScroll, { capture: true });
    };
  }, [variant]);

  const handleClick = () => {
    if (bouncing) return;
    setBouncing(true);
    timers.current.push(
      setTimeout(() => {
        setBouncing(false);
        onOpen(nudge ? 'reminder' : 'default');
      }, 420)
    );
  };

  const collapsed =
    forceCollapsed ?? (nudge || (variant === 'auto' && autoCollapsed && !hover)); // 알림 중엔 옆 카드 숨김

  const classes = [
    'wonnie-fab',
    `wonnie-fab--${variant}`,
    SIDE_CARD.includes(variant) && 'wonnie-fab--side',
    collapsed && 'is-collapsed',
    nudge && 'is-nudge',
    hidden && 'is-hidden',
  ].filter(Boolean).join(' ');

  const renderLabel = () => {
    if (variant === 'extended' || variant === 'auto') {
      return (
        <span className="wonnie-fab__label" aria-hidden={collapsed}>
          <small>{WONNIE.fabCardSub}</small>
          <strong>
            <em>{WONNIE.name}</em>
            {WONNIE.fabCardMain}
          </strong>
        </span>
      );
    }
    if (variant === 'compact') {
      return (
        <span className="wonnie-fab__label" aria-hidden={collapsed}>
          <strong>
            <em>{WONNIE.name}</em>
            {WONNIE.fabCardMain}
          </strong>
        </span>
      );
    }
    if (variant === 'mini') {
      return <span className="wonnie-fab__label">AI</span>;
    }
    return (
      <span className="wonnie-fab__label">
        <svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
          <path d="M8 1l1.7 4.3L14 7l-4.3 1.7L8 13 6.3 8.7 2 7l4.3-1.7Z" fill="currentColor" />
        </svg>
        {WONNIE.fabLabel}
      </span>
    );
  };

  return (
    <button
      ref={btnRef}
      type="button"
      className={classes}
      aria-label={nudge ? '워니 주요 알림: 구독 관련 중요한 소식이 있어요. 눌러서 확인' : 'AI 상담 챗봇 워니 열기'}
      tabIndex={hidden ? -1 : 0}
      onClick={handleClick}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      onFocus={() => setHover(true)}
      onBlur={() => setHover(false)}
    >
      {showBubble && !SIDE_CARD.includes(variant) && bubble !== 'off' && !hidden && (
        <span className={`wonnie-fab__bubble ${bubble === 'hide' ? 'is-out' : ''}`} aria-hidden="true">
          금융 고민, <b>워니</b>에게 상담하세요!
        </span>
      )}

      <span className="wonnie-fab__body">
        <span className="wonnie-fab__circle" aria-hidden="true" />
        {nudge && <span className="wonnie-fab__badge" aria-hidden="true">1</span>}
        {nudge && (
          <span className="wonnie-fab__alert" aria-hidden="true">
            {/* 머리 위 느낌표 + 손에 든 종 */}
            <svg className="wonnie-fab__mark" viewBox="0 0 14 30" width="9" height="19">
              <rect x="3" y="0" width="8" height="19" rx="4" fill="#f04452" />
              <circle cx="7" cy="25.5" r="4" fill="#f04452" />
            </svg>
            <svg className="wonnie-fab__bell" viewBox="0 0 30 32" width="22" height="24">
              <defs>
                <radialGradient id="fab-bell" cx="0.35" cy="0.3" r="0.8">
                  <stop offset="0" stopColor="#fff6c4" />
                  <stop offset="0.5" stopColor="#ffcf33" />
                  <stop offset="1" stopColor="#d39200" />
                </radialGradient>
              </defs>
              <path d="M15 2 v3" stroke="#b97d06" strokeWidth="2.4" strokeLinecap="round" />
              <path d="M6 22 C6 12 9 6 15 6 C21 6 24 12 24 22 L27 26 H3 Z" fill="url(#fab-bell)" stroke="#c48a06" strokeWidth="1.2" strokeLinejoin="round" />
              <circle cx="15" cy="28.5" r="3" fill="#d39200" />
              <path d="M10 12 Q11 9 14 8.5" stroke="#fff" strokeWidth="1.8" fill="none" strokeLinecap="round" opacity="0.85" />
            </svg>
          </span>
        )}
        <span className={`wonnie-fab__squash ${bouncing ? 'is-bouncing' : ''}`}>
          <Wonnie size={62} animated shadow={false} face={nudge ? 'wow' : 'default'} />
        </span>
      </span>

      {renderLabel()}
    </button>
  );
}
