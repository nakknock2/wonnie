import wonnieImg from '../../assets/wonnie-3d.png';
import wonnieFace from '../../assets/wonnie-3d-face.png';
import wowImg from '../../assets/wonnie-3d-wow.png';

// face: 표정 변형 이미지 (tools/faces3d.py 로 생성)
const FACE_IMG = { default: wonnieImg, wow: wowImg };
import './Wonnie.css';

/*
 * 워니(Wonnie) 캐릭터 – 새싹 머리 3D 워니 (홈 크리스마스 장면과 같은 이미지)
 *
 * <Wonnie />        전신 캐릭터 – 플로팅 버튼, 상담 창 환영 카드
 * <WonnieAvatar />  원형 얼굴 아바타 – 상담 창 헤더, 말풍선 옆
 *
 * 3D 이미지는 표정이 하나라 expression 은 '동작'으로 표현합니다.
 *  - thinking  : 갸웃갸웃 흔들림 (답변 준비 중)
 *  - listening : 살짝 기울임 (사용자 입력 중)
 *  - excited   : 통통 뜀
 *  - 그 외     : 기본
 */

const RATIO = 335 / 300; // 전신 이미지 세로/가로 비율

export default function Wonnie({
  expression = 'default',
  size = 68,
  animated = false,
  shadow = true,
  face = 'default',
  className = '',
}) {
  const classes = ['wonnie', `wonnie--${expression}`, animated && 'wonnie--animated', className]
    .filter(Boolean)
    .join(' ');

  return (
    <span className={classes} style={{ width: size, height: size * RATIO }} aria-hidden="true">
      {shadow && <span className="wonnie__shadow" />}
      <img className="wonnie__img" src={FACE_IMG[face] ?? wonnieImg} alt="" draggable="false" />
    </span>
  );
}

export function WonnieAvatar({ size = 32, expression = 'default', className = '' }) {
  return (
    <span
      className={`wonnie-avatar wonnie--${expression} ${className}`}
      style={{ width: size, height: size }}
      aria-hidden="true"
    >
      <img className="wonnie-avatar__img" src={wonnieFace} alt="" draggable="false" />
    </span>
  );
}
