globalThis.WonnieDemoEngine = (() => {
  const scenarios = globalThis.WONNIE_SCENARIOS;
  const normalize = text => String(text).toLowerCase().replace(/[\s\p{P}\p{S}]+/gu, '');
  const choose = items => items[Math.floor(Math.random() * items.length)];
  const get = id => scenarios.find(s => s.id === id) || scenarios[0];
  let bag = [];
  const muted = new Set();
  function next(currentId) {
    bag = bag.filter(id => !muted.has(id) && scenarios.some(s => s.id === id));
    if (!bag.length) {
      bag = scenarios.filter(s => !muted.has(s.id)).map(s => s.id);
      for (let i = bag.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [bag[i], bag[j]] = [bag[j], bag[i]]; }
    }
    if (bag.length > 1 && bag[bag.length - 1] === currentId) [bag[0], bag[bag.length - 1]] = [bag[bag.length - 1], bag[0]];
    return bag.length ? get(bag.pop()) : null;
  }
  function classify(value) {
    if (/(유지|괜찮|나중|안할|싫|관심없|그대로|필요없|계속이용|계속볼|계속사용|여행에서도이용)/.test(value)) return 'keep';
    if (/(왜|이유|근거|정말|확실|판단)/.test(value)) return 'reason';
    if (/(제안해|추천해|맞춰)/.test(value)) return 'compare';
    if (/(조건|실적|한도|연회비|같은요금제|같은상품|받을수|이용권)/.test(value) && !/(비교|계산|얼마)/.test(value)) return 'conditions';
    if (/(방법|어떻게|바꾸|변경|신청|발급|해지|취소|환불|연결|적용|진행|문의|쉬고|잠시쉬)/.test(value)) return 'action';
    if (/(얼마|절약|비교|계산|차이|이득)/.test(value)) return 'compare';
    if (/(내역|보여|몇건|뭐가|어떤|날짜|언제|일정)/.test(value)) return 'details';
    if (/^(네|예|응|좋아|좋아요|해줘|볼게요|확인해줘|알려줘)$/.test(value)) return 'compare';
    return null;
  }
  function resolve(question, currentId) {
    const value = normalize(question);
    const current = get(currentId);
    if (/(다른제안|다른시나리오|다음제안|새로운제안)/.test(value)) return { kind: 'next' };
    const direct = current.prompts.find(p => normalize(p.label) === value);
    const matched = direct ? current : scenarios.find(s => s.keywords.some(k => value.includes(normalize(k))));
    const scenario = matched || current;
    const intent = direct?.intent || classify(value);
    if (intent === 'keep') {
      muted.add(scenario.id);
      return { kind: 'reply', scenarioId: scenario.id, intent, text: choose(['알겠어요. 현재 이용 방식을 유지하시는 것으로 이해했어요. 이번 시연 동안 이 제안은 무작위 추천에서 제외할게요.', '좋아요. 추가 안내를 멈출게요. 같은 제안을 이번 시연에서 자동으로 반복하지 않아요. 원하시면 시나리오 선택에서 다시 볼 수 있어요.']), rows: [], followUps: ['다른 제안 보여줘'] };
    }
    if (!intent && !matched) return { kind: 'reply', scenarioId: scenario.id, intent: 'fallback', text: `지금은 “${scenario.name}” 제안을 보고 있어요. 절약액 비교, 판단 근거, 조건이나 관리 방법을 물어보시면 준비된 예시 답변으로 안내할게요. 다른 시나리오 이름을 입력해도 전환할 수 있어요.`, rows: [], followUps: scenario.prompts.map(p => p.label) };
    const selectedIntent = intent || 'compare';
    const answer = scenario.replies[selectedIntent] || scenario.replies.details;
    return { kind: 'reply', scenarioId: scenario.id, intent: selectedIntent, text: choose(answer.text), rows: answer.rows, followUps: answer.followUps };
  }
  return { scenarios, get, choose, next, resolve, normalize, isMuted: id => muted.has(id) };
})();
