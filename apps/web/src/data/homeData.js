// 화면에 들어가는 텍스트/데이터는 전부 여기서 관리합니다.
// 브랜드명, 계좌번호, 배너 문구 등은 이 파일만 바꾸면 화면 전체에 반영됩니다.

export const BRAND = {
  name: '우리',
  group: '우리금융그룹',
};

export const PROMO_BANNERS = [
  { id: 1, line1: '우리금융 크리스마스 페스타 OPEN!', line2: '전기차·태블릿 세트 골드바까지', icon: 'gift', bg: '#ffe4e8', color: '#f2667a' },
  { id: 2, line1: '첫 이체하면 커피 쿠폰 증정', line2: '이번 달 말까지 선착순 이벤트', icon: 'coffee', bg: '#fff1d6', color: '#e59a1d' },
  { id: 3, line1: '해외송금 수수료 0원', line2: '연말까지 모든 고객 대상', icon: 'globe', bg: '#dff0ff', color: '#2d82ef' },
  { id: 4, line1: '청년 적금 최고 연 6.0%', line2: '지금 가입하고 혜택 받기', icon: 'piggy', bg: '#e4f7ec', color: '#1fa463' },
  { id: 5, line1: '금융사기 예방 캠페인', line2: '의심 문자는 바로 신고하세요', icon: 'shield', bg: '#ece8ff', color: '#6b5ce7' },
];

// 다른 은행 로고 자리(실제 은행 로고가 아닌 가상의 원형 마크)
export const OTHER_BANKS = [
  { id: 'a', label: 'A', bg: '#ffd400', color: '#222' },
  { id: 'b', label: 'B', bg: '#d9ecff', color: '#2d6fe0' },
  { id: 'c', label: 'C', bg: '#6b5d4e', color: '#ffcc33' },
  { id: 'd', label: 'D', bg: '#0b8a72', color: '#fff' },
];

export const MAIN_ACCOUNT = {
  name: 'WON 통장',
  bank: '우리',
  number: '1002-123-456789',  // 가상의 계좌번호
  logoSrc: '/woori-logo.png', // 우리은행 로고 (public/woori-logo.png)
  tags: ['한도제한'],
  balance: 1284500,
};

export const SERVICE_GROUPS = [
  {
    id: 'g1',
    bg: '#f6ecfa',
    items: [
      { id: 's1', emoji: '📱', title: '우리WON모바일', desc: '우리은행이 만든 알뜰한 통신' },
      { id: 's2', emoji: '💸', title: '퇴직연금 한눈에', desc: '수수료없는 퇴직연금 알아보기' },
      { id: 's3', emoji: '👛', title: '우리WON지갑', desc: '국민비서 등 유용한 생활 서비스' },
    ],
  },
  {
    id: 'g2',
    bg: '#e8effc',
    items: [
      { id: 's4', emoji: '💯', title: '365 생활혜택', desc: '매일 받는 소소한 포인트' },
      { id: 's5', emoji: '🏷️', title: '금리우대 쿠폰', desc: '대출·예금 금리 쿠폰 모아보기' },
      { id: 's6', emoji: '🌐', title: '외환 서비스', desc: '환전부터 해외송금까지' },
    ],
  },
];

export const GROUP_SERVICES = [
  { id: 'cap', title: '캐피탈', color: '#e0832b', desc: ['내 차 구매', '할부·리스도'], emoji: '🚙' },
  { id: 'sec', title: '증권', color: '#2d6fe0', desc: ['새로운', '투자의 시작'], emoji: '📈' },
  { id: 'sav', title: '저축은행', color: '#c2307a', desc: ['나에게', '꼭 맞는 대출'], emoji: '🐷' },
  { id: 'card', title: '카드', color: '#1fa463', desc: ['생활 속', '카드 혜택'], emoji: '💳' },
];

export const NOTICE = '최근 금융권 개인정보 유출사고에 따른 보안 강화 안내';

export const TABS = [
  { id: 'product', label: '상품' },
  { id: 'asset', label: '자산·소비' },
  { id: 'home', label: '홈' },
  { id: 'stock', label: '주식' },
  { id: 'benefit', label: '혜택' },
];
