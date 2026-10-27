import { useReducer } from 'react';
import { ACCOUNT, WATCHA } from '../data/subscriptions.js';

/*
 * 데모 시나리오 상태 (김우리 · 구독 절약)
 *  앱 진입·동의 → 구독 현황 → D-1 알림 → "아니" → 해지 → 날짜 넘기기 → 미결제 확인 → 적금 모의 이체 → 누적 절약액
 * 실제 돈·실제 해지는 일어나지 않는 모의 상태입니다.
 */
export const initialDemo = {
  day: 0,                 // 데모 날짜 = 시작일 + day
  consented: false,       // 카드 결제내역 조회 동의
  scanned: false,         // 구독 찾기 완료
  reminderSent: false,    // D-1 알림 보냄
  decision: null,         // 'keep' | 'cancel'
  cancelled: false,       // 왓챠 해지 완료
  resultShown: false,     // 결제일 결과(미결제) 확인
  transferred: false,     // 적금 모의 이체 완료
  notif: { asked: false, d1: false, price: false, os: null }, // 알림 동의 (os: 'granted' | 'denied')
  priceSent: false,       // 구독료 인상 알림 보냄
  balance: ACCOUNT.balance,
  savingsBalance: 0,
  savedTotal: 0,
  savedItems: [],         // [{ id, name, amount }]
};

function reducer(s, a) {
  switch (a.type) {
    case 'CONSENT': return { ...s, consented: true };
    case 'SCANNED': return { ...s, scanned: true };
    case 'REMINDER': return { ...s, reminderSent: true };
    case 'DECIDE': return { ...s, decision: a.value };
    case 'CANCELLED': return { ...s, cancelled: true, decision: 'cancel' };
    case 'ADVANCE_DAY': return { ...s, day: s.day + 1 };
    case 'RESULT_SHOWN': return { ...s, resultShown: true };
    case 'TRANSFER':
      return {
        ...s,
        transferred: true,
        balance: s.balance - a.amount,
        savingsBalance: s.savingsBalance + a.amount,
        savedTotal: s.savedTotal + a.amount,
        savedItems: [...s.savedItems, { id: WATCHA.id, name: WATCHA.name, amount: a.amount }],
      };
    case 'NOTIF_SET': return { ...s, notif: { ...s.notif, ...a.value, asked: true } };
    case 'PRICE_SENT': return { ...s, priceSent: true };
    case 'RESET': return initialDemo;
    default: return s;
  }
}

export default function useDemo() {
  return useReducer(reducer, initialDemo);
}

// 데모 패널 체크리스트
export const STEPS = [
  { key: 'consent', label: '앱 진입 · 동의', done: (s) => s.consented },
  { key: 'scan', label: '구독 현황 (7개 · 월 61,300원)', done: (s) => s.scanned },
  { key: 'notif', label: '알림 동의 (D-1 · 인상)', done: (s) => s.notif.asked },
  { key: 'remind', label: '결제 D-1 알림', done: (s) => s.reminderSent },
  { key: 'cancel', label: '"아니" → 왓챠 해지', done: (s) => s.cancelled },
  { key: 'day', label: '날짜 넘기기', done: (s) => s.day >= 1 },
  { key: 'check', label: '결제 안 된 것 확인', done: (s) => s.resultShown },
  { key: 'transfer', label: '적금 모의 이체', done: (s) => s.transferred },
  { key: 'total', label: '누적 절약액', done: (s) => s.transferred },
];
