import wonnieImg from '../assets/wonnie-3d.png';
import singImg from '../assets/wonnie-3d-sing.png';   // 눈 감고 'o' 입 + 두 팔 내림 (노래)
import sideImg from '../assets/wonnie-3d-side.png';   // 옆(3/4)모습: 오른쪽을 봄
import backImg from '../assets/wonnie-3d-back.png';   // 뒷모습 (좌우 반전, 든 손이 왼쪽)

/*
 * 크리스마스 장면 속 3D 워니들
 * 3D 워니 원본에서 눈·입을 지우고 새 표정을 그린 변형 이미지(tools/faces3d.py)와
 * 들어 올린 팔을 내린 자세(tools/armsdown.py)로 워니마다 표정·자세를 다르게 했어요.
 *
 * 각 워니는 <svg viewBox="0 0 300 335"> 안에 이미지와 소품을 함께 그립니다.
 * → 소품 좌표가 '이미지 픽셀' 기준이라 크기·좌우반전과 관계없이 머리·손에 정확히 붙어 있어요.
 * 모자를 쓴 경우, 모자를 그린 뒤 새싹 부분만 이미지를 한 번 더 그려 새싹이 모자 위로 나오게 합니다.
 *
 * 이미지 기준 좌표 (300 x 335)
 *  - 새싹: 큰 잎 (70,0)~(125,52), 작은 잎 (128,22)~(166,48), 줄기 밑동 (118,52)
 *  - 정수리 곡선: (55,112) → (85,72) → (122,54) → (165,56) → (205,78) → (232,118)
 *  - 눈썹: 왼쪽 y≈105, 오른쪽 y≈90 / 들어 올린 손 (255~295, 128~190) / 배 (80~250, 185~305)
 */
const W = 300;
const H = 335;

function Sprite({ id, img = wonnieImg, height, flip = false, tilt = 0, squeeze = 1, clipTop, className = '', children, overlay }) {
  const width = (height * W * squeeze) / H;
  return (
    <div className={`xw ${className}`} style={{ width, height }} aria-hidden="true">
      <svg className="xw__svg" viewBox={`0 0 ${W} ${H}`} width={width} height={height} overflow="visible" preserveAspectRatio="none" style={{ rotate: `${tilt}deg` }}>
        <g transform={flip ? `translate(${W} 0) scale(-1 1)` : undefined}>
          {clipTop != null && (
            <clipPath id={`${id}-body`}><rect x="-20" y={clipTop} width={W + 40} height={H} /></clipPath>
          )}
          <image href={img} width={W} height={H} clipPath={clipTop != null ? `url(#${id}-body)` : undefined} />
          {children}
          {overlay?.(id)}
        </g>
      </svg>
    </div>
  );
}

// 새싹만 이미지에서 다시 그리기 (모자 위로 새싹이 나오도록)
function SproutOnTop({ id, img = wonnieImg }) {
  return (
    <>
      <clipPath id={`${id}-sprout`}>
        <ellipse cx="97" cy="24" rx="34" ry="19" transform="rotate(38 97 24)" />
        <ellipse cx="147" cy="36" rx="22" ry="13" transform="rotate(-22 147 36)" />
        <rect x="108" y="30" width="22" height="22" rx="6" />
      </clipPath>
      <image href={img} width={W} height={H} clipPath={`url(#${id}-sprout)`} />
    </>
  );
}

/* ---------- 소품 ---------- */

function Grad({ id }) {
  return (
    <defs>
      <radialGradient id={`${id}-red`} cx="0.35" cy="0.25" r="0.85">
        <stop offset="0" stopColor="#ff8a8a" />
        <stop offset="0.5" stopColor="#e2303c" />
        <stop offset="1" stopColor="#9c1420" />
      </radialGradient>
      <linearGradient id={`${id}-fur`} x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="#ffffff" />
        <stop offset="0.6" stopColor="#f1f4f9" />
        <stop offset="1" stopColor="#c9d4e4" />
      </linearGradient>
      <radialGradient id={`${id}-pom`} cx="0.35" cy="0.3" r="0.75">
        <stop offset="0" stopColor="#ffffff" />
        <stop offset="0.7" stopColor="#eef2f8" />
        <stop offset="1" stopColor="#c3cfe0" />
      </radialGradient>
      <radialGradient id={`${id}-green`} cx="0.35" cy="0.25" r="0.85">
        <stop offset="0" stopColor="#5fd08f" />
        <stop offset="0.55" stopColor="#2e9160" />
        <stop offset="1" stopColor="#17583c" />
      </radialGradient>
      <linearGradient id={`${id}-gold`} x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stopColor="#fff2b0" />
        <stop offset="1" stopColor="#e2a81a" />
      </linearGradient>
      <linearGradient id={`${id}-vol`} x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stopColor="#fff" stopOpacity="0.3" />
        <stop offset="0.5" stopColor="#fff" stopOpacity="0" />
        <stop offset="1" stopColor="#000" stopOpacity="0.25" />
      </linearGradient>
    </defs>
  );
}

// 산타 모자: 정수리를 덮고, 끝은 오른쪽 뒤로 넘어감. 새싹은 모자 꼭대기로 나옴
function SantaHat({ id }) {
  return (
    <g>
      <Grad id={id} />
      {/* 넘어가는 끝 + 방울 */}
      <path d="M150 52 C190 30 236 34 252 62 C236 52 214 54 196 70 Z" fill={`url(#${id}-red)`} />
      {/* 돔 */}
      <path d="M50 118 C52 76 84 48 120 46 C162 44 206 62 234 112 Q140 50 50 118 Z" fill={`url(#${id}-red)`} />
      <path d="M78 76 C92 60 108 54 124 53" fill="none" stroke="#ffa3a3" strokeWidth="5" strokeLinecap="round" opacity="0.5" />
      {/* 털 띠: 정수리 곡선을 따라감 */}
      <path d="M50 122 Q140 52 234 116" fill="none" stroke="#a9b6c8" strokeWidth="23" strokeLinecap="round" opacity="0.45" />
      <path d="M50 119 Q140 49 234 113" fill="none" stroke={`url(#${id}-fur)`} strokeWidth="21" strokeLinecap="round" />
      <path d="M58 110 Q140 46 226 104" fill="none" stroke="#fff" strokeWidth="5" strokeLinecap="round" opacity="0.9" />
      <circle cx="254" cy="66" r="15" fill={`url(#${id}-pom)`} />
      <SproutOnTop id={id} />
    </g>
  );
}

// 옆모습용 산타 모자: 정수리를 비스듬히 덮고, 끝이 뒤통수 쪽으로 늘어짐 (눈썹 위에서 멈춤)
function SideSantaHat({ id }) {
  return (
    <g>
      <Grad id={id} />
      {/* 뒤로 늘어지는 끝 */}
      <path d="M122 20 C78 10 36 38 24 90 C42 70 66 58 100 56 Z" fill={`url(#${id}-red)`} />
      {/* 돔 */}
      <path d="M58 108 C52 56 88 18 130 17 C170 17 198 44 210 74 Q142 22 58 108 Z" fill={`url(#${id}-red)`} />
      <path d="M80 70 C92 44 110 30 136 27" fill="none" stroke="#ffa3a3" strokeWidth="5" strokeLinecap="round" opacity="0.45" />
      {/* 털 띠: 머리 윤곽을 따라 뒤통수 → 이마 위 */}
      <path d="M58 112 Q138 26 211 79" fill="none" stroke="#a9b6c8" strokeWidth="22" strokeLinecap="round" opacity="0.45" />
      <path d="M58 109 Q138 23 211 76" fill="none" stroke={`url(#${id}-fur)`} strokeWidth="20" strokeLinecap="round" />
      <path d="M66 100 Q138 20 203 68" fill="none" stroke="#fff" strokeWidth="5" strokeLinecap="round" opacity="0.9" />
      <circle cx="24" cy="96" r="15" fill={`url(#${id}-pom)`} />
    </g>
  );
}

// 니트 모자: 초록 니트 + 빨간 테두리, 새싹이 꼭대기로 나옴
function Beanie({ id }) {
  return (
    <g>
      <Grad id={id} />
      <path d="M52 120 C52 74 86 46 124 44 C164 42 208 62 232 114 Q140 54 52 120 Z" fill={`url(#${id}-green)`} />
      <g stroke="#ffffff" strokeWidth="4" strokeLinecap="round" opacity="0.7" fill="none" strokeDasharray="1 11">
        <path d="M70 88 Q140 46 214 84" />
        <path d="M86 68 Q140 38 196 64" />
      </g>
      <path d="M52 122 Q140 56 232 116" fill="none" stroke="#9c1420" strokeWidth="21" strokeLinecap="round" opacity="0.5" />
      <path d="M52 119 Q140 53 232 113" fill="none" stroke={`url(#${id}-red)`} strokeWidth="19" strokeLinecap="round" />
      <path d="M60 116 Q140 52 224 109" fill="none" stroke="#ffffff" strokeWidth="2.5" strokeDasharray="5 6" opacity="0.6" />
    </g>
  );
}

// 목도리: 얼굴 아래·배 위를 감쌈, 꼬리는 왼쪽으로 늘어짐
function Scarf({ id }) {
  return (
    <g>
      <Grad id={id} />
      <path d="M44 186 Q148 222 250 180 L252 204 Q148 248 44 210 Z" fill="#7a0f18" opacity="0.25" transform="translate(0 4)" />
      <path d="M44 186 Q148 222 250 180 L252 204 Q148 248 44 210 Z" fill={`url(#${id}-red)`} />
      <path d="M48 190 Q148 225 246 184" fill="none" stroke="#ffb0b3" strokeWidth="4" opacity="0.7" />
      <g stroke="#fff" strokeWidth="6" opacity="0.9">
        <path d="M84 200 v22 M124 209 v24 M164 210 v24 M204 202 v22" />
      </g>
      <path d="M70 200 L52 262 L76 268 L92 206 Z" fill={`url(#${id}-red)`} />
      <path d="M56 252 L78 258 M54 262 L76 268" stroke="#fff" strokeWidth="4" />
    </g>
  );
}

// 앞에 놓인 큰 선물 상자 (배 아래를 가림 → 상자 뒤에서 고개 내민 모습)
function GiftInFront({ id }) {
  return (
    <g>
      <Grad id={id} />
      <rect x="54" y="226" width="196" height="112" rx="10" fill={`url(#${id}-red)`} />
      <rect x="54" y="226" width="196" height="112" rx="10" fill={`url(#${id}-vol)`} />
      <rect x="44" y="204" width="216" height="34" rx="10" fill="#f0525b" />
      <rect x="44" y="204" width="216" height="34" rx="10" fill={`url(#${id}-vol)`} />
      <rect x="138" y="204" width="28" height="134" fill={`url(#${id}-gold)`} />
      <path d="M152 206 C120 156 84 178 116 204 Z M152 206 C184 156 220 178 188 204 Z" fill={`url(#${id}-gold)`} stroke="#c88f10" strokeWidth="3" />
      <circle cx="152" cy="204" r="11" fill="#f5c33b" stroke="#c88f10" strokeWidth="2.5" />
    </g>
  );
}

// 들어 올린 손에 든 오너먼트 (대롱대롱)
function HeldOrnament({ id, color = 'red', x = 274 }) {
  const stops = color === 'red' ? ['#ffb0b0', '#ef3f4a', '#a8141f'] : ['#fff3c0', '#f5c33b', '#b97d06'];
  return (
    <g className="xw__swing" style={{ transformOrigin: `${x}px 150px` }}>
      <defs>
        <radialGradient id={`${id}-orn`} cx="0.35" cy="0.3" r="0.75">
          <stop offset="0" stopColor={stops[0]} />
          <stop offset="0.45" stopColor={stops[1]} />
          <stop offset="1" stopColor={stops[2]} />
        </radialGradient>
      </defs>
      <path d={`M${x} 150 V180`} stroke="#d9b25a" strokeWidth="3" />
      <rect x={x - 7} y="178" width="14" height="8" rx="2" fill="#d9b25a" />
      <circle cx={x} cy="206" r="21" fill={`url(#${id}-orn)`} />
      <path d={`M${x - 11} 198 Q${x - 7} 190 ${x + 2} 189`} stroke="#fff" strokeWidth="4" fill="none" opacity="0.8" strokeLinecap="round" />
    </g>
  );
}

// 노래하는 워니의 음표 (입 오른쪽 위로 떠오름)
function MusicNotes() {
  const note = (x, y, cls, color, double = false) => (
    <g className={`xw__note ${cls}`} style={{ transformOrigin: `${x}px ${y}px` }}>
      {double ? (
        <>
          <path d={`M${x} ${y} V${y - 46} L${x + 34} ${y - 54} V${y - 8}`} fill="none" stroke={color} strokeWidth="7" strokeLinejoin="round" />
          <ellipse cx={x - 8} cy={y} rx="12" ry="9" transform={`rotate(-20 ${x - 8} ${y})`} fill={color} />
          <ellipse cx={x + 26} cy={y - 8} rx="12" ry="9" transform={`rotate(-20 ${x + 26} ${y - 8})`} fill={color} />
        </>
      ) : (
        <>
          <path d={`M${x} ${y} V${y - 44} Q${x + 18} ${y - 36} ${x + 20} ${y - 22}`} fill="none" stroke={color} strokeWidth="7" strokeLinecap="round" />
          <ellipse cx={x - 8} cy={y} rx="12" ry="9" transform={`rotate(-20 ${x - 8} ${y})`} fill={color} />
        </>
      )}
    </g>
  );
  return (
    <g>
      {note(232, 112, 'xw__note--1', '#e5303c')}
      {note(270, 70, 'xw__note--2', '#2d82ef', true)}
      {note(300, 128, 'xw__note--3', '#f2a100')}
    </g>
  );
}

/* ---------- 장면 배치 ---------- */
export default function WonnieCast() {
  return (
    <>
      {/* 트리 왼쪽: 옆모습으로 트리를 바라보며, 산타 모자 쓰고 오너먼트를 거는 워니 */}
      <Sprite id="w1" img={sideImg} height={60} squeeze={0.86} tilt={4} clipTop={46} className="xw--left" overlay={(id) => (
        <>
          <SideSantaHat id={id} />
          <HeldOrnament id={id} color="red" />
        </>
      )} />

      {/* 트리 오른쪽: 목도리 두르고 두 팔 내린 채 눈 감고 캐럴 부르는 워니 */}
      <Sprite id="w2" img={singImg} height={60} tilt={-3} className="xw--right" overlay={(id) => (
        <>
          <Scarf id={id} />
          <MusicNotes />
        </>
      )} />

      {/* 트리 앞: 뒤돌아서 트리에 금색 오너먼트를 다는 워니 */}
      <Sprite id="w3" img={backImg} height={46} className="xw--front" overlay={(id) => <HeldOrnament id={id} color="gold" x={26} />} />

      {/* 창틀: 니트 모자 쓰고 선물 상자 뒤에서 손 흔드는 워니 */}
      <Sprite id="w4" height={30} flip clipTop={52} className="xw--window" overlay={(id) => (
        <>
          <Beanie id={id} />
          <GiftInFront id={`${id}g`} />
        </>
      )} />
    </>
  );
}
