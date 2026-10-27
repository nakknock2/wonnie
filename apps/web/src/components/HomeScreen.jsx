import { useCallback, useEffect, useRef, useState } from 'react';
import Header from './Header.jsx';
import PromoBanner from './PromoBanner.jsx';
import HeroScene from './HeroScene.jsx';
import { OtherBankCard, AccountCard } from './AccountSection.jsx';
import { SavingsPromo, ServiceCarousel, GroupServices, NoticeBar } from './ServiceSection.jsx';
import BottomNav from './BottomNav.jsx';
import {
  BRAND, PROMO_BANNERS, OTHER_BANKS, MAIN_ACCOUNT, SERVICE_GROUPS, GROUP_SERVICES, NOTICE, TABS,
} from '../data/homeData.js';
import '../styles/home.css';

// 토스트 메시지 훅 (버튼 눌렀을 때 "눌렸다"는 피드백용)
function useToast() {
  const [msg, setMsg] = useState(null);
  const timer = useRef();
  const show = useCallback((text) => {
    clearTimeout(timer.current);
    setMsg(text);
    timer.current = setTimeout(() => setMsg(null), 1800);
  }, []);
  useEffect(() => () => clearTimeout(timer.current), []);
  return [msg, show];
}

// demo: 데모 시나리오 상태 (잔액 · 워니 절약 적금 · 구독 현황 반영)
export default function HomeScreen({ onOpenChat, demo }) {
  const [easyMode, setEasyMode] = useState(false);
  const [hideBalance, setHideBalance] = useState(true);
  const [showPromo, setShowPromo] = useState(true);
  const [showOtherBank, setShowOtherBank] = useState(true);
  const [activeTab, setActiveTab] = useState('home');
  const [toast, showToast] = useToast();

  const soon = (name) => showToast(`'${name}' 화면은 준비 중이에요`);

  const copyNumber = async (num) => {
    try {
      await navigator.clipboard.writeText(num);
    } catch {
      /* 클립보드 권한이 없는 환경에서는 무시 */
    }
    showToast('계좌번호가 복사되었어요');
  };

  const tabLabel = TABS.find((t) => t.id === activeTab)?.label;

  return (
    <div className={`app ${easyMode ? 'is-easy' : ''}`}>
      <Header
        easyMode={easyMode}
        onToggleMode={(v) => {
          setEasyMode(v);
          showToast(v ? '쉬운 홈으로 전환했어요' : '일반홈으로 전환했어요');
        }}
        onAction={(name) => (name === 'AI 상담' && onOpenChat ? onOpenChat() : soon(name))}
        hasNotice
      />

      <main className="app-scroll">
        {activeTab === 'home' ? (
          <>
            <div className="top-bg">
              {showPromo && (
                <PromoBanner
                  banners={PROMO_BANNERS}
                  onClose={() => setShowPromo(false)}
                  onClick={(b) => soon(b.line1)}
                />
              )}
              <HeroScene
                hideBalance={hideBalance}
                onToggleHide={(v) => {
                  setHideBalance(v);
                  showToast(v ? '잔액을 숨겼어요' : '잔액을 표시해요');
                }}
                onSettings={() => soon('홈계좌설정')}
              />
            </div>

            <div className="content">
              {showOtherBank && (
                <OtherBankCard
                  banks={OTHER_BANKS}
                  onClose={() => setShowOtherBank(false)}
                  onAdd={() => soon('다른 은행 계좌 추가')}
                />
              )}

              <AccountCard
                account={demo ? { ...MAIN_ACCOUNT, balance: demo.balance } : MAIN_ACCOUNT}
                hideBalance={hideBalance}
                onCopy={copyNumber}
                onTransfer={() => soon('이체')}
                onMenu={soon}
                onRevealOnce={() => setHideBalance((v) => !v)}
              />

              <button className="btn-block" onClick={() => soon('전체계좌보기')}>전체계좌보기</button>

              <SavingsPromo onClick={() => soon('적금 이벤트')} />

              <ServiceCarousel groups={SERVICE_GROUPS} onClick={soon} />

              <GroupServices title={`${BRAND.group} 서비스`} items={GROUP_SERVICES} onClick={soon} />

              <NoticeBar text={NOTICE} onClick={() => soon('공지사항')} />

              <p className="footer-note">본 화면은 해커톤용 UI 목업이며 실제 금융 서비스가 아닙니다.</p>
            </div>
          </>
        ) : (
          <div className="placeholder">
            <p className="placeholder__title">{tabLabel}</p>
            <p className="placeholder__desc">이 탭은 아직 준비 중이에요.<br />하단의 ‘홈’을 눌러 돌아가세요.</p>
          </div>
        )}
      </main>

      <BottomNav tabs={TABS} active={activeTab} onChange={setActiveTab} />

      <div className={`toast ${toast ? 'is-show' : ''}`} role="status">{toast}</div>
    </div>
  );
}
