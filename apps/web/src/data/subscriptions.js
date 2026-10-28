import { resolveServiceLogo } from '../services/serviceLogos.js';

// 데모 시나리오 데이터 (실제 연동 시 API 응답으로 교체)
// 날짜는 '데모 날짜'(오늘 + 넘긴 일수) 기준으로 계산합니다. 왓챠는 데모 시작일 기준 '내일' 결제.

export const CUSTOMER = { name: '김우리', age: 28 };
export const CUSTOMER_NAME = CUSTOMER.name;

export const ACCOUNT = { name: 'WON 통장', number: '우리 1002-123-456789', balance: 1284500 };
export const SAVINGS = { name: '워니 절약 적금', number: '우리 1005-987-654321' };

// 데모 시작일 (자정 기준)
export const BASE_DATE = (() => {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
})();

export const addDays = (base, n) => {
  const d = new Date(base);
  d.setDate(d.getDate() + n);
  return d;
};

// 카드 결제내역에서 찾은 정기결제 7건 · 월 61,300원 (예시 금액)
// due: 데모 시작일 기준 결제일까지 남은 일수
export const SUBSCRIPTIONS = [
  { id: 'watcha', name: '왓챠', plan: '베이직', price: 7900, letter: 'W', color: '#2b2d33', due: 1, card: '우리카드 ****1234', lastUsed: '42일 전' },
  { id: 'netflix', name: '넷플릭스', plan: '프리미엄', price: 17000, letter: 'N', color: '#141414', due: 5, card: '우리카드 ****1234', lastUsed: '어제' },
  { id: 'youtube', name: '유튜브 프리미엄', plan: '개인', price: 14900, letter: 'play', color: '#e8334a', due: 9, card: '우리카드 ****1234', lastUsed: '오늘' },
  { id: 'ms365', name: 'Microsoft 365', plan: 'Personal', price: 8900, letter: 'M', color: '#2f6fd6', due: 12, card: '우리카드 ****5678', lastUsed: '3일 전' },
  { id: 'tving', name: '티빙', plan: '광고형 스탠다드', price: 5500, letter: 'T', color: '#e2263a', due: 15, card: '우리카드 ****1234', lastUsed: '2주 전' },
  { id: 'naver', name: '네이버플러스 멤버십', plan: '월간', price: 4900, letter: 'N+', color: '#1fb34f', due: 19, card: '우리카드 ****5678', lastUsed: '5일 전' },
  { id: 'icloud', name: 'iCloud+', plan: '200GB', price: 2200, letter: 'i', color: '#5aa6f0', due: 24, card: '우리카드 ****5678', lastUsed: '자동 결제' },
].map((subscription) => ({ ...subscription, logoSrc: resolveServiceLogo(subscription) }));

export const WATCHA = SUBSCRIPTIONS[0];
export const MONTHLY_TOTAL = SUBSCRIPTIONS.reduce((a, s) => a + s.price, 0); // 61,300

export const billDateOf = (sub) => addDays(BASE_DATE, sub.due);

export const won = (n) => `${n.toLocaleString('ko-KR')}원`;
export const md = (d) => `${d.getMonth() + 1}월 ${d.getDate()}일`;
const WEEK = ['일', '월', '화', '수', '목', '금', '토'];
export const mdw = (d) => `${md(d)}(${WEEK[d.getDay()]})`;
export const ymd = (d) => `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;

// 데모 날짜 기준 남은 일수 → 'D-1', '오늘', '11월 3일'
export function dueLabel(sub, dayOffset) {
  const left = sub.due - dayOffset;
  if (left === 0) return '오늘 결제';
  if (left === 1) return '내일 결제';
  if (left < 0) return '결제 완료';
  return md(billDateOf(sub));
}

// 구독료 인상 예시 이벤트 (데모용 가상 시나리오)
export const PRICE_CHANGE = { subId: 'netflix', from: 17000, to: 18500, effectiveDue: 5 };

/*
 * 워니 알림 원칙
 *  - 사용자가 동의한 종류만: ① 결제 하루 전 알림 ② 구독료 인상·변경 알림
 *  - 보낼 '중요한 사건'이 있는 날에만, 오후 1~2시 사이 1회 (매일 보내는 알림 없음)
 */
export const NUDGE_WINDOW = { startHour: 13, endHour: 14 };

export const NOTIF_TYPES = [
  {
    id: 'd1',
    title: '결제 하루 전 알림',
    desc: '정기결제가 있는 날 전날, 오후 1~2시에 한 번',
    example: `예) 내일 왓챠 7,900원이 결제돼요`,
  },
  {
    id: 'price',
    title: '구독료 인상·변경 알림',
    desc: '요금 인상·무료체험 종료·결제 실패가 확인될 때만',
    example: '예) 다음 결제부터 요금이 1,500원 올라요',
  },
];
