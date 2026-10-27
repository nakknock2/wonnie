import { useCallback, useEffect, useRef, useState } from 'react';
import wonnieImg from '../../assets/wonnie-3d.png';
import wonnieFace from '../../assets/wonnie-3d-face.png';
import { ConsentForm } from '../Consent/Consent.jsx';
import CancelPage from './CancelPage.jsx';
import TransferSheet from './TransferSheet.jsx';
import NotifConsent, { OsPermission } from './NotifConsent.jsx';
import { ProposalCard, ProposalReply, ScenarioSheet } from './Proposals.jsx';
import { engine, calendar } from '../../proposals/index.js';
import {
  CUSTOMER, SUBSCRIPTIONS, WATCHA, MONTHLY_TOTAL, SAVINGS,
  BASE_DATE, addDays, billDateOf, dueLabel, won, md, mdw, ymd, NOTIF_TYPES, PRICE_CHANGE,
} from '../../data/subscriptions.js';
import { apiDueLabel, mergeChatMessages, toSubscriptionCard, wonnieApi } from '../../services/wonnieApi.js';
import './WonnieChatScreen.css';

/*
 * 워니 상담 화면 (폰 화면 전체를 덮는 풀스크린) – 'WONNIE와 대화중' 데모 디자인
 *
 * 데모 시나리오(김우리 · 구독 절약)를 대화로 진행합니다.
 *  request.mode
 *   - 'default'  : 일반 진입
 *   - 'scan'     : 동의 직후 → 카드 결제내역에서 구독 찾기
 *   - 'reminder' : 결제 D-1 알림 → "내일 왓챠 7,900원이 결제돼요. 계속 볼 거예요?"
 *   - 'result'   : 날짜 넘긴 뒤 → 결제 안 들어온 것 확인 → 적금 제안
 *   - 'price'    : 구독료 인상 알림 (예시 시나리오)
 *   - 'picker'   : 시나리오 고르기 시트 열기
 *
 * 대화 모드
 *   - 'subs'     : 김우리 구독 절약 데모 (기본)
 *   - 'proposal' : 시나리오별 절약 제안 (팀 3.agent-proposals 의 데이터·응답 엔진 사용)
 *
 * 알림은 구독 조회 후 대화 안에서 종류별로 따로 동의를 받고(기본 꺼짐),
 * 동의한 종류의 '중요한 사건'이 있는 날에만 오후 1~2시에 한 번 보냅니다.
 */

/* ---------- 아이콘 ---------- */
const sv = { fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round', strokeLinejoin: 'round' };
const Ico = {
  back: <svg viewBox="0 0 24 24" width="24" height="24" {...sv}><path d="M15 5l-7 7 7 7" /></svg>,
  chat: <svg viewBox="0 0 24 24" width="20" height="20" {...sv} strokeWidth="1.8"><path d="M4 5h16v11H9l-5 4V5Z" /></svg>,
  hide: <svg viewBox="0 0 24 24" width="20" height="20" {...sv} strokeWidth="1.8"><path d="M3 3l18 18" /><path d="M10.6 5.1A9.8 9.8 0 0 1 12 5c5 0 9 4.5 10 7a12.6 12.6 0 0 1-2.6 3.6M6.6 6.6C4.3 8 2.7 10.2 2 12c1 2.5 5 7 10 7a9.6 9.6 0 0 0 4.4-1.1" /><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" /></svg>,
  reset: <svg viewBox="0 0 24 24" width="20" height="20" {...sv} strokeWidth="1.8"><path d="M3 12a9 9 0 1 0 3-6.7L3 8" /><path d="M3 3v5h5" /></svg>,
  chevron: <svg viewBox="0 0 24 24" width="24" height="24" {...sv}><path d="M9 5l7 7-7 7" /></svg>,
  plus: <svg viewBox="0 0 24 24" width="24" height="24" {...sv} strokeWidth="1.8"><path d="M12 4v16M4 12h16" /></svg>,
  mic: <svg viewBox="0 0 24 24" width="23" height="23" fill="none" stroke="#2789ec" strokeWidth="1.9" strokeLinecap="round"><rect x="9" y="3" width="6" height="11" rx="3" /><path d="M5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21M9 21h6" /></svg>,
  send: <svg viewBox="0 0 24 24" width="23" height="23" {...sv} strokeWidth="1.8"><path d="M21 3 10 14" /><path d="M21 3l-7 18-4-7-7-4 18-7Z" /></svg>,
  link: <svg viewBox="0 0 24 24" width="16" height="16" {...sv}><path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5" /></svg>,
};

function SubIcon({ sub, size = 36 }) {
  return (
    <span className="wc-sub-icon" style={{ background: sub.color, width: size, height: size }} aria-hidden="true">
      {sub.letter === 'play'
        ? <svg viewBox="0 0 24 24" width={size * 0.45} height={size * 0.45}><path d="M8 5.5v13l11-6.5Z" fill="#fff" /></svg>
        : <span style={{ fontSize: sub.letter.length > 1 ? size * 0.34 : size * 0.44 }}>{sub.letter}</span>}
    </span>
  );
}

function SubRow({ sub, badge, tone = 'blue', note }) {
  return (
    <div className={`wc-row ${tone === 'muted' ? 'is-muted' : ''}`}>
      <SubIcon sub={sub} />
      <span className="wc-row__copy">
        <strong>{sub.name}</strong>
        <small>{note ?? `월 ${won(sub.price)}`}</small>
      </span>
      <span className={`wc-row__badge wc-row__badge--${tone}`}>{badge}</span>
    </div>
  );
}

const nowTime = () => {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

// 워니 답변 틀 (아바타 + 한 줄 소개 + 카드). 컴포넌트 밖에 둬야 안의 입력 상태가 유지돼요.
function Reply({ intro, children }) {
  return (
    <div className="wc-reply">
      <img className="wc-reply-avatar" src={wonnieFace} alt="" />
      <p className="wc-reply-intro">{intro}</p>
      {children}
    </div>
  );
}

let seq = 1;
const mk = (from, kind, extra = {}) => ({ id: seq++, from, kind, time: nowTime(), createdAt: Date.now(), ...extra });

export default function WonnieChatScreen({ open, request, demo, dispatch, onClose, backend }) {
  const [messages, setMessages] = useState([]);
  const [typing, setTyping] = useState(false);
  const [fetchingSubscriptions, setFetchingSubscriptions] = useState(false);
  const [historyAfter, setHistoryAfter] = useState(null);
  const scanningRef = useRef(false);
  const scanGeneration = useRef(0);
  const [input, setInput] = useState('');
  const [hidden, setHidden] = useState(false);
  const [listening, setListening] = useState(false);
  const [toast, setToast] = useState(null);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [transferOpen, setTransferOpen] = useState(false);
  const [declinedOffer, setDeclinedOffer] = useState(false);
  const [osAsk, setOsAsk] = useState(null);           // 기기 알림 권한 팝업 대기 중인 선택값
  const [priceAnswer, setPriceAnswer] = useState(null);
  const [mode, setMode] = useState('subs');            // 'subs' | 'proposal'
  const [activeId, setActiveId] = useState(null);      // 지금 보고 있는 제안 시나리오
  const [sheet, setSheet] = useState(null);            // null | 'list' | 'calendar'
  const convRef = useRef(null);
  const timers = useRef([]);
  const queue = useRef(Promise.resolve());
  const gen = useRef(0);
  const handledReq = useRef(null);
  const demoRef = useRef(demo);
  demoRef.current = demo;
  const modeRef = useRef(mode);
  modeRef.current = mode;
  const resetRef = useRef(() => {});
  const busy = typing || fetchingSubscriptions || backend.sending;
  const serverMessages = historyAfter === null ? backend.messages : backend.messages.filter((m) => m.createdAt > historyAfter);
  const visibleMessages = mergeChatMessages(messages, serverMessages, backend.pendingMessage ? [backend.pendingMessage] : []);

  useEffect(() => { if (open) setHistoryAfter(null); }, [open]);

  const later = (fn, ms) => timers.current.push(setTimeout(fn, ms));
  useEffect(() => () => { timers.current.forEach(clearTimeout); scanGeneration.current += 1; }, []);

  const showToast = useCallback((t) => {
    setToast(t);
    later(() => setToast(null), 1800);
  }, []);

  useEffect(() => { if (open && backend.error) showToast(backend.error); }, [open, backend.error, showToast]);

  const push = useCallback((m) => setMessages((ms) => [...ms, m]), []);

  // 워니가 순서대로 말하기 (타이핑 표시 후 메시지 추가)
  const say = useCallback((m, delay = 850) => {
    const g = gen.current;
    queue.current = queue.current.then(() => new Promise((res) => {
      if (g !== gen.current) { res(); return; }
      setTyping(true);
      later(() => {
        if (g === gen.current) {
          setTyping(false);
          if (typeof m === 'function') m(); else push(m);
        }
        res();
      }, delay);
    }));
  }, [push]);

  const userSays = (text) => push(mk('user', 'text', { text }));

  // 새 메시지 → 맨 아래로
  useEffect(() => {
    const el = convRef.current;
    if (el && visibleMessages.length) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
  }, [messages, backend.messages, backend.pendingMessage, busy]);

  /* ----- 시나리오 단계 ----- */
  const startScan = useCallback(async () => {
    if (scanningRef.current) return;
    scanningRef.current = true;
    const generation = ++scanGeneration.current;
    const scanning = mk('assistant', 'scanning');
    push(scanning);
    setFetchingSubscriptions(true);
    try {
      const [report, dashboard] = await Promise.all([
        wonnieApi.subscriptions(),
        wonnieApi.dashboard().catch(() => null),
      ]);
      if (generation !== scanGeneration.current) return;
      if (!report.data?.subscriptions) throw new Error(report.summary || '구독 정보를 불러오지 못했어요.');
      setMessages((ms) => ms.filter((m) => m.id !== scanning.id));
      push(mk('assistant', 'found', { subscriptionCard: toSubscriptionCard(report, dashboard) }));
      dispatch({ type: 'SCANNED' });
      say(() => { if (!demoRef.current.notif.asked) push(mk('assistant', 'notif-ask')); }, 900);
    } catch (error) {
      if (generation === scanGeneration.current) {
        setMessages((ms) => ms.filter((m) => m.id !== scanning.id));
        showToast(error.message);
      }
    } finally {
      if (generation === scanGeneration.current) {
        setFetchingSubscriptions(false);
        scanningRef.current = false;
      }
    }
  }, [say, push, dispatch, showToast]);

  /* ----- 알림 동의 ----- */
  const notifLabel = (sel) => NOTIF_TYPES.filter((t) => sel[t.id]).map((t) => t.title).join(', ');

  const submitNotif = (msgId, sel) => {
    setMessages((ms) => ms.map((m) => (m.id === msgId ? { ...m, locked: true, value: sel } : m)));
    const any = sel.d1 || sel.price;
    userSays(any ? `${notifLabel(sel)} 받을게요` : '알림은 안 받을게요');
    if (any) later(() => setOsAsk(sel), 500);
    else {
      dispatch({ type: 'NOTIF_SET', value: { d1: false, price: false, os: null } });
      say(mk('assistant', 'notif-done', { value: { d1: false, price: false, os: null } }));
    }
  };

  const answerOs = (granted) => {
    const value = { ...osAsk, os: granted ? 'granted' : 'denied' };
    setOsAsk(null);
    dispatch({ type: 'NOTIF_SET', value });
    say(mk('assistant', 'notif-done', { value }), 600);
  };

  const askNotif = () => say(mk('assistant', 'notif-ask'));

  const showSubs = useCallback(() => {
    const d = demoRef.current;
    if (!d.consented) say(mk('assistant', 'consent'));
    else startScan();
  }, [say, startScan]);

  const onConsent = () => {
    dispatch({ type: 'CONSENT' });
    userSays('동의할게요');
    say(mk('assistant', 'text', { text: '동의해 주셔서 고마워요! 카드 결제내역에서 정기결제를 찾아볼게요.' }), 700);
    startScan();
  };

  const answerReminder = (keep) => {
    if (keep) {
      userSays('응, 계속 볼래');
      dispatch({ type: 'DECIDE', value: 'keep' });
      say(mk('assistant', 'text', { text: `좋아요! 내일 ${WATCHA.name} ${won(WATCHA.price)}이 결제돼요.\n즐거운 시청 되세요 🍿` }));
    } else {
      userSays('아니');
      dispatch({ type: 'DECIDE', value: 'cancel' });
      say(mk('assistant', 'cancel-link'));
    }
  };

  const onCancelled = useCallback(() => {
    dispatch({ type: 'CANCELLED' });
    say(mk('assistant', 'text', { text: `${WATCHA.name} 해지 완료를 확인했어요 ✅\n내일 ${mdw(billDateOf(WATCHA))} 결제가 정말 안 나가는지 워니가 지켜볼게요 👀` }), 500);
  }, [dispatch, say]);

  const showResult = useCallback(() => {
    const d = demoRef.current;
    push(mk('system', 'date', { text: ymd(addDays(BASE_DATE, d.day)) }));
    if (d.cancelled) {
      dispatch({ type: 'RESULT_SHOWN' });
      say(mk('assistant', 'result'), 900);
      say(mk('assistant', 'offer'), 1100);
    } else {
      say(mk('assistant', 'charged'), 900);
    }
  }, [push, say, dispatch]);

  const acceptOffer = () => {
    userSays('응, 넣어줘');
    later(() => setTransferOpen(true), 350);
  };
  const declineOffer = () => {
    setDeclinedOffer(true);
    userSays('다음에');
    say(mk('assistant', 'text', { text: `알겠어요. 아낀 ${won(WATCHA.price)}은 언제든 적금으로 옮길 수 있어요.` }));
  };
  const onTransferDone = (amount) => {
    setTransferOpen(false);
    dispatch({ type: 'TRANSFER', amount });
    say(mk('assistant', 'saved', { amount }), 700);
  };

  // 외부 요청 처리 (동의 직후 / D-1 알림 / 날짜 넘김)
  useEffect(() => {
    if (!open || !request || handledReq.current === request.id) return;
    handledReq.current = request.id;
    setHidden(false);
    if (request.mode === 'picker') { setSheet('list'); return; }
    // 알림·동의 흐름은 김우리 데모 대화로 돌아와서 보여줘요
    if (modeRef.current === 'proposal' && ['scan', 'reminder', 'result', 'price'].includes(request.mode)) { resetRef.current(); setMode('subs'); }
    if (request.mode === 'scan') {
      say(mk('assistant', 'text', { text: `${CUSTOMER.name}님, 동의해 주셔서 고마워요! 카드 결제내역에서 정기결제를 찾아볼게요.` }), 500);
      startScan();
    } else if (request.mode === 'reminder') {
      say(mk('assistant', 'reminder'), 700);
    } else if (request.mode === 'result') {
      showResult();
    } else if (request.mode === 'price') {
      say(mk('assistant', 'price'), 700);
    }
  }, [open, request, say, startScan, showResult]);

  // ESC 로 닫기
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => {
      if (e.key !== 'Escape') return;
      if (sheet) setSheet(null);
      else if (cancelOpen) setCancelOpen(false);
      else if (transferOpen) setTransferOpen(false);
      else onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose, cancelOpen, transferOpen, sheet]);

  /* ----- 시나리오별 절약 제안 ----- */
  const startProposal = (scenario) => {
    reset();
    setSheet(null);
    setMode('proposal');
    setActiveId(scenario ? scenario.id : null);
    push(mk('assistant', 'proposal', { scenarioId: scenario ? scenario.id : null, description: scenario ? engine.choose(scenario.messages) : '' }));
    later(() => convRef.current?.scrollTo({ top: 0 }), 30);
  };
  const nextProposal = () => startProposal(engine.next(activeId));
  const pickProposal = (id) => startProposal(engine.get(id));
  const backToSubs = () => { reset(); setSheet(null); setMode('subs'); };

  const sendToBackend = async (text, topic) => {
    try {
      await backend.send(text, topic);
    } catch {
      setInput(text);
    }
  };

  const proposalAsk = (q) => {
    if (busy) return;
    const scenario = activeId ? engine.get(activeId) : null;
    // Calendar scenes remain explicitly labeled client-side previews; other questions use the API.
    if (scenario?.source !== 'calendar-demo') {
      void sendToBackend(q, scenario?.title);
      return;
    }
    const answer = engine.resolve(q, activeId);
    if (answer.kind === 'next') { nextProposal(); return; }
    userSays(q);
    say(() => { setActiveId(answer.scenarioId); push(mk('assistant', 'proposal-reply', { answer })); }, 600);
  };

  const ask = (text) => {
    const t = text.trim();
    if (!t || busy) return;
    if (t.length > 4000) { showToast('메시지는 4,000자 이내로 입력해 주세요.'); return; }
    if (mode === 'proposal') {
      if (/김우리|구독\s*절약\s*데모|기본\s*데모/.test(t)) { backToSubs(); return; }
      proposalAsk(t);
      return;
    }
    if (/^알림\s*(설정|설정 바꿀래|받기 설정)/.test(t)) {
      userSays(t);
      if (!demoRef.current.scanned) showSubs(); else askNotif();
      return;
    }
    void sendToBackend(t);
  };

  const reset = () => {
    // Returning to the welcome view does not delete the server conversation.
    setHistoryAfter(Date.now());
    scanGeneration.current += 1;
    scanningRef.current = false;
    setFetchingSubscriptions(false);
    gen.current += 1;
    queue.current = Promise.resolve();
    timers.current.forEach(clearTimeout);
    timers.current = [];
    setMessages([]);
    setTyping(false);
    setHidden(false);
    setInput('');
  };
  resetRef.current = reset;
  const goHome = () => { reset(); setMode('subs'); };

  const today = addDays(BASE_DATE, demo.day);
  const activeTotal = MONTHLY_TOTAL - (demo.cancelled ? WATCHA.price : 0);
  const hasMessages = visibleMessages.length > 0 || busy;
  const tab = open && !cancelOpen && !transferOpen && !osAsk && !sheet ? 0 : -1;
  const lastId = visibleMessages.length ? visibleMessages[visibleMessages.length - 1].id : null;
  const lastProposalId = [...messages].reverse().find((m) => m.kind === 'proposal')?.id;

  /* ----- 메시지 렌더링 ----- */
  const renderAssistant = (m) => {
    switch (m.kind) {
      case 'consent':
        return (
          <Reply intro="구독을 찾으려면 동의가 필요해요.">
            <div className="wc-card">
              <ConsentForm compact disabled={demo.consented} onAgree={onConsent} />
            </div>
          </Reply>
        );
      case 'scanning':
        return (
          <Reply intro="카드 결제내역을 살펴보고 있어요…">
            <div className="wc-card wc-scan">
              <div className="wc-scan__bar"><i /></div>
              <p>카드 결제내역에서 <b>정기결제 정보</b>를 조회하는 중</p>
            </div>
          </Reply>
        );
      case 'found': {
        const found = m.subscriptionCard;
        if (!found) return null;
        return (
          <Reply intro="카드 결제에서 정기결제를 찾았어요.">
            <div className="wc-card">
              <div className="wc-card__head">
                <div>
                  <small>카드 결제에서 찾은 구독 {found.subscriptions.length}개</small>
                  <strong className="wc-big">매달 {won(found.monthlyTotal)}</strong>
                </div>
                <span className="wc-sample-badge">예시 데이터</span>
              </div>
              <div className="wc-rows">
                {found.subscriptions.map((s) => {
                  const cancelled = false;
                  const label = apiDueLabel(s.nextPaymentDate);
                  return (
                    <SubRow key={s.id} sub={s}
                      tone={cancelled ? 'muted' : label === '내일 결제' || label === '오늘 결제' ? 'red' : 'blue'}
                      note={`${s.billingCycle} ${won(s.price)}`}
                      badge={label} />
                  );
                })}
              </div>
              {demo.cancelled && !m.subscriptionCard && <p className="wc-card__foot">정리 후 매달 <b>{won(activeTotal)}</b></p>}
            </div>
          </Reply>
        );
      }
      case 'reminder': {
        const answered = demo.decision !== null || demo.cancelled;
        return (
          <Reply intro={<><span className="wc-bell" aria-hidden="true">🔔</span> 결제 D-1 알림</>}>
            <div className="wc-bubble wc-bubble--assistant wc-bubble--wide">
              <b>내일 {WATCHA.name} {won(WATCHA.price)}이 결제돼요.</b>
              {'\n'}계속 볼 거예요?
            </div>
            <div className="wc-card wc-card--tight">
              <SubRow sub={WATCHA} tone="red" badge="내일 결제" note={`${mdw(billDateOf(WATCHA))} · 마지막 시청 ${WATCHA.lastUsed}`} />
            </div>
            <div className="wc-choices">
              <button type="button" disabled={answered} onClick={() => answerReminder(true)} tabIndex={tab}>응, 계속 볼래</button>
              <button type="button" className="is-primary" disabled={answered} onClick={() => answerReminder(false)} tabIndex={tab}>아니</button>
            </div>
            <p className="wc-why">동의하신 '결제 하루 전 알림'이에요 · 이 결제 건은 한 번만 안내해요</p>
          </Reply>
        );
      }
      case 'notif-ask': {
        const locked = !!m.locked;
        return (
          <Reply intro={locked ? '알림 받기' : '알림은 먼저 여쭤볼게요'}>
            <NotifConsent initial={m.value ?? demo.notif} locked={locked} onSubmit={(sel) => submitNotif(m.id, sel)} />
          </Reply>
        );
      }
      case 'notif-done': {
        const v = m.value;
        const any = v.d1 || v.price;
        return (
          <Reply intro={any ? '알림 설정을 마쳤어요' : '알림을 받지 않기로 했어요'}>
            <div className="ns">
              <div className="ns-title"><i className={any ? '' : 'is-off'}>{any ? '✓' : '–'}</i>{any ? '워니 알림 설정 완료' : '알림 꺼짐'}</div>
              <div className="ns-rows">
                {NOTIF_TYPES.map((t) => (
                  <div key={t.id} className="ns-row">{t.title}<b className={v[t.id] ? '' : 'is-off'}>{v[t.id] ? '받음' : '안 받음'}</b></div>
                ))}
                {any && (
                  <div className="ns-row">기기 알림<b className={v.os === 'granted' ? '' : 'is-off'}>{v.os === 'granted' ? '허용' : '꺼짐 · 앱 안에서만'}</b></div>
                )}
              </div>
              <small>
                {any
                  ? `중요한 일이 있는 날에만 오후 1~2시에 한 번 보내요. 매일 보내는 알림은 없어요.${v.os === 'granted' ? '' : '\n기기 알림이 꺼져 있어서, 앱에 들어오면 워니 버튼으로 알려드릴게요.'}`
                  : '구독 정보는 원하실 때 대화에서 언제든 확인할 수 있어요.'}
              </small>
              <button type="button" className="ns-edit" onClick={() => { userSays('알림 설정 바꿀래'); askNotif(); }} tabIndex={tab}>알림 설정 바꾸기</button>
            </div>
          </Reply>
        );
      }
      case 'price': {
        const sub = SUBSCRIPTIONS.find((x) => x.id === PRICE_CHANGE.subId);
        const diff = PRICE_CHANGE.to - PRICE_CHANGE.from;
        const eff = billDateOf({ due: PRICE_CHANGE.effectiveDue });
        return (
          <Reply intro={<><span className="wc-bell" aria-hidden="true">🔔</span> 구독료 인상 안내</>}>
            <div className="wc-bubble wc-bubble--assistant wc-bubble--wide">
              <b>{sub.name} 요금이 다음 결제({md(eff)})부터 올라요.</b>{'\n'}한 달에 {won(diff)}, 1년이면 {won(diff * 12)} 더 나가요.
            </div>
            <div className="wc-card">
              <SubRow sub={sub} tone="red" badge="요금 인상" note={`${sub.plan} · ${mdw(eff)} 결제분부터`} />
              <div className="wc-price"><s>{won(PRICE_CHANGE.from)}</s><b>{won(PRICE_CHANGE.to)}</b><em>+{won(diff)}</em></div>
            </div>
            <div className="wc-choices">
              <button type="button" disabled={!!priceAnswer} onClick={() => { setPriceAnswer('keep'); userSays('그대로 쓸게요'); say(mk('assistant', 'text', { text: `알겠어요. ${md(eff)} 결제 전날에 바뀐 금액으로 한 번 더 알려드릴게요.` })); }} tabIndex={tab}>그대로 쓸게요</button>
              <button type="button" className="is-primary" disabled={!!priceAnswer} onClick={() => { setPriceAnswer('plans'); userSays('요금제 비교해줘'); say(mk('assistant', 'plans')); }} tabIndex={tab}>요금제 비교해줘</button>
            </div>
            <p className="wc-why">동의하신 '구독료 인상·변경 알림'이에요 · 예시 시나리오</p>
          </Reply>
        );
      }
      case 'plans': {
        const sub = SUBSCRIPTIONS.find((x) => x.id === PRICE_CHANGE.subId);
        const plans = [
          { name: `${sub.plan} (인상 후)`, price: PRICE_CHANGE.to, now: true },
          { name: '스탠다드', price: 13500 },
          { name: '광고형 스탠다드', price: 5500 },
        ];
        return (
          <Reply intro="요금제를 바꾸면 이만큼 아낄 수 있어요 (예시 금액)">
            <div className="wc-card">
              <div className="wc-plans">
                {plans.map((p) => (
                  <div key={p.name} className={`wc-plan ${p.now ? 'is-now' : ''}`}>
                    <span>{p.name}</span>
                    <span><b>{won(p.price)}</b>{!p.now && <small>-{won(PRICE_CHANGE.to - p.price)}</small>}</span>
                  </div>
                ))}
              </div>
              <p className="wc-card__foot">요금제 변경은 {sub.name} 앱·웹에서 할 수 있어요.</p>
            </div>
          </Reply>
        );
      }
      case 'cancel-link':
        return (
          <Reply intro="오늘 해지하면 내일 결제를 막을 수 있어요.">
            <button type="button" className="wc-linkcard" onClick={() => setCancelOpen(true)} disabled={demo.cancelled} tabIndex={tab}>
              <SubIcon sub={WATCHA} size={40} />
              <span className="wc-linkcard__copy">
                <strong>{WATCHA.name} 해지 페이지로 연결할게요</strong>
                <small>{demo.cancelled ? '해지 완료' : `${WATCHA.name} · 구독 관리 > 해지`}</small>
              </span>
              <span className="wc-linkcard__go">{demo.cancelled ? '완료' : <>{Ico.link}</>}</span>
            </button>
          </Reply>
        );
      case 'result':
        return (
          <Reply intro={`오늘은 ${WATCHA.name} 결제일이었어요.`}>
            <div className="wc-bubble wc-bubble--assistant wc-bubble--wide">
              <b>결제가 들어오지 않았어요 ✅</b>{'\n'}어제 해지한 {WATCHA.name}, 오늘 카드 결제내역에서 청구되지 않은 걸 확인했어요.
            </div>
            <div className="wc-card">
              <div className="wc-card__head">
                <div>
                  <small>오늘 카드 결제내역 · {mdw(today)}</small>
                  <strong>{WATCHA.card}</strong>
                </div>
              </div>
              <div className="wc-receipt">
                <SubIcon sub={WATCHA} size={32} />
                <span className="wc-receipt__copy">
                  <strong>{WATCHA.name} 정기결제</strong>
                  <small>예정이었던 결제</small>
                </span>
                <span className="wc-receipt__amt"><s>{won(WATCHA.price)}</s><b>청구 없음</b></span>
              </div>
            </div>
          </Reply>
        );
      case 'offer': {
        const answered = demo.transferred || declinedOffer || transferOpen;
        return (
          <Reply intro="워니의 제안">
            <div className="wc-bubble wc-bubble--assistant wc-bubble--wide">
              <b>{WATCHA.name} {won(WATCHA.price)} 아꼈어요 🎉</b>{'\n'}그만큼 적금에 넣을까요?
            </div>
            <div className="wc-choices">
              <button type="button" disabled={answered} onClick={declineOffer} tabIndex={tab}>다음에</button>
              <button type="button" className="is-primary" disabled={answered} onClick={acceptOffer} tabIndex={tab}>응, 넣어줘</button>
            </div>
          </Reply>
        );
      }
      case 'saved':
        return (
          <Reply intro={`${SAVINGS.name}에 ${won(m.amount)}을 넣었어요 🌱`}>
            <div className="wc-card wc-saved">
              <small>워니와 함께 아낀 돈</small>
              <strong className="wc-saved__total">{won(demo.savedTotal)}</strong>
              <div className="wc-saved__bar"><i style={{ width: `${Math.min(100, (demo.savedTotal / 100000) * 100)}%` }} /></div>
              <span className="wc-saved__goal">첫 목표 100,000원까지 {won(Math.max(0, 100000 - demo.savedTotal))}</span>
              <dl>
                <div><dt>정리한 구독</dt><dd>{demo.savedItems.map((i) => i.name).join(', ') || '-'}</dd></div>
                <div><dt>월 구독료</dt><dd><s>{won(MONTHLY_TOTAL)}</s> → <b>{won(activeTotal)}</b></dd></div>
                <div><dt>1년 유지하면</dt><dd className="is-blue">{won(WATCHA.price * 12)} 절약</dd></div>
                <div><dt>{SAVINGS.name}</dt><dd>{won(demo.savingsBalance)}</dd></div>
              </dl>
            </div>
          </Reply>
        );
      case 'charged':
        return (
          <Reply intro={`오늘은 ${WATCHA.name} 결제일이었어요.`}>
            <div className="wc-card wc-card--tight">
              <SubRow sub={WATCHA} tone="muted" badge="결제 완료" note={`${mdw(today)} · ${won(WATCHA.price)} 결제됨`} />
            </div>
          </Reply>
        );
      case 'proposal':
        return (
          <ProposalCard scenarioId={m.scenarioId} description={m.description} latest={m.id === lastProposalId}
            busy={busy} onAsk={proposalAsk} onNext={nextProposal} onPick={() => setSheet('list')} tab={tab} />
        );
      case 'proposal-reply':
        return <ProposalReply answer={m.answer} latest={m.id === lastId} busy={busy} onAsk={proposalAsk} tab={tab} />;
      default:
        return <div className="wc-bubble wc-bubble--assistant">{m.text}</div>;
    }
  };

  return (
    <div className={`wc-screen ${open ? 'is-open' : ''}`} aria-hidden={!open} role="dialog" aria-modal="true" aria-label="WONNIE와 대화중">
      <header className="wc-header">
        <div className="wc-title-bar">
          <button type="button" className="wc-icon-btn wc-back" aria-label="뒤로가기" onClick={onClose} tabIndex={tab}>{Ico.back}</button>
          <h1>WONNIE와 대화중</h1>
          <div className="wc-gallery" aria-label="WONNIE"><img src={wonnieImg} alt="" /></div>
        </div>
        <nav className="wc-tools" aria-label="채팅 도구">
          <button type="button" onClick={() => setSheet('list')} tabIndex={tab}>{Ico.chat}<span>시나리오</span></button>
          <button type="button" aria-pressed={hidden} onClick={() => setHidden((v) => !v)} tabIndex={tab}>{Ico.hide}<span>상담숨기기</span></button>
          <button type="button" onClick={goHome} tabIndex={tab}>{Ico.reset}<span>처음으로</span></button>
        </nav>
      </header>

      <section className={`wc-conversation ${hasMessages ? 'has-messages' : ''}`} ref={convRef} aria-label="대화 내용">
        <button type="button" className="wc-service-card" onClick={() => showToast('전담직원 상담은 준비 중이에요')} tabIndex={tab}>
          <span className="wc-service-mark">?</span>
          <span className="wc-service-copy"><strong>나만의 전담직원</strong><small>상담서비스</small></span>
          {Ico.chevron}
        </button>

        <div className="wc-date"><span>{ymd(BASE_DATE)}</span></div>
        <div className="wc-connected"><span className="wc-connected-dot" />Wonnie와 연결되었어요</div>
        <button type="button" className="wp-calsrc" onClick={() => setSheet('calendar')} tabIndex={tab}>
          <span>캘린더 예시 일정 {calendar.events.length}건</span><small>실제 연결 전 · 일정 보기 ›</small>
        </button>

        <div aria-live="polite">
          {!hasMessages && (
            <>
              <article className="wc-welcome">
                <h2>{CUSTOMER.name} 고객님, 안녕하세요!</h2>
                <p className="wc-welcome-lead">저는 금융 AI 도우미 WONNIE예요.</p>
                <p className="wc-welcome-detail">궁금한 금융 업무를 편하게 말씀해 주세요.</p>
                <div className="wc-suggestions">
                  <strong>이렇게 물어보세요</strong>
                  <button type="button" onClick={() => ask('구독하고 있는 서비스들 보여줘')} tabIndex={tab}>구독하고 있는 서비스들 보여줘</button>
                  <button type="button" onClick={() => ask('오늘의 절약 제안 보여줘')} tabIndex={tab}>오늘의 절약 제안 보여줘</button>
                  <button type="button" onClick={() => ask('친구에게 이체해줘')} tabIndex={tab}>친구에게 이체해줘</button>
                </div>
                <img className="wc-welcome-mascot" src={wonnieImg} alt="웃으며 반기는 WONNIE" />
              </article>
              <p className="wc-prompt">필요한 금융 업무를 말씀해 주세요</p>
            </>
          )}

          <div className="wc-messages">
            {visibleMessages.map((m) => (m.kind === 'date' ? (
              <div key={m.id} className="wc-date wc-date--inline"><span>{m.text}</span></div>
            ) : (
              <div key={m.id} className={`wc-message ${m.from} ${m.kind !== 'text' ? 'is-card' : ''}`}>
                {m.from === 'user' ? <div className="wc-bubble wc-bubble--user">{m.text}</div> : renderAssistant(m)}
                {m.kind !== 'proposal' && <span className="wc-time">{m.time}</span>}
              </div>
            )))}
            {busy && (
              <div className="wc-message assistant">
                <div className="wc-bubble wc-bubble--assistant wc-typing" aria-label="WONNIE가 답변을 준비하고 있어요"><span /><span /><span /></div>
              </div>
            )}
          </div>
        </div>

        {hidden && (
          <div className="wc-privacy">
            <p>상담 화면을 숨겼어요</p>
            <button type="button" onClick={() => setHidden(false)}>대화 다시 보기</button>
          </div>
        )}
      </section>

      <form className="wc-composer" autoComplete="off" onSubmit={(e) => { e.preventDefault(); if (!input.trim() || busy) return; const text = input; setInput(''); ask(text); }}>
        <button type="button" className="wc-icon-btn wc-attach" aria-label="시나리오 선택" onClick={() => setSheet('list')} tabIndex={tab}>{Ico.plus}</button>
        <div className="wc-input-shell">
          <input type="text" maxLength={4000} value={input} onChange={(e) => setInput(e.target.value)} placeholder={mode === 'proposal' ? '얼마나 절약돼? · 조건 알려줘' : '질문하기 · 메시지를 입력해 주세요'} aria-label="메시지 입력" tabIndex={tab} />
          <button type="button" className={`wc-icon-btn wc-mic ${listening ? 'is-listening' : ''}`} aria-label="음성 입력"
            onClick={() => { setListening((v) => !v); if (!listening) showToast('음성 입력은 준비 중이에요'); }} tabIndex={tab}>{Ico.mic}</button>
          <button type="submit" className="wc-icon-btn wc-send" aria-label="보내기" tabIndex={tab}>{Ico.send}</button>
        </div>
      </form>

      <ScenarioSheet open={!!sheet} tab={sheet} activeId={activeId} mode={mode} onClose={() => setSheet(null)}
        onPickSubs={backToSubs} onPickProposal={pickProposal} />
      <OsPermission open={!!osAsk} onAnswer={answerOs} />
      <CancelPage open={cancelOpen} onClose={() => setCancelOpen(false)} onCancelled={onCancelled} />
      <TransferSheet open={transferOpen} amount={WATCHA.price} balance={demo.balance} onClose={() => setTransferOpen(false)} onDone={onTransferDone} />

      {toast && <div className="wc-toast" role="status">{toast}</div>}
    </div>
  );
}
