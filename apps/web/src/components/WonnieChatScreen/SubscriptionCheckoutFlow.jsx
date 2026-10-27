import { useEffect, useMemo, useState } from 'react';
import dailyCard from '../../assets/card-woori-daily.png';
import routineCard from '../../assets/card-woori-routine.png';
import jeongseokCard from '../../assets/card-woori-jeongseok2.png';

const CARD_IMAGES = {
  'woori-daily': dailyCard,
  'woori-routine': routineCard,
  'woori-jeongseok2': jeongseokCard,
};

const won = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`;

function CardPlate({ card }) {
  const image = CARD_IMAGES[card.imageKey];
  if (image) return <img className="sc-card-plate" src={image} alt={`${card.name} 카드 이미지`} />;
  return (
    <div className="sc-card-plate sc-card-plate--fallback"
      style={{ background: `linear-gradient(145deg, ${card.gradientStart}, ${card.gradientEnd})` }}>
      <strong>WOORI CARD</strong><span>{card.maskedNumber || 'WOORI'}</span>
    </div>
  );
}

export function PlanPicker({ options, onSelect, disabled }) {
  return (
    <div className="sc-box">
      <div className="sc-service"><i>N</i><div><small>NETFLIX</small><strong>어떤 요금제로 시작할까요?</strong></div></div>
      <div className="sc-plans">
        {options.plans.map((plan) => (
          <button type="button" key={plan.code} disabled={disabled} onClick={() => onSelect(plan)}>
            <span><b>{plan.name}</b><small>{plan.description}</small></span>
            <span><strong>{won(plan.price)}</strong><small>/월</small></span>
            {plan.recommended && <em>추천</em>}
          </button>
        ))}
      </div>
      <p className="sc-caption">모의 가입 화면이에요. 실제 넷플릭스 계정이나 결제망과 연결되지 않아요.</p>
    </div>
  );
}

function CardTile({ card, onClick, actionLabel, rank }) {
  const discount = Number(card.potentialDiscount || card.estimatedDiscount || 0);
  return (
    <button type="button" className="sc-card-tile" onClick={() => onClick(card)}>
      <span className="sc-card-rank">{rank === 0 ? 'BEST' : `${rank + 1}`}</span>
      <CardPlate card={card} />
      <span className="sc-card-copy">
        <small>{card.issuer}{card.owned && card.maskedNumber ? ` · ${card.maskedNumber}` : ''}</small>
        <b>{card.name}</b>
        <strong>월 최대 {won(discount)} 할인</strong>
        <small>{Number(card.discountRate)}% · 전월실적 {won(card.minimumSpend)} 이상</small>
      </span>
      <span className="sc-card-action">{actionLabel}</span>
    </button>
  );
}

export function CardRecommendations({ options, onOwned, onCandidate }) {
  const plan = options.selectedPlan;
  return (
    <div className="sc-box sc-box--cards">
      <div className="sc-choice-head"><span><i>N</i><b>{plan.name}</b></span><strong>{won(plan.price)}<small>/월</small></strong></div>

      <section className="sc-card-section">
        <div className="sc-section-title"><div><strong>내 카드로 결제</strong><small>예상 할인 순</small></div><span>{options.ownedCards.length}장</span></div>
        {options.ownedCards.length ? (
          <div className="sc-carousel" aria-label="보유 카드 추천">
            {options.ownedCards.map((card, index) => <CardTile key={card.id} card={card} rank={index} onClick={onOwned} actionLabel="이 카드로 결제" />)}
          </div>
        ) : <div className="sc-empty">아직 보유한 카드가 없어요. 아래 추천 카드부터 발급해 주세요.</div>}
      </section>

      <section className="sc-card-section">
        <div className="sc-section-title"><div><strong>할인 좋은 우리카드</strong><small>발급 후 결제 가능</small></div><span>{options.recommendedCards.length}장</span></div>
        <div className="sc-carousel" aria-label="미보유 우리카드 추천">
          {options.recommendedCards.map((card, index) => <CardTile key={card.id} card={card} rank={index} onClick={onCandidate} actionLabel="바로 발급하기" />)}
        </div>
      </section>
      <p className="sc-caption">예상 혜택은 저장된 상품 조건과 전월실적을 반영한 모의 계산이며 실제 청구할인과 다를 수 있어요.</p>
    </div>
  );
}

function ConsentChecks({ checked, onAll }) {
  return (
    <div className="sc-consents">
      <button type="button" className={checked ? 'is-checked' : ''} onClick={() => onAll(!checked)}>
        <i>{checked ? '✓' : ''}</i><span><b>필수 약관 모두 동의</b><small>아래 필수 항목을 한 번에 확인해요</small></span>
      </button>
      {['개인정보 수집 및 이용', '신용정보 조회 및 카드 발급 심사', '상품 설명서 및 이용약관 확인'].map((label) => (
        <div key={label}><i>{checked ? '✓' : ''}</i><span>[필수] {label}</span><em>›</em></div>
      ))}
    </div>
  );
}

export function CardIssueConsentSheet({ card, onClose, onIssue, onAuthenticate }) {
  const [agreed, setAgreed] = useState(false);
  const [status, setStatus] = useState('idle');
  const [error, setError] = useState('');
  useEffect(() => { setAgreed(false); setStatus('idle'); setError(''); }, [card?.id]);
  if (!card) return null;
  const issue = async () => {
    if (status !== 'idle') return;
    setError('');
    try {
      setStatus('authenticating');
      const authenticated = await onAuthenticate?.();
      if (authenticated === false) { setStatus('idle'); return; }
      setStatus('loading');
      const issuedSelection = await onIssue(card);
      setStatus('done');
      setTimeout(() => onClose(issuedSelection), 700);
    }
    catch (e) { setStatus('idle'); setError(e.message); }
  };
  return (
    <div className="sc-sheet" role="dialog" aria-modal="true" aria-labelledby="card-issue-title">
      <button type="button" className="sc-sheet-dim" aria-label="닫기" onClick={status === 'idle' ? onClose : undefined} />
      <div className="sc-sheet-panel">
        <div className="sc-sheet-grab" />
        {status === 'done' ? <div className="sc-sheet-done"><i>✓</i><h2>카드가 바로 발급됐어요</h2><p>이제 보유 카드에서 선택해 결제할 수 있어요.</p></div> : status === 'loading' ? <div className="sc-sheet-done"><span className="sc-loader" /><h2>카드를 발급하고 있어요</h2><p>모의 발급 심사를 진행 중이에요.</p></div> : <>
          <header><button type="button" disabled={status !== 'idle'} onClick={onClose}>×</button><small>우리카드 바로발급</small><h2 id="card-issue-title">약관 동의만 하면<br />바로 사용할 수 있어요</h2></header>
          <div className="sc-issue-card"><CardPlate card={card} /><div><b>{card.name}</b><small>연회비 {won(card.annualFee)}</small><strong>넷플릭스 최대 {won(card.potentialDiscount)} 할인</strong></div></div>
          <ConsentChecks checked={agreed} onAll={setAgreed} />
          {error && <p className="sc-sheet-error">{error}</p>}
          <button type="button" className="sc-primary" disabled={!agreed || status !== 'idle'} onClick={issue}>모두 동의하고 카드 발급</button>
          <p className="sc-legal">실제 신용조회 없이 즉시 발급되는 시연용 카드입니다.</p>
        </>}
      </div>
    </div>
  );
}

export function PaymentConsentSheet({ selection, onClose, onPay, onAuthenticate }) {
  const [agreed, setAgreed] = useState(false);
  const [status, setStatus] = useState('idle');
  const [error, setError] = useState('');
  useEffect(() => { setAgreed(false); setStatus('idle'); setError(''); }, [selection?.card?.id, selection?.plan?.code]);
  if (!selection) return null;
  const { card, plan } = selection;
  const pay = async () => {
    if (status !== 'idle') return;
    setError('');
    try {
      setStatus('authenticating');
      const authenticated = await onAuthenticate?.();
      if (authenticated === false) { setStatus('idle'); return; }
      setStatus('loading');
      await onPay(selection); setStatus('done'); setTimeout(onClose, 1050);
    }
    catch (e) { setStatus('idle'); setError(e.message); }
  };
  return (
    <div className="sc-sheet" role="dialog" aria-modal="true" aria-labelledby="subscription-pay-title">
      <button type="button" className="sc-sheet-dim" aria-label="닫기" onClick={status === 'idle' ? onClose : undefined} />
      <div className="sc-sheet-panel">
        <div className="sc-sheet-grab" />
        {status === 'done' ? <div className="sc-sheet-done"><i>✓</i><h2>결제가 완료됐어요</h2><p>넷플릭스 {plan.name} 구독을 시작했어요.</p></div> : status === 'loading' ? <div className="sc-sheet-done"><span className="sc-loader" /><h2>결제를 승인하고 있어요</h2><p>잠시만 기다려 주세요.</p></div> : <>
          <header><button type="button" disabled={status !== 'idle'} onClick={onClose}>×</button><small>정기결제 신청</small><h2 id="subscription-pay-title">결제 내용을<br />마지막으로 확인해 주세요</h2></header>
          <div className="sc-pay-summary">
            <div className="sc-service"><i>N</i><div><small>넷플릭스</small><strong>{plan.name}</strong></div><b>{won(plan.price)}</b></div>
            <dl><div><dt>결제 카드</dt><dd>{card.name} {card.maskedNumber}</dd></div><div><dt>결제 주기</dt><dd>매월 오늘 자동 결제</dd></div><div><dt>예상 할인</dt><dd className="is-blue">최대 {won(card.potentialDiscount)}</dd></div></dl>
          </div>
          <ConsentChecks checked={agreed} onAll={setAgreed} />
          {error && <p className="sc-sheet-error">{error}</p>}
          <button type="button" className="sc-primary" disabled={!agreed || status !== 'idle'} onClick={pay}>{won(plan.price)} 결제 승인</button>
          <p className="sc-legal">모의 정기결제이며 실제 금액은 청구되지 않습니다.</p>
        </>}
      </div>
    </div>
  );
}

export function SubscriptionCompleteCard({ result }) {
  const total = useMemo(() => result.subscriptions.reduce((sum, item) => sum + Number(item.price || 0), 0), [result]);
  return (
    <div className="sc-box sc-complete">
      <div className="sc-complete-head"><i>✓</i><div><small>정기결제 승인 완료</small><strong>넷플릭스 {result.plan.name}</strong></div><b>{won(result.approvedAmount)}</b></div>
      <div className="sc-receipt"><span>결제 카드</span><b>{result.cardName}</b><span>다음 결제일</span><b>{result.subscriptions.find((item) => item.id === result.subscriptionId)?.nextPaymentDate}</b></div>
      <div className="sc-current"><div><strong>현재 구독 중</strong><small>{result.subscriptions.length}개 · 월 {won(total)}</small></div>{result.subscriptions.map((item) => <div className="sc-current-row" key={item.id}><i style={{ background: item.color }}>{item.letter}</i><span><b>{item.name}</b><small>{item.plan || '월간 요금제'} · {item.cardName}</small></span><strong>{won(item.price)}</strong></div>)}</div>
      <p className="sc-caption">결제와 카드 발급은 서비스 체험을 위한 모의 처리입니다.</p>
    </div>
  );
}
