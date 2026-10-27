// 앱에서 쓰는 라인 아이콘 모음 (직접 그린 SVG)
const base = {
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
};

export const WalletIcon = (p) => (
  <svg viewBox="0 0 28 28" width="28" height="28" {...base} {...p}>
    <path d="M5 8.5V21a2 2 0 0 0 2 2h15a1.5 1.5 0 0 0 1.5-1.5V10A1.5 1.5 0 0 0 22 8.5H5Z" />
    <path d="M5 8.5 19.5 5v3.5" />
    <circle cx="19" cy="15.8" r="1.3" fill="currentColor" stroke="none" />
  </svg>
);

export const AiIcon = (p) => (
  <svg viewBox="0 0 30 28" width="30" height="28" {...p}>
    <path
      d="M14 4.5a9.5 9.5 0 1 0 5.9 17l3.6 1.3-1-3.6A9.5 9.5 0 0 0 14 4.5Z"
      fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round"
    />
    <text x="14" y="18" textAnchor="middle" fontSize="8.5" fontWeight="800" fill="currentColor" fontFamily="inherit">AI</text>
    <path d="M24 1.5l1.3 3 3 1.3-3 1.3-1.3 3-1.3-3-3-1.3 3-1.3Z" fill="url(#aiGrad)" />
    <defs>
      <linearGradient id="aiGrad" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stopColor="#4a7bff" />
        <stop offset="1" stopColor="#b14dff" />
      </linearGradient>
    </defs>
  </svg>
);

export const BellIcon = (p) => (
  <svg viewBox="0 0 28 28" width="28" height="28" {...base} {...p}>
    <path d="M7 19.5V13a7 7 0 0 1 14 0v6.5l1.5 1.5h-17L7 19.5Z" />
    <path d="M11.5 23.5a2.6 2.6 0 0 0 5 0" />
  </svg>
);

export const MenuIcon = (p) => (
  <svg viewBox="0 0 28 28" width="28" height="28" {...base} {...p}>
    <path d="M5 7.5h18M5 14h18M5 20.5h18" />
  </svg>
);

export const CloseIcon = (p) => (
  <svg viewBox="0 0 20 20" width="18" height="18" {...base} strokeWidth="1.8" {...p}>
    <path d="M4 4l12 12M16 4 4 16" />
  </svg>
);

export const GearIcon = (p) => (
  <svg viewBox="0 0 24 24" width="18" height="18" {...p}>
    <path
      fill="currentColor"
      d="M13.7 2.5l.5 2.3c.6.2 1.2.5 1.7.9l2.2-.8 1.7 3-1.8 1.5c.1.6.1 1.3 0 1.9l1.8 1.5-1.7 3-2.2-.8c-.5.4-1.1.7-1.7.9l-.5 2.3h-3.4l-.5-2.3c-.6-.2-1.2-.5-1.7-.9l-2.2.8-1.7-3 1.8-1.5a6 6 0 0 1 0-1.9L4.2 7.9l1.7-3 2.2.8c.5-.4 1.1-.7 1.7-.9l.5-2.3h3.4ZM12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z"
      transform="translate(0 1)"
    />
  </svg>
);

export const CopyIcon = (p) => (
  <svg viewBox="0 0 20 20" width="18" height="18" {...base} strokeWidth="1.6" {...p}>
    <rect x="7" y="7" width="10" height="10" rx="2" />
    <path d="M13 4.5V4a1.5 1.5 0 0 0-1.5-1.5H4A1.5 1.5 0 0 0 2.5 4v7.5A1.5 1.5 0 0 0 4 13h.5" />
  </svg>
);

export const MoreIcon = (p) => (
  <svg viewBox="0 0 6 24" width="6" height="22" {...p}>
    <circle cx="3" cy="4" r="2.2" fill="currentColor" />
    <circle cx="3" cy="12" r="2.2" fill="currentColor" />
    <circle cx="3" cy="20" r="2.2" fill="currentColor" />
  </svg>
);

export const ChevronRight = (p) => (
  <svg viewBox="0 0 20 20" width="18" height="18" {...base} strokeWidth="2" {...p}>
    <path d="M7.5 4.5 13 10l-5.5 5.5" />
  </svg>
);

/* ---------- 하단 탭 아이콘 ---------- */
export const TabIcons = {
  product: (p) => (
    <svg viewBox="0 0 28 28" width="28" height="28" {...base} {...p}>
      <rect x="4.5" y="5" width="19" height="18.5" rx="3.5" />
      <path d="M10.5 10.5a3.5 3.5 0 0 0 7 0" />
    </svg>
  ),
  asset: (p) => (
    <svg viewBox="0 0 28 28" width="28" height="28" {...base} {...p}>
      <path d="M12.5 5.5a9 9 0 1 0 10 10h-10v-10Z" />
      <path d="M16 3a9 9 0 0 1 9 9h-9V3Z" />
    </svg>
  ),
  home: (p) => (
    <svg viewBox="0 0 28 28" width="28" height="28" {...p}>
      <path d="M14 3 25 9.5V21a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V9.5L14 3Z" fill="currentColor" />
      <circle cx="14" cy="15" r="1.8" fill="#fff" />
    </svg>
  ),
  stock: (p) => (
    <svg viewBox="0 0 28 28" width="28" height="28" {...base} {...p}>
      <rect x="4" y="4.5" width="20" height="19" rx="3" />
      <path d="m8 17 4-4.5 3.5 3L20 10" />
    </svg>
  ),
  benefit: (p) => (
    <svg viewBox="0 0 28 28" width="28" height="28" {...base} {...p}>
      {/* 아래로 갈수록 살짝 벌어지는 종 모양 몸통 (아래는 열림) */}
      <path
        d="M4.4 24.8C5 21.7 5.4 19.1 5.4 16.4 5.4 11.6 9.2 8.5 14 8.5S22.6 11.6 22.6 16.4c0 2.7.4 5.3 1 8.4"
        strokeWidth="1.9"
      />
      {/* 끝에 작은 공이 달린 짧은 더듬이 2개 */}
      <path d="M12.1 8.7 10.9 5.8M15.9 8.7l1.2-2.9" strokeWidth="1.5" />
      <circle cx="10.5" cy="4.9" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="17.5" cy="4.9" r="1.4" fill="currentColor" stroke="none" />
      {/* 점 눈 + 넓은 미소 */}
      <circle cx="10.2" cy="15.6" r="1.05" fill="currentColor" stroke="none" />
      <circle cx="17.8" cy="15.6" r="1.05" fill="currentColor" stroke="none" />
      <path d="M10.6 18.6Q14 21.8 17.4 18.6" strokeWidth="1.8" />
    </svg>
  ),
};

/* ---------- 프로모션 배너 아이콘 ---------- */
export const PromoIcons = {
  gift: (c) => (
    <svg viewBox="0 0 32 32" width="34" height="34">
      <rect x="5" y="12" width="22" height="16" rx="2" fill={c} opacity="0.85" />
      <rect x="4" y="9" width="24" height="5" rx="1.5" fill={c} />
      <rect x="14.5" y="9" width="3" height="19" fill="#fff" opacity="0.8" />
      <path d="M16 9c-2-4-7-5-7-2s5 2 7 2Zm0 0c2-4 7-5 7-2s-5 2-7 2Z" fill="none" stroke={c} strokeWidth="2" />
    </svg>
  ),
  coffee: (c) => (
    <svg viewBox="0 0 32 32" width="34" height="34">
      <path d="M6 12h16v8a6 6 0 0 1-6 6h-4a6 6 0 0 1-6-6v-8Z" fill={c} />
      <path d="M22 14h2a3 3 0 0 1 0 6h-2" fill="none" stroke={c} strokeWidth="2.2" />
      <path d="M11 5c-1 2 1 3 0 5M16 5c-1 2 1 3 0 5" stroke={c} strokeWidth="1.8" fill="none" strokeLinecap="round" />
    </svg>
  ),
  globe: (c) => (
    <svg viewBox="0 0 32 32" width="34" height="34" fill="none" stroke={c} strokeWidth="2.2">
      <circle cx="16" cy="16" r="11" fill={c} fillOpacity="0.15" />
      <path d="M5 16h22M16 5c4 4 4 18 0 22M16 5c-4 4-4 18 0 22" />
    </svg>
  ),
  piggy: (c) => (
    <svg viewBox="0 0 32 32" width="34" height="34">
      <ellipse cx="15" cy="18" rx="10" ry="8" fill={c} />
      <rect x="9" y="23" width="3" height="5" rx="1" fill={c} />
      <rect x="17" y="23" width="3" height="5" rx="1" fill={c} />
      <circle cx="25" cy="17" r="2.5" fill={c} />
      <rect x="12" y="10" width="7" height="2" rx="1" fill="#fff" />
      <circle cx="19" cy="15.5" r="1.1" fill="#fff" />
    </svg>
  ),
  shield: (c) => (
    <svg viewBox="0 0 32 32" width="34" height="34">
      <path d="M16 3 26 7v8c0 7-5 12-10 14C11 27 6 22 6 15V7l10-4Z" fill={c} />
      <path d="m11 16 3.5 3.5L21 13" fill="none" stroke="#fff" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
};
