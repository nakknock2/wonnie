import { useId, useState } from 'react';
import './DemoGuidePanel.css';

const GUIDES = [
  {
    id: 'subscribe',
    label: '시연 1',
    title: '새 구독을 시작할 때',
    description: '요금제를 고르고 혜택 카드를 추천받은 뒤, 절약한 금액을 적금으로 연결해 보세요.',
    steps: [
      { title: '현재 구독 확인', message: '내 구독 현황 알려줘', detail: '채팅창에 입력하고 현재 이용 중인 구독 목록을 확인하세요.' },
      { title: '넷플릭스 구독 시작', message: '나 넷플릭스 구독할래', detail: '채팅창에 입력하면 요금제 선택지가 표시돼요.' },
      { title: '요금제 선택', selection: '스탠다드', detail: '요금제를 선택하고 결제 전 카드 추천을 확인하세요.' },
      { title: '혜택 카드 선택', selection: '카드의정석2 ROUTINE', detail: '추천 카드의 혜택을 확인하세요. 미보유 카드는 발급 단계로 이어져요.' },
      { title: '카드 발급 · 결제 승인', detail: '필수 약관에 동의하고 Face ID로 카드 발급을 승인하세요. 이어지는 결제 확인 화면에서도 동의와 Face ID 승인을 완료하세요.' },
      { title: '절약 금액을 적금으로', message: '응, 넣어줘', detail: '절약 금액 적금 제안에 답하고, 이어지는 이체 화면에서 모의 입금을 완료한 뒤 적금 잔액을 확인하세요.' },
    ],
  },
  {
    id: 'cancel',
    label: '시연 2',
    title: '구독을 해지해야 할 때',
    description: '채팅으로 해지할 구독을 찾고, 해지로 절약한 금액을 적금으로 연결해 보세요.',
    steps: [
      { title: '구독 해지 요청', message: '구독을 해지하고 싶어', detail: '채팅창에 입력해 해지할 구독을 찾아보세요.' },
      { title: '해지 대상 확인', detail: '조회된 구독 목록에서 해지할 서비스를 확인하세요.' },
      { title: '넷플릭스 선택', message: '넷플릭스 해지', detail: '채팅창에 서비스 이름과 해지 요청을 입력하면 해지 확인 화면이 표시돼요.' },
      { title: '해지 승인', detail: '해지 내용을 확인하고 ‘Face ID로 해지하기’를 눌러 승인하세요. 구독 해지 완료 안내를 확인하세요.' },
      { title: '절약 금액을 적금으로', message: '응, 넣어줘', detail: '절약 금액 적금 제안에 답하고, 이어지는 이체 화면에서 모의 입금을 완료한 뒤 적금 잔액을 확인하세요.' },
    ],
  },
];

export default function DemoGuidePanel() {
  const [activeIndex, setActiveIndex] = useState(0);
  const id = useId();

  const navigateTabs = (event) => {
    const nextIndex = {
      ArrowRight: (activeIndex + 1) % GUIDES.length,
      ArrowLeft: (activeIndex + GUIDES.length - 1) % GUIDES.length,
      Home: 0,
      End: GUIDES.length - 1,
    }[event.key];
    if (nextIndex === undefined) return;
    event.preventDefault();
    setActiveIndex(nextIndex);
    event.currentTarget.parentElement.children[nextIndex].focus();
  };

  return (
    <aside className="demo-guide" aria-labelledby={`${id}-title`}>
      <header className="demo-guide__header">
        <span className="demo-guide__badge">DEMO</span>
        <h1 id={`${id}-title`}>시연 가이드</h1>
        <p>휴대폰 화면에서 순서대로 따라 해보세요.</p>
      </header>

      <div className="demo-guide__tabs" role="tablist" aria-label="시연 선택">
        {GUIDES.map((item, index) => (
          <button key={item.id} type="button" role="tab" id={`${id}-tab-${item.id}`}
            aria-selected={index === activeIndex} aria-controls={`${id}-panel-${item.id}`}
            tabIndex={index === activeIndex ? 0 : -1}
            onClick={() => setActiveIndex(index)} onKeyDown={navigateTabs}>
            {item.label}<span>{index === 0 ? '새 구독' : '구독 해지'}</span>
          </button>
        ))}
      </div>

      {GUIDES.map((item, index) => (
        <section key={item.id} className="demo-guide__content" role="tabpanel"
          id={`${id}-panel-${item.id}`} aria-labelledby={`${id}-tab-${item.id}`}
          hidden={index !== activeIndex} tabIndex={0}>
          <h2>{item.title}</h2>
          <p className="demo-guide__description">{item.description}</p>
          <ol className="demo-guide__steps">
            {item.steps.map((step, stepIndex) => (
              <li key={step.title}>
                <span className="demo-guide__number" aria-hidden="true">{stepIndex + 1}</span>
                <div>
                  <h3>{step.title}</h3>
                  {step.message && <p className="demo-guide__phrase"><span>채팅 입력</span><q>{step.message}</q></p>}
                  {step.selection && <p className="demo-guide__phrase"><span>선택</span><strong>{step.selection}</strong></p>}
                  <p className="demo-guide__detail">{step.detail}</p>
                </div>
              </li>
            ))}
          </ol>
        </section>
      ))}

      <footer className="demo-guide__notice">
        <strong>시연 전 확인해 주세요</strong>
        <p>실제 결제·카드 발급·해지·입금은 이루어지지 않아요. Face ID는 시연용 인증 연출이에요.</p>
        <p>채팅 입력 문구는 안내된 표현을 그대로 사용해 주세요.</p>
      </footer>
    </aside>
  );
}
