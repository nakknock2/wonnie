/* Schedule-based proposals share the same frontend and response engine as savings proposals. */
(() => {
  const calendar = globalThis.WONNIE_CALENDAR_DEMO;
  const event = id => calendar.events.find(e => e.id === id);
  const date = value => value.replaceAll('-', '.');
  const won = value => `${value.toLocaleString('ko-KR')}원`;
  const rows = calendar.payments.map(p => [`${p.service} · ${date(p.date)}`, won(p.amount)]);
  const payment = service => calendar.payments.find(p => p.service === service);
  const reply = (text, rows = [], followUps = ['내 일정 보여줘', '다른 제안 보여줘']) => ({ text: Array.isArray(text) ? text : [text], rows, followUps });
  const context = id => ({ title: event(id).title, date: event(id).start === event(id).end ? date(event(id).start) : `${date(event(id).start)} ~ ${date(event(id).end)}`, note: event(id).note });
  globalThis.WONNIE_SCENARIOS.push(
    {
      id: 'calendar-exam', source: 'calendar-demo', calendarEventId: 'exam', calendarContext: context('exam'),
      name: '시험 일정 응원과 결제 안내', category: '캘린더 · 시험 일정', service: '중간고사 · Netflix',
      title: '내일부터 시험 기간이네요\nWONNIE가 응원할게요!',
      messages: ['준비한 만큼 잘 풀리길 응원할게요! 아, 내일은 Netflix 정기구독 결제도 예정돼 있어요. 이번 달에도 계속 이용하실 생각인가요?', '내일부터 시험 기간이네요. 늘 화이팅이에요! 내일 예정된 Netflix 결제도 함께 알려드려요. 이번 달 이용 계획은 어떠세요?'],
      evidence: ['캘린더 예시 · 시험', '정기결제일 · 내일'],
      metric: { label: '내일 결제 예정', value: won(payment('Netflix').amount), detail: `Netflix · ${date(payment('Netflix').date)} · 예시 결제` },
      prompts: [{ label: '계속 이용할게요', intent: 'keep' }, { label: '잠시 쉬고 싶어요', intent: 'action' }, { label: '결제 내용 보여줘', intent: 'details' }],
      keywords: ['시험', '중간고사', '기말고사', '공부일정'],
      replies: {
        compare: reply(['시험 일정과 결제일이 겹쳐 이용 계획을 여쭤봤어요. 시험 기간이라는 이유만으로 Netflix를 안 쓴다고 판단하지는 않아요. 계속 이용하셔도 괜찮고, 잠시 쉬고 싶으시면 관리 방법을 안내할게요.', '내일 시작되는 예시 시험 일정과 Netflix 결제 예정일을 함께 확인했어요. 사용 빈도를 알 수 없으므로 이번 달 이용 계획을 고객님께 여쭤보는 제안이에요.'], [['캘린더 예시', `${context('exam').title} · ${context('exam').date}`], ['Netflix 예시 결제', `${date(payment('Netflix').date)} · ${won(payment('Netflix').amount)}`]], ['계속 이용할게요', '잠시 쉬고 싶어요', '결제 내용 보여줘']),
        details: reply(`Netflix 예시 정기결제는 내일 ${date(payment('Netflix').date)}, ${won(payment('Netflix').amount)} 예정이에요. 같은 날 예시 시험 기간이 시작돼 함께 알려드렸어요.`, [['서비스', 'Netflix'], ['결제 예정일', date(payment('Netflix').date)], ['예정 금액', won(payment('Netflix').amount)]], ['계속 이용할게요', '잠시 쉬고 싶어요']),
        reason: reply('시연용 캘린더의 중간고사 시작일과 Netflix 정기결제일이 모두 내일이라 제안했어요. 실제 구글캘린더나 서비스 사용 빈도는 조회하지 않았어요. 고객님의 이용 계획을 묻는 것이며 유지·해지를 자동 판단하지 않아요.', [], ['결제 내용 보여줘', '계속 이용할게요']),
        conditions: reply('예시 캘린더의 일정 제목에 “중간고사”가 있고 시작일이 내일이에요. 내일 예정된 구독 결제가 함께 있어 두 정보를 묶었어요. 이 시나리오는 카드 실적이나 사용 빈도 없이 일정과 결제일만 사용해요.'),
        action: reply('잠시 쉬고 싶으시군요. Netflix 계정의 구독 관리에서 다음 결제일과 해지 후 이용 가능 기간을 먼저 확인해 주세요. 원하시면 확인할 항목을 정리해 드릴게요. 여기서는 결제 잠금이나 해지를 대신 실행하지 않아요.', [], ['결제 내용 보여줘', '지금 방식 유지할게요'])
      }
    },
    {
      id: 'calendar-travel', source: 'calendar-demo', calendarEventId: 'travel', calendarContext: context('travel'),
      name: '여행 전 구독 결제 챙기기', category: '캘린더 · 여행 일정', service: '제주도 여행 · Spotify',
      title: '다음 주 여행이 있네요\n출발 전에 결제도 챙길까요?',
      messages: ['제주도 여행, 즐거운 시간 보내세요! 여행 시작일에 Spotify 결제가 예정돼 있어요. 여행에서도 이용하실 계획인지 미리 확인해 볼까요?', '다음 주 여행 준비 중이시네요. 출발 무렵 예정된 Spotify 구독 결제를 확인했어요. 여행 중에도 계속 이용하실 계획인가요?'],
      evidence: ['캘린더 예시 · 여행', '출발일 정기결제'],
      metric: { label: '여행 시작일 결제 예정', value: won(payment('Spotify Premium').amount), detail: `Spotify Premium · ${date(payment('Spotify Premium').date)} · 예시 결제` },
      prompts: [{ label: '여행에서도 이용할게요', intent: 'keep' }, { label: '여행 중 결제 보여줘', intent: 'details' }, { label: '관리 방법 알려줘', intent: 'action' }],
      keywords: ['여행', '제주도', '출장', '휴가'],
      replies: {
        compare: reply(['여행이라고 구독이 필요 없다고 판단하지는 않아요. 예시 여행 시작일에 Spotify 결제가 잡혀 있어 출발 전에 이용 계획을 확인하실 수 있도록 알려드렸어요.', '여행 일정과 같은 날 예정된 Spotify 결제를 함께 보여드려요. 여행 중 음악을 이용할 계획이 있으시면 그대로 유지하셔도 괜찮아요.'], [['여행 예시 일정', context('travel').date], ['Spotify 결제 예정', `${date(payment('Spotify Premium').date)} · ${won(payment('Spotify Premium').amount)}`]], ['여행에서도 이용할게요', '여행 중 결제 보여줘', '관리 방법 알려줘']),
        details: reply('예시 여행 기간 중에는 Spotify Premium 1건이 결제될 예정이에요. 여행에 필요한 서비스인지 여부는 고객님이 선택하실 수 있어요.', [['여행', `${context('travel').title} · ${context('travel').date}`], ['Spotify Premium', `${date(payment('Spotify Premium').date)} · ${won(payment('Spotify Premium').amount)}`]], ['여행에서도 이용할게요', '관리 방법 알려줘']),
        reason: reply('제주도 여행 예시 일정의 시작일과 Spotify 결제일이 같아서 미리 챙겨드리는 제안이에요. 여행 중 서비스 사용량이나 인터넷 이용 환경은 알 수 없어 이용하지 않을 것이라고 추정하지 않았어요.'),
        conditions: reply('예시 여행 일정의 시작일부터 종료일까지 들어오는 정기결제만 확인하는 방식이에요. 실제 연동은 추후 연결하고, 지금은 data/calendar-events.js의 예시 일정과 결제일을 사용해요.'),
        action: reply('출발 전에 Spotify 계정에서 현재 요금제와 다음 결제일을 확인해 주세요. 이용을 쉬려면 서비스가 안내하는 구독 관리 조건과 종료 시점을 먼저 확인하시면 돼요. 이 화면에서는 요금제나 구독을 변경하지 않아요.', [], ['여행 중 결제 보여줘', '여행에서도 이용할게요'])
      }
    },
    {
      id: 'calendar-deadline', source: 'calendar-demo', calendarEventId: 'deadline', calendarContext: context('deadline'),
      name: '마감 일정에 맞춘 결제 요약', category: '캘린더 · 마감 일정', service: '프로젝트 마감 · 결제 요약',
      title: '프로젝트 마감이 다가와요\n결제 확인은 한 번에 할까요?',
      messages: ['3일 후 프로젝트 마감이 있네요. 잘 마무리하시길 응원할게요! 바쁜 동안 놓치지 않도록 앞으로 7일의 구독 결제를 한 번에 정리해 드릴까요?', '프로젝트 마무리로 바쁘시겠어요. 결제를 하나씩 묻기보다 이번 주 예정 내역을 한 번에 확인하실 수 있게 정리해 드릴게요.'],
      evidence: ['캘린더 예시 · 마감', '앞으로 7일의 결제일'],
      metric: { label: '7일 이내 예시 구독 결제', value: '42,800원', detail: 'Netflix · YouTube Premium · Spotify Premium · 3건' },
      prompts: [{ label: '이번 주 결제 정리해줘', intent: 'details' }, { label: '왜 이때 알려줘?', intent: 'reason' }, { label: '지금은 괜찮아요', intent: 'keep' }],
      keywords: ['마감', '프로젝트', '제출', '바쁜일정'],
      replies: {
        compare: reply(['예시 마감 일정이 다가와 앞으로 7일의 구독 결제 3건, 총 42,800원을 한 번에 정리했어요. 마감 때문에 구독을 해지하라고 권하는 제안은 아니에요.', '바쁜 일정에 결제를 여러 번 확인하는 수고를 줄이려는 제안이에요. 이번 예시에서는 7일 이내 구독 3건을 묶어서 보여드려요.'], rows, ['이번 주 결제 정리해줘', '내 일정 보여줘']),
        details: reply('앞으로 7일의 예시 정기결제는 아래 3건, 총 42,800원이에요. 마감 일정과 모든 결제일이 같아야 하는 것은 아니고, 다가오는 일정을 참고해 한 번에 챙겨드리는 방식이에요.', [...rows, ['7일 이내 합계', '42,800원']], ['내 일정 보여줘', '지금은 괜찮아요']),
        reason: reply('예시 캘린더에 3일 후 프로젝트 제출 마감이 있어요. 일정이 바쁜 시점에 여러 결제를 한 번에 확인할 수 있도록 제안했어요. 마감 일정으로 서비스 사용 여부를 추정하지 않았고 별도의 알림 예약도 만들지 않았어요.'),
        conditions: reply('시연용 캘린더에 다가오는 마감 일정이 있으면 앞으로 7일의 결제 내역을 묶어 안내해요. 이용 빈도나 실제 업무량을 분석하지 않아요. 결제 금액은 모두 예시예요.'),
        action: reply('지금 결제 목록을 한 번에 확인할 수 있어요. 이후 알림을 다시 받는 기능은 아직 예약하지 않았으므로, 이 시연에서는 현재 목록 확인과 대화만 제공해요.', rows, ['지금은 괜찮아요', '다른 제안 보여줘'])
      }
    }
  );
})();
