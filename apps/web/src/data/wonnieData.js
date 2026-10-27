// AI 상담 챗봇 '워니(Wonnie)' 관련 문구 모음

export const WONNIE = {
  name: '워니',
  title: '워니 AI 상담',
  subtitle: '보통 바로 답변해요',
  fabLabel: 'AI 상담',
  fabCardSub: '궁금한 점이 있으면',   // 플로팅 버튼 카드 윗줄
  fabCardMain: '에게 물어보세요',     // 플로팅 버튼 카드 아랫줄 (앞에 '워니'가 파란색으로 붙음)
  fabBubble: '금융 고민, 워니에게 상담하세요!',
  greeting: '안녕하세요! 우리WON 금융 상담 챗봇 워니예요 🌱\n무엇을 도와드릴까요?',
  welcomeTitle: '금융 고민, 워니에게 물어보세요',
  welcomeDesc: '계좌·이체·카드·대출까지\n24시간 언제든 상담해 드려요',
  inputPlaceholder: '워니에게 상담할 내용을 입력하세요',
  typingDelay: 1000, // 답변 전 '생각 중' 연출 시간(ms)
};

export const QUICK_CHIPS = ['계좌 조회', '이체 방법', '카드 분실 신고', '대출 상담', '상품 추천'];

// 실제 답변 로직 없이 연출용 응답 문구
export function replyFor(text) {
  const short = text.length > 20 ? `${text.slice(0, 20)}…` : text;
  return `'${short}' 상담은 준비 중이에요. 곧 도와드릴게요!`;
}
