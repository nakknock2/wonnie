const SERVICES = [
  { name: '넷플릭스', pattern: /넷플릭스|netflix/i },
  { name: '스포티파이', pattern: /스포티파이|스포티|spotify/i },
  { name: 'ChatGPT', pattern: /chatgpt|챗지피티|챗gpt|지피티|gpt/i },
  { name: '유튜브 프리미엄', pattern: /유튜브|youtube/i },
  { name: '네이버플러스 멤버십', pattern: /네이버(?:플러스)?멤버[십쉽]|네이버플러스|naver(?:plus|membership)/i },
];

export const checkoutServiceFromMessage = (text) => {
  const compact = text.replace(/\s+/g, '');
  if (!/(구독|가입|신청|시작|결제|요금제)/.test(compact)) return null;
  if (/해지|취소|중단|환불|구독(?:목록|내역|현황|료)/.test(compact)) return null;
  const matches = SERVICES.filter((service) => service.pattern.test(compact));
  return matches.length === 1 ? matches[0].name : null;
};

export const isYearlyPlan = (plan) => ['YEARLY', '매년', '연간'].includes(plan?.billingCycle);
export const billingPeriod = (plan) => isYearlyPlan(plan) ? '년' : '월';
export const monthlySubscriptionAmount = (subscription) => Number(subscription.price || 0)
  / (isYearlyPlan(subscription) ? 12 : 1);
