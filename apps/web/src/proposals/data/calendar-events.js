/* Sample schedules only. Google Calendar is not connected. Dates move with today in Korea. */
globalThis.WONNIE_CALENDAR_DEMO = (() => {
  function day(offset) {
    const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
    const value = type => parts.find(p => p.type === type).value;
    const date = new Date(`${value('year')}-${value('month')}-${value('day')}T00:00:00+09:00`);
    date.setTime(date.getTime() + offset * 86400000);
    return new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit' }).format(date);
  }
  return {
    source: 'demo', connected: false,
    events: [
      { id: 'exam', type: 'exam', title: '중간고사 기간', start: day(1), end: day(7), note: '내일부터 시험 기간' },
      { id: 'travel', type: 'travel', title: '제주도 여행', start: day(7), end: day(10), note: '다음 주 여행' },
      { id: 'deadline', type: 'deadline', title: '프로젝트 제출 마감', start: day(3), end: day(3), note: '3일 후 마감' }
    ],
    payments: [
      { service: 'Netflix', amount: 17000, date: day(1) },
      { service: 'YouTube Premium', amount: 14900, date: day(3) },
      { service: 'Spotify Premium', amount: 10900, date: day(7) }
    ]
  };
})();
