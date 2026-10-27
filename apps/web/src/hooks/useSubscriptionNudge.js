import { useEffect } from 'react';
import { NUDGE_WINDOW } from '../data/subscriptions.js';

/*
 * 정기구독 결제 D-1 알림(넛지) 자동 타이밍
 *  - enabled 일 때(동의 완료 + 내일 결제 예정 구독 있음)만
 *  - 하루 한 번, 오후 1시~2시 사이의 무작위 시각(그날 하루 고정)에 onFire() 호출
 *  - 이미 표시한 날은 다시 표시하지 않음 (localStorage)
 *  - 데모에서는 패널의 'D-1 알림 보내기' 버튼으로 바로 보낼 수 있어요
 *
 * 실제 서비스에서는 서버 푸시/배치에서 대상과 시각을 정하는 것이 맞아요.
 */
const store = {
  get(k) { try { return window.localStorage.getItem(k); } catch { return null; } },
  set(k, v) { try { window.localStorage.setItem(k, String(v)); } catch { /* 저장 불가 환경은 무시 */ } },
};

const dayKey = (d = new Date()) => `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;

export default function useSubscriptionNudge(enabled, onFire) {
  useEffect(() => {
    if (!enabled) return undefined;

    const key = `wonnie-nudge-${dayKey()}`;
    if (store.get(key) === 'shown') return undefined;

    const now = new Date();
    const start = new Date(now); start.setHours(NUDGE_WINDOW.startHour, 0, 0, 0);
    const end = new Date(now); end.setHours(NUDGE_WINDOW.endHour, 0, 0, 0);
    if (now >= end) return undefined;

    // 오늘의 노출 시각 (13:00~13:55 사이 무작위, 하루 동안 고정)
    let at = Number(store.get(`${key}-at`));
    if (!at) {
      at = start.getTime() + Math.floor(Math.random() * 55 * 60 * 1000);
      store.set(`${key}-at`, at);
    }

    const t = setTimeout(() => {
      if (new Date() >= end) return;
      store.set(key, 'shown');
      onFire();
    }, Math.max(0, at - now.getTime()));
    return () => clearTimeout(t);
  }, [enabled, onFire]);
}
