import { useCallback, useEffect, useRef, useState } from 'react';
import PhoneFrame from './components/PhoneFrame.jsx';
import HomeScreen from './components/HomeScreen.jsx';
import WonnieChatFab from './components/WonnieChatFab';
import WonnieChatScreen from './components/WonnieChatScreen/WonnieChatScreen.jsx';
import ConsentSheet from './components/Consent/Consent.jsx';
import DemoPanel from './demo/DemoPanel.jsx';
import useDemo from './demo/useDemo.js';
import useSubscriptionNudge from './hooks/useSubscriptionNudge.js';
import useWonnieBackend from './hooks/useWonnieBackend.js';
import { WATCHA } from './data/subscriptions.js';

export default function App() {
  const [demo, dispatch] = useDemo();
  const [runId, setRunId] = useState(0);            // 처음부터 → 폰 화면 전체 다시 그림
  const [chatOpen, setChatOpen] = useState(false);
  const [chatRequest, setChatRequest] = useState(null); // { id, mode }
  const [nudgeMode, setNudgeMode] = useState(null);     // null | 'reminder' | 'result' | 'price'
  const [consentOpen, setConsentOpen] = useState(false);
  const backend = useWonnieBackend(chatOpen);
  const chatOpenRef = useRef(false);
  chatOpenRef.current = chatOpen;

  // 앱 진입 1.2초 뒤 동의 시트
  useEffect(() => {
    const t = setTimeout(() => setConsentOpen(true), 1200);
    return () => clearTimeout(t);
  }, [runId]);

  const openChat = useCallback((mode = 'default') => {
    setChatRequest({ id: Date.now() + Math.random(), mode });
    setChatOpen(true);
    setConsentOpen(false);
  }, []);
  const closeChat = useCallback(() => setChatOpen(false), []);

  // 워니 버튼: 알림이 있으면 그 알림으로 열기
  const onFab = useCallback(() => {
    if (nudgeMode) {
      openChat(nudgeMode);
      setNudgeMode(null);
    } else openChat('default');
  }, [nudgeMode, openChat]);

  // 알림 보내기 (상담 화면이 열려 있으면 바로 대화에 표시)
  const deliver = useCallback((mode) => {
    if (chatOpenRef.current) openChat(mode);
    else setNudgeMode(mode);
  }, [openChat]);

  const sendReminder = useCallback(() => {
    dispatch({ type: 'REMINDER' });
    deliver('reminder');
  }, [dispatch, deliver]);

  const sendPrice = () => {
    dispatch({ type: 'PRICE_SENT' });
    deliver('price');
  };

  const advanceDay = () => {
    const nextDay = demo.day + 1;
    dispatch({ type: 'ADVANCE_DAY' });
    // 왓챠 결제일이 된 날: 결제 결과 알림
    if (demo.consented && nextDay === WATCHA.due) deliver('result');
  };

  const restart = () => {
    dispatch({ type: 'RESET' });
    setChatOpen(false);
    setChatRequest(null);
    setNudgeMode(null);
    setConsentOpen(false);
    setRunId((n) => n + 1);
  };

  const agree = () => {
    dispatch({ type: 'CONSENT' });
    openChat('scan');
  };

  // 알림 원칙: '결제 하루 전 알림'에 동의했고, 내일 결제 건이 실제로 있을 때만 → 그날 13~14시 사이 1회
  // (매일 보내는 알림 없음 · 동의하지 않은 종류는 보내지 않음)
  const d1Event = demo.scanned && demo.notif.d1 && !demo.reminderSent && !demo.cancelled && WATCHA.due - demo.day === 1;
  const priceEvent = demo.scanned && demo.notif.price && !demo.priceSent;
  useSubscriptionNudge(d1Event, sendReminder);

  return (
    <div className="stage">
      <DemoPanel
        demo={demo}
        canRemind={d1Event}
        onRemind={sendReminder}
        canPrice={priceEvent}
        onPrice={sendPrice}
        onAdvance={advanceDay}
        onRestart={restart}
        onScenarios={() => openChat('picker')}
      />

      {/* PhoneFrame 의 children 은 폰 화면 컨테이너(.phone-screen)의 직계 자식으로 렌더링됨 */}
      <PhoneFrame key={runId}>
        <HomeScreen onOpenChat={() => openChat('default')} demo={demo} />
        <WonnieChatFab hidden={chatOpen || consentOpen} nudge={!!nudgeMode || backend.unreadCount > 0} onOpen={onFab} />
        <ConsentSheet open={consentOpen && !demo.consented} onAgree={agree} onClose={() => setConsentOpen(false)} />
        <WonnieChatScreen backend={backend} open={chatOpen} request={chatRequest} demo={demo} dispatch={dispatch} onClose={closeChat} />
      </PhoneFrame>
    </div>
  );
}
