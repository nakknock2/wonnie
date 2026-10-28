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
    case 'HYDRATE_CONSENTS':
      return {
        ...s,
        consented: a.value.subscriptionDataAgreed,
        notif: {
          ...s.notif,
          asked: a.value.notifications.decided,
          d1: a.value.notifications.d1,
          price: a.value.notifications.priceChange,
        },
      };
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
        savedItems: [...s.savedItems, {
          id: a.subscription?.id ?? WATCHA.id,
          name: a.subscription?.name ?? WATCHA.name,
          amount: a.amount,
        }],
      };
    case 'NOTIF_SET': return { ...s, notif: { ...s.notif, ...a.value, asked: true } };
    case 'PRICE_SENT': return { ...s, priceSent: true };
    case 'RESET':
      return { ...initialDemo, consented: s.consented, notif: s.notif };
    default: return s;
  }
}

export default function useDemo() {
  return useReducer(reducer, initialDemo);
}
