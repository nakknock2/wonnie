import { GearIcon } from './Icons.jsx';
import WonnieCast from './WonnieCast.jsx';
import '../styles/hero-winter.css';

/*
 * 크리스마스 오두막 거실 일러스트 (SVG, 그라데이션·글로우로 입체감)
 * - 왼쪽: 벽돌 벽난로 (타오르는 불, 양말, 촛불, 리스)
 * - 가운데: 크리스마스 트리 + 함께 트리를 꾸미는 워니들 (WonnieCast.jsx)
 * - 오른쪽: 눈 내리는 밤 창문
 */

const ORNAMENTS = [
  [187, 38, 'red'], [203, 44, 'gold'],
  [180, 58, 'blue'], [196, 64, 'red'], [212, 58, 'gold'],
  [174, 80, 'gold'], [189, 86, 'blue'], [205, 87, 'red'], [220, 80, 'blue'],
];
const BULBS = [
  [183, 44], [195, 47], [208, 41], [176, 64], [190, 69], [205, 70], [218, 64],
  [170, 88], [182, 93], [196, 95], [210, 94], [223, 89],
];
const WINDOW_SNOW = [
  [290, 0, 5.5], [298, 2.2, 6.5], [306, 1.1, 5], [314, 3.4, 7], [322, 0.6, 6],
  [330, 2.8, 5.8], [338, 1.7, 6.8], [346, 0.3, 5.2],
];

function Scene() {
  return (
    <svg className="hero-svg" viewBox="0 0 390 136" preserveAspectRatio="xMidYMax slice" aria-hidden="true">
      <defs>
        {/* 위아래 가장자리를 부드럽게 페이드 → 홈 배경과 자연스럽게 연결 */}
        <linearGradient id="xr-fade" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#fff" stopOpacity="0" />
          <stop offset="0.17" stopColor="#fff" stopOpacity="1" />
          <stop offset="0.86" stopColor="#fff" stopOpacity="1" />
          <stop offset="1" stopColor="#fff" stopOpacity="0" />
        </linearGradient>
        <mask id="xr-mask"><rect width="390" height="136" fill="url(#xr-fade)" /></mask>

        <linearGradient id="xr-wall" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#fbeedd" />
          <stop offset="1" stopColor="#f1d6b2" />
        </linearGradient>
        <pattern id="xr-paper" width="16" height="16" patternUnits="userSpaceOnUse">
          <path d="M8 3 L10 8 L8 13 L6 8 Z" fill="#e8c597" opacity="0.45" />
        </pattern>
        <linearGradient id="xr-wains" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#d9a06c" />
          <stop offset="1" stopColor="#b97a4a" />
        </linearGradient>
        <linearGradient id="xr-floor" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#c98a55" />
          <stop offset="1" stopColor="#a8683c" />
        </linearGradient>
        <linearGradient id="xr-brick" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#c8694f" />
          <stop offset="0.5" stopColor="#b85a40" />
          <stop offset="1" stopColor="#9c4532" />
        </linearGradient>
        <pattern id="xr-bricks" width="14" height="8" patternUnits="userSpaceOnUse">
          <path d="M0 0 H14 M0 4 H14 M4 0 V4 M11 4 V8" stroke="#8a3a2a" strokeWidth="0.7" opacity="0.55" fill="none" />
        </pattern>
        <linearGradient id="xr-mantel" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#9a6038" />
          <stop offset="1" stopColor="#5f3519" />
        </linearGradient>
        <radialGradient id="xr-firebox" cx="0.5" cy="0.85" r="0.75">
          <stop offset="0" stopColor="#ff9a3c" />
          <stop offset="0.45" stopColor="#7a2e14" />
          <stop offset="1" stopColor="#2a140b" />
        </radialGradient>
        <linearGradient id="xr-flame" x1="0" y1="1" x2="0" y2="0">
          <stop offset="0" stopColor="#ffde59" />
          <stop offset="0.5" stopColor="#ff8a1f" />
          <stop offset="1" stopColor="#e8381c" />
        </linearGradient>
        <linearGradient id="xr-flame-in" x1="0" y1="1" x2="0" y2="0">
          <stop offset="0" stopColor="#fffbe0" />
          <stop offset="1" stopColor="#ffd23f" />
        </linearGradient>
        <linearGradient id="xr-tree" x1="0" y1="0" x2="1" y2="0.3">
          <stop offset="0" stopColor="#5cc08a" />
          <stop offset="0.45" stopColor="#2e9160" />
          <stop offset="1" stopColor="#17583c" />
        </linearGradient>
        <linearGradient id="xr-night" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#1d3566" />
          <stop offset="1" stopColor="#47689f" />
        </linearGradient>
        <linearGradient id="xr-curtain" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#c62a35" />
          <stop offset="0.35" stopColor="#e5484f" />
          <stop offset="0.65" stopColor="#b8202c" />
          <stop offset="1" stopColor="#d63a44" />
        </linearGradient>
        <radialGradient id="xr-orn-red" cx="0.35" cy="0.3" r="0.75">
          <stop offset="0" stopColor="#ffb0b0" /><stop offset="0.45" stopColor="#ef3f4a" /><stop offset="1" stopColor="#a8141f" />
        </radialGradient>
        <radialGradient id="xr-orn-gold" cx="0.35" cy="0.3" r="0.75">
          <stop offset="0" stopColor="#fff3c0" /><stop offset="0.45" stopColor="#f5c33b" /><stop offset="1" stopColor="#b97d06" />
        </radialGradient>
        <radialGradient id="xr-orn-blue" cx="0.35" cy="0.3" r="0.75">
          <stop offset="0" stopColor="#cfe6ff" /><stop offset="0.45" stopColor="#3d8df0" /><stop offset="1" stopColor="#1a4fa8" />
        </radialGradient>
        <linearGradient id="xr-gold" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#fff6c4" /><stop offset="0.5" stopColor="#ffd23f" /><stop offset="1" stopColor="#e39a00" />
        </linearGradient>
        <linearGradient id="xr-box-red" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#f2545d" /><stop offset="1" stopColor="#b8202c" />
        </linearGradient>
        <linearGradient id="xr-box-blue" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stopColor="#5aa2f5" /><stop offset="1" stopColor="#2563c9" />
        </linearGradient>
        <filter id="xr-glow" x="-100%" y="-100%" width="300%" height="300%">
          <feGaussianBlur stdDeviation="1.5" result="b" />
          <feMerge><feMergeNode in="b" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
        <filter id="xr-blur" x="-60%" y="-60%" width="220%" height="220%">
          <feGaussianBlur stdDeviation="6" />
        </filter>
        <filter id="xr-blur-sm" x="-60%" y="-60%" width="220%" height="220%">
          <feGaussianBlur stdDeviation="2" />
        </filter>
        <clipPath id="xr-window-clip"><rect x="286" y="22" width="64" height="44" rx="2" /></clipPath>
      </defs>

      <g mask="url(#xr-mask)">
        {/* ===== 벽 ===== */}
        <rect x="0" y="0" width="390" height="104" fill="url(#xr-wall)" />
        <rect x="0" y="0" width="390" height="80" fill="url(#xr-paper)" />
        {/* 웨인스코팅 */}
        <rect x="0" y="80" width="390" height="24" fill="url(#xr-wains)" />
        <rect x="0" y="78" width="390" height="3" fill="#8f5a36" />
        <g stroke="#9e6538" strokeWidth="0.8" opacity="0.5">
          {[13, 39, 65, 91, 117, 143, 169, 195, 221, 247, 273, 299, 325, 351, 377].map((x) => (
            <rect key={x} x={x - 10} y="84" width="20" height="16" rx="1.5" fill="none" />
          ))}
        </g>
        {/* 바닥 */}
        <rect x="0" y="104" width="390" height="32" fill="url(#xr-floor)" />
        <rect x="0" y="102" width="390" height="3" fill="#7a4526" />
        <g stroke="#8a4f2a" strokeWidth="0.7" opacity="0.5">
          <path d="M0 112 H390 M0 121 H390 M0 130 H390" />
          <path d="M40 104 V112 M120 112 V121 M210 104 V112 M300 112 V121 M360 104 V112 M80 121 V130 M250 121 V130" />
        </g>

        {/* 따뜻한 빛 (벽난로·트리) */}
        <ellipse className="xr-fireglow" cx="70" cy="80" rx="70" ry="50" fill="#ffb256" opacity="0.4" filter="url(#xr-blur)" />
        <ellipse cx="196" cy="60" rx="60" ry="50" fill="#fff2c4" opacity="0.45" filter="url(#xr-blur)" />

        {/* ===== 왼쪽: 벽난로 ===== */}
        {/* 리스 */}
        <g transform="translate(70 26)">
          <circle r="9" fill="none" stroke="#2e8a5a" strokeWidth="5" />
          <circle r="9" fill="none" stroke="#4fb27b" strokeWidth="2" strokeDasharray="2 2.5" />
          {[[-7, -5], [6, -6], [8, 4], [-8, 4], [0, 9]].map(([x, y]) => (
            <circle key={`${x}${y}`} cx={x} cy={y} r="1.3" fill="#e5303c" />
          ))}
          <path d="M0 6 L-4 13 L-1 12 Z M0 6 L4 13 L1 12 Z" fill="#d42a35" />
          <path d="M0 6 C-6 2 -7 9 0 6.5 C7 9 6 2 0 6Z" fill="#e5484f" />
        </g>
        {/* 몸체 */}
        <rect x="26" y="46" width="88" height="58" fill="url(#xr-brick)" />
        <rect x="26" y="46" width="88" height="58" fill="url(#xr-bricks)" />
        {/* 불 넣는 곳 */}
        <path d="M42 92 V70 Q42 58 54 58 H86 Q98 58 98 70 V92 Z" fill="url(#xr-firebox)" />
        <path d="M42 92 V70 Q42 58 54 58 H86 Q98 58 98 70 V92" fill="none" stroke="#6d2d1c" strokeWidth="2" />
        {/* 장작 */}
        <rect x="52" y="85" width="34" height="5" rx="2.5" fill="#6b3a1f" transform="rotate(-6 69 87)" />
        <rect x="54" y="85" width="34" height="5" rx="2.5" fill="#7d4524" transform="rotate(7 71 87)" />
        <circle cx="53" cy="88" r="2.4" fill="#c58a5a" />
        <circle cx="87" cy="88" r="2.4" fill="#c58a5a" />
        {/* 불꽃 */}
        <g className="xr-fire" filter="url(#xr-glow)">
          <path className="xr-flame xr-flame--a" d="M70 88 C56 84 58 70 64 64 C64 71 68 72 68 72 C66 64 72 58 76 56 C74 64 82 68 82 76 C84 73 84 71 84 69 C90 76 86 88 70 88 Z" fill="url(#xr-flame)" />
          <path className="xr-flame xr-flame--b" d="M70 88 C62 86 63 78 67 74 C68 78 70 79 70 79 C70 74 73 71 75 70 C75 75 79 78 78 83 C77 87 74 88 70 88 Z" fill="url(#xr-flame-in)" />
        </g>
        {/* 불티 */}
        {[[64, 0], [72, 1.1], [78, 2.2]].map(([x, d]) => (
          <circle key={x} className="xr-ember" cx={x} cy="66" r="0.9" fill="#ffd36b" style={{ animationDelay: `${d}s` }} />
        ))}
        {/* 단 + 맨틀 */}
        <rect x="34" y="92" width="72" height="5" rx="1" fill="#c9bdb1" />
        <rect x="20" y="40" width="100" height="7" rx="1.5" fill="url(#xr-mantel)" />
        <rect x="20" y="47" width="100" height="2" fill="#000" opacity="0.15" />
        {/* 맨틀 갈랜드 */}
        <path d="M22 41 Q34 46 46 41 Q58 46 70 41 Q82 46 94 41 Q106 46 118 41" fill="none" stroke="#2e8a5a" strokeWidth="3.4" strokeLinecap="round" />
        <path d="M22 41 Q34 46 46 41 Q58 46 70 41 Q82 46 94 41 Q106 46 118 41" fill="none" stroke="#56b582" strokeWidth="1.2" strokeDasharray="1.5 2.5" strokeLinecap="round" />
        {[[28, 0], [40, 1], [52, 2], [64, 0], [76, 1], [88, 2], [100, 0], [112, 1]].map(([x, g]) => (
          <circle key={x} className={`xr-bulb xr-bulb--${g}`} cx={x} cy={x % 24 === 4 ? 44 : 43} r="1.1"
            fill={['#ff6b6b', '#ffd23f', '#6ec1ff'][g]} filter="url(#xr-glow)" />
        ))}
        {/* 촛불 */}
        {[30, 110].map((x) => (
          <g key={x}>
            <rect x={x - 2} y="30" width="4" height="10" rx="1" fill="#fff6e6" />
            <rect x={x - 2} y="30" width="1.5" height="10" fill="#fff" opacity="0.7" />
            <ellipse className="xr-candle" cx={x} cy="27.5" rx="1.4" ry="2.4" fill="#ffcf4d" filter="url(#xr-glow)" />
          </g>
        ))}
        {/* 양말 */}
        {[[36, '#e2303c', '#fff'], [104, '#2e8a5a', '#fff']].map(([x, c, cuff]) => (
          <g key={x} transform={`translate(${x} 49)`}>
            <path d="M-4 0 H4 V12 Q4 16 8 16 Q11 16 11 19 Q11 22 7 22 H-1 Q-5 22 -5 17 Z" fill={c} />
            <rect x="-5" y="-1" width="10" height="4" rx="1.5" fill={cuff} />
            <path d="M-3 8 H3 M-3 11 H3" stroke="#fff" strokeWidth="0.9" opacity="0.6" />
          </g>
        ))}

        {/* ===== 오른쪽: 밤 창문 ===== */}
        <g clipPath="url(#xr-window-clip)">
          <rect x="286" y="22" width="64" height="44" fill="url(#xr-night)" />
          <circle cx="338" cy="32" r="8" fill="#fff6d0" opacity="0.35" filter="url(#xr-blur-sm)" />
          <circle cx="338" cy="32" r="4.5" fill="#fff6d0" />
          <path d="M280 60 Q300 50 318 56 Q334 60 356 52 V70 H280Z" fill="#dfe9f8" />
          {[[296, 58], [308, 56], [344, 54]].map(([x, y]) => (
            <g key={x} transform={`translate(${x} ${y})`}>
              <path d="M0 -12 L5 -4 L3 -4 L7 2 L-7 2 L-3 -4 L-5 -4 Z" fill="#20365c" />
              <path d="M0 -12 L3 -7 L-3 -7 Z M-5 -4 L5 -4 L4 -5 L-4 -5Z" fill="#fff" />
            </g>
          ))}
          {WINDOW_SNOW.map(([x, d, dur], i) => (
            <circle key={i} className="xr-wsnow" cx={x} cy="18" r={i % 3 === 0 ? 1.1 : 0.8} fill="#fff"
              style={{ animationDelay: `${d}s`, animationDuration: `${dur}s` }} />
          ))}
          {/* 성에 */}
          <path d="M286 22 Q292 22 293 28 Q288 27 286 32Z M350 22 Q344 22 343 28 Q348 27 350 32Z M286 66 Q290 60 296 66Z M350 66 Q346 60 340 66Z" fill="#fff" opacity="0.7" />
        </g>
        {/* 창틀 */}
        <rect x="284" y="20" width="68" height="48" rx="3" fill="none" stroke="#fffaf2" strokeWidth="4" />
        <path d="M318 22 V66 M286 44 H350" stroke="#fffaf2" strokeWidth="2.6" />
        <rect x="278" y="67" width="80" height="5" rx="1.5" fill="#fffaf2" />
        <rect x="278" y="72" width="80" height="2" fill="#000" opacity="0.08" />
        {/* 커튼 */}
        <rect x="270" y="12" width="96" height="3.5" rx="1.75" fill="#8f5a36" />
        <circle cx="270" cy="13.75" r="2.6" fill="#c9963a" />
        <circle cx="366" cy="13.75" r="2.6" fill="#c9963a" />
        <path d="M272 15 Q282 15 288 16 Q284 34 290 50 Q281 62 286 80 L272 80 Z" fill="url(#xr-curtain)" />
        <path d="M364 15 Q354 15 348 16 Q352 34 346 50 Q355 62 350 80 L364 80 Z" fill="url(#xr-curtain)" />
        <path d="M282 48 Q288 46 291 50 M354 48 Q348 46 345 50" stroke="#e9b84a" strokeWidth="2" fill="none" strokeLinecap="round" />

        {/* ===== 가운데: 러그 + 트리 ===== */}
        <ellipse cx="196" cy="114" rx="78" ry="10" fill="#f6e7d0" />
        <ellipse cx="196" cy="114" rx="78" ry="10" fill="none" stroke="#c9363f" strokeWidth="3" />
        <ellipse cx="196" cy="114" rx="70" ry="7.5" fill="none" stroke="#2e8a5a" strokeWidth="1.2" strokeDasharray="3 3" />

        <g>
        <ellipse cx="196" cy="106" rx="30" ry="4" fill="#5b3a22" opacity="0.25" />
        <path d="M188 98 H204 L202 107 H190 Z" fill="#b8202c" />
        <rect x="186" y="96" width="20" height="3.5" rx="1.5" fill="#e5484f" />
        <path d="M196 50 L232 98 Q196 104 160 98 Z" fill="url(#xr-tree)" />
        <path d="M196 32 L225 76 Q196 82 167 76 Z" fill="url(#xr-tree)" />
        <path d="M196 14 L218 54 Q196 59 174 54 Z" fill="url(#xr-tree)" />
        <path d="M196 50 L232 98 Q214 102 198 102 Z M196 32 L225 76 Q211 80 198 80 Z M196 14 L218 54 Q207 57 198 57 Z" fill="#0d3b28" opacity="0.18" />
        {/* 갈랜드 */}
        <path d="M178 48 Q196 57 214 46 M171 70 Q196 81 222 69 M164 92 Q196 104 228 91" fill="none" stroke="url(#xr-gold)" strokeWidth="1.6" strokeDasharray="0.1 2.6" strokeLinecap="round" />
        {BULBS.map(([x, y], i) => (
          <circle key={i} className={`xr-bulb xr-bulb--${i % 3}`} cx={x} cy={y} r="1.25" fill="#fff8d6" filter="url(#xr-glow)" />
        ))}
        {ORNAMENTS.map(([x, y, c], i) => (
          <g key={i}>
            <circle cx={x} cy={y} r="3.3" fill={`url(#xr-orn-${c})`} />
            <rect x={x - 1} y={y - 4.5} width="2" height="1.4" rx="0.4" fill="#d9b25a" />
          </g>
        ))}
        {/* 리본 장식 */}
        {[[184, 72], [210, 34]].map(([x, y]) => (
          <path key={x} d={`M${x} ${y} c-4 -4 -6 1 0 1 c6 0 4 -5 0 -1 l-3 5 m3 -5 l3 5`} fill="#e5303c" stroke="#e5303c" strokeWidth="1" strokeLinecap="round" />
        ))}

        {/* 선물 */}
        <g transform="translate(184 100)">
          <rect x="0" y="0" width="13" height="10" rx="1.2" fill="url(#xr-box-red)" />
          <rect x="-0.6" y="-2" width="14.2" height="3" rx="1" fill="#f86a72" />
          <rect x="5.5" y="-2" width="2" height="12" fill="url(#xr-gold)" />
          <path d="M6.5 -2 C3 -6 0 -4 2.6 -2.5 Z M6.5 -2 C10 -6 13 -4 10.4 -2.5 Z" fill="url(#xr-gold)" />
        </g>
        <g transform="translate(199 103)">
          <rect x="0" y="0" width="11" height="8" rx="1.2" fill="url(#xr-box-blue)" />
          <rect x="-0.5" y="-1.6" width="12" height="2.6" rx="1" fill="#7cb6f8" />
          <rect x="4.5" y="-1.6" width="2" height="9.6" fill="#fff" />
        </g>
        </g>
      </g>

      {/* 트리 꼭대기 별 (페이드 영향 없이 또렷하게) */}
<g>
      <circle className="xr-star-glow" cx="196" cy="13" r="12" fill="#fff3b0" opacity="0.7" filter="url(#xr-blur-sm)" />
      <path className="xr-star" d="M196 4.5 l2.6 5.4 5.9 .7 -4.3 4 1.2 5.8 -5.4-3 -5.4 3 1.2-5.8 -4.3-4 5.9-.7Z" fill="url(#xr-gold)" stroke="#e39a00" strokeWidth="0.6" strokeLinejoin="round" />
      </g>
    </svg>
  );
}

export default function HeroScene({ hideBalance, onToggleHide, onSettings }) {
  return (
    <section className="hero hero--winter">
      <Scene />

      {/* 함께 트리를 꾸미는 워니들 */}
      <WonnieCast />

      <div className="hero-actions">
        <button className="hero-pill" onClick={onSettings}>
          <GearIcon className="hero-pill__gear" />
          홈계좌설정
        </button>

        <label className="hero-pill hero-pill--switch">
          잔액숨김
          <input
            type="checkbox"
            className="switch-input"
            checked={hideBalance}
            onChange={(e) => onToggleHide(e.target.checked)}
          />
          <span className="switch" aria-hidden="true" />
        </label>
      </div>
    </section>
  );
}
