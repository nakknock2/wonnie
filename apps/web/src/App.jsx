import { useCallback, useEffect, useRef, useState } from "react";
import PhoneFrame from "./components/PhoneFrame.jsx";
import HomeScreen from "./components/HomeScreen.jsx";
import WonnieChatFab from "./components/WonnieChatFab";
import WonnieChatScreen from "./components/WonnieChatScreen/WonnieChatScreen.jsx";
import ConsentSheet from "./components/Consent/Consent.jsx";
import DemoGuidePanel from "./demo/DemoGuidePanel.jsx";
import useDemo from "./demo/useDemo.js";
import useSubscriptionNudge from "./hooks/useSubscriptionNudge.js";
import useWonnieBackend from "./hooks/useWonnieBackend.js";
import { WATCHA } from "./data/subscriptions.js";
import { wonnieApi } from "./services/wonnieApi.js";

export default function App({ showDemoGuide = true, user = null }) {
  const [demo, dispatch] = useDemo();
  const [chatOpen, setChatOpen] = useState(false);
  const [chatRequest, setChatRequest] = useState(null); // { id, mode }
  const [nudgeMode, setNudgeMode] = useState(null); // null | 'reminder' | 'result' | 'price'
  const [consentOpen, setConsentOpen] = useState(false);
  const [consentsLoaded, setConsentsLoaded] = useState(false);
  const [consentSaving, setConsentSaving] = useState(false);
  const [consentError, setConsentError] = useState(null);
  const [faceIdOpen, setFaceIdOpen] = useState(false);
  const backend = useWonnieBackend(chatOpen);
  const chatOpenRef = useRef(false);
  const faceIdRequestRef = useRef(null);
  chatOpenRef.current = chatOpen;

  useEffect(() => {
    const controller = new AbortController();
    wonnieApi.consents(controller.signal)
      .then((value) => dispatch({ type: "HYDRATE_CONSENTS", value }))
      .catch(() => setConsentError("동의 상태를 불러오지 못했어요. 잠시 후 다시 시도해 주세요."))
      .finally(() => setConsentsLoaded(true));
    return () => controller.abort();
  }, [dispatch]);

  // 서버의 동의 상태를 확인한 뒤, 미동의 사용자에게만 앱 진입 1.2초 뒤 동의 시트
  useEffect(() => {
    if (!consentsLoaded || demo.consented) return undefined;
    const t = setTimeout(() => setConsentOpen(true), 1200);
    return () => clearTimeout(t);
  }, [consentsLoaded, demo.consented]);

  const openChat = useCallback((mode = "default") => {
    setChatRequest({ id: Date.now() + Math.random(), mode });
    setChatOpen(true);
    setConsentOpen(false);
  }, []);
  const closeChat = useCallback(() => setChatOpen(false), []);
  const requestFaceId = useCallback(() => new Promise((resolve) => {
    faceIdRequestRef.current?.(false);
    faceIdRequestRef.current = resolve;
    setFaceIdOpen(true);
  }), []);
  const finishFaceId = useCallback((approved) => {
    setFaceIdOpen(false);
    const resolve = faceIdRequestRef.current;
    faceIdRequestRef.current = null;
    resolve?.(approved);
  }, []);

  // 워니 버튼: 알림이 있으면 그 알림으로 열기
  const onFab = useCallback(() => {
    if (nudgeMode) {
      openChat(nudgeMode);
      setNudgeMode(null);
    } else openChat("default");
  }, [nudgeMode, openChat]);

  // 알림 보내기 (상담 화면이 열려 있으면 바로 대화에 표시)
  const deliver = useCallback(
    (mode) => {
      if (chatOpenRef.current) openChat(mode);
      else setNudgeMode(mode);
    },
    [openChat],
  );

  const sendReminder = useCallback(() => {
    dispatch({ type: "REMINDER" });
    deliver("reminder");
  }, [dispatch, deliver]);

  const agree = async () => {
    setConsentSaving(true);
    setConsentError(null);
    try {
      const value = await wonnieApi.agreeToSubscriptionData();
      dispatch({ type: "HYDRATE_CONSENTS", value });
      openChat("scan");
    } catch (error) {
      setConsentError(error.message);
    } finally {
      setConsentSaving(false);
    }
  };

  // 알림 원칙: '결제 하루 전 알림'에 동의했고, 내일 결제 건이 실제로 있을 때만 → 그날 13~14시 사이 1회
  // (매일 보내는 알림 없음 · 동의하지 않은 종류는 보내지 않음)
  const d1Event =
    demo.scanned &&
    demo.notif.d1 &&
    !demo.reminderSent &&
    !demo.cancelled &&
    WATCHA.due - demo.day === 1;
  useSubscriptionNudge(d1Event, sendReminder);

  return (
    <div className="stage">
      {/* PhoneFrame 의 children 은 폰 화면 컨테이너(.phone-screen)의 직계 자식으로 렌더링됨 */}
      <PhoneFrame faceIdOpen={faceIdOpen}
        onFaceIdComplete={() => finishFaceId(true)} onFaceIdCancel={() => finishFaceId(false)}>
        <HomeScreen
          onOpenChat={() => openChat("default")}
          demo={demo}
          user={user}
        />
        <WonnieChatFab
          hidden={chatOpen || consentOpen}
          nudge={!!nudgeMode || backend.unreadCount > 0}
          onOpen={onFab}
        />
        <ConsentSheet
          open={consentOpen && !demo.consented}
          onAgree={agree}
          onClose={() => setConsentOpen(false)}
          saving={consentSaving}
          error={consentError}
        />
        <WonnieChatScreen
          backend={backend}
          open={chatOpen}
          request={chatRequest}
          demo={demo}
          dispatch={dispatch}
          onConsent={async () => {
            const value = await wonnieApi.agreeToSubscriptionData();
            dispatch({ type: "HYDRATE_CONSENTS", value });
          }}
          onAuthenticate={requestFaceId}
          onClose={closeChat}
        />
      </PhoneFrame>
      {showDemoGuide && <DemoGuidePanel />}
    </div>
  );
}
