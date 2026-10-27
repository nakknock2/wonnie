# 모바일 뱅킹 홈 화면 목업 (React)

스마트폰 틀 안에 은행 앱 홈 화면을 보여주는 React 앱입니다. 병합된 화면 디자인을 유지하며 워니 상담·대화 기록·알림 읽음 처리·구독 조회를 기존 Spring Boot API에 연결했습니다. 홈의 계좌와 상품 버튼, 시연 패널의 해지·이체·캘린더는 기존 모의 동작입니다.

## 실행
```bash
# 저장소 루트에서
pnpm install
pnpm dev
# OpenAI + 별도 MCP 상담: 같은 터미널에서 OPENAI_API_KEY 설정 후
pnpm dev:openai
```
브라우저에서 http://localhost:5173 접속

웹만 실행하려면 `pnpm --dir apps/web dev`를 사용합니다. API(8080)가 별도로 실행되어 있어야 합니다. Vite의 `/api` 프록시가 `http://localhost:8080`으로 전달하며, 운영 환경에서도 동일 출처 `/api`를 API 서버로 전달해야 합니다. 프론트에 OpenAI 키를 넣지 않습니다.

## 백엔드 연결

| 화면 동작 | 요청 |
|---|---|
| 일반 질문·캘린더 외 제안의 후속 질문 | `POST /api/assistant/messages` (`message`, 선택한 시나리오의 `topic`) |
| 채팅 열기·열려 있는 동안 갱신 | `GET /api/assistant/conversation` |
| 대화 조회 성공 후 읽음 처리 | `POST /api/assistant/conversation/read` |
| 홈 캐릭터의 기존 알림 표시 | `GET /api/assistant/unread-count` (15초 간격·창 포커스 시) |
| 동의 후 기존 구독 카드 채우기 | `GET /api/finance/subscriptions`, `GET /api/dashboard` (브랜드 표시 보조) |

`src/services/wonnieApi.js`가 요청과 화면 데이터 변환을, `src/hooks/useWonnieBackend.js`가 대화·전송·폴링을 담당합니다. 구독 개수·금액·결제일은 서버 응답을 표시합니다. API 오류는 기존 토스트로 표시하며, 전송 실패한 질문은 입력창에 복원합니다. 상담 API는 성공한 대화 쌍을 저장합니다. H2 로컬 모드는 API 재시작 시 초기화되며 MySQL 모드는 DB에 남습니다. '처음으로'는 현재 화면만 초기화하며 저장된 대화를 삭제하지 않습니다.

캘린더 예시, 첫 제안 카드와 시연 패널의 알림, 해지·이체, 알림 동의/기기 권한 설정은 클라이언트 예시입니다. 서버가 저장한 알림은 대화 조회로 별도로 표시합니다. 모의 동의 설정이 서버 알림 발송을 제어하지는 않습니다. 현재 서버 금융 데이터도 `DEMO`이며 실제 금융기관 조회가 아닙니다.

검증: 저장소 루트에서 `pnpm --dir apps/web test`, `pnpm --dir apps/web build`.

## 구조
```
src/
├─ App.jsx                 폰 틀 + 홈 화면 조립
├─ data/homeData.js        브랜드명·계좌·배너 문구 등 모든 데이터
├─ components/
│  ├─ PhoneFrame.jsx       스마트폰 외형 + 상태바
│  ├─ HomeScreen.jsx       홈 화면 상태 관리(잔액숨김, 탭, 토스트 등)
│  ├─ Header.jsx           일반홈/쉬운 토글 + 상단 아이콘
│  ├─ PromoBanner.jsx      자동 슬라이드 이벤트 배너
│  ├─ HeroScene.jsx        가을 캠핑 일러스트 + 홈계좌설정/잔액숨김
│  ├─ AccountSection.jsx   다른 은행 안내 카드, 대표 계좌 카드
│  ├─ ServiceSection.jsx   적금 배너, 서비스 카드, 금융그룹 서비스, 공지
│  ├─ BottomNav.jsx        하단 탭
│  └─ Icons.jsx            SVG 아이콘
└─ styles/                 global / phone / home CSS
```

## 동작하는 인터랙션
- 일반홈 ↔ 쉬운 토글 (쉬운 모드는 글자가 커짐)
- 이벤트 배너 자동 넘김, 점 클릭/좌우 스와이프, X로 닫기
- 잔액숨김 스위치 ↔ 계좌 잔액 표시 (잔액 영역을 눌러도 전환)
- 계좌번호 클릭 시 복사, ⋮ 메뉴 팝오버
- 다른 은행 안내 카드 닫기
- 서비스 카드 가로 스크롤
- 하단 탭 전환 (홈 외 탭은 "준비 중" 화면)
- 나머지 버튼은 "준비 중" 토스트 표시

## 연동 시 수정 포인트
- `homeData.js`의 더미 데이터를 API 응답으로 교체
- `HomeScreen.jsx`의 `soon(...)` 호출 부분을 라우팅/화면 이동으로 교체

## 데모 시나리오: 김우리(28세) · 구독 절약
`preview.html` 을 열면 폰 오른쪽에 **데모 패널**(체크리스트 · 데모 날짜 · 진행 버튼)이 보여요.

| 순서 | 화면 | 하는 방법 |
|---|---|---|
| 1 | 앱 진입 · 동의 | 1초 뒤 올라오는 동의 시트 → "동의하고 내 구독 찾기" |
| 2 | 구독 현황 | 동의 후 API 조회 → **서버 응답의 구독 개수·월 환산 금액** (단독 `preview.html`은 기존 예시) |
| 2-1 | 알림 동의 | 조회가 끝나면 대화에서 '결제 하루 전 알림' · '구독료 인상·변경 알림'을 따로 선택(기본 꺼짐) → 기기 알림 권한(모의) → 설정 요약. "알림 설정"이라고 말하면 다시 변경 |
| 3 | 결제 D-1 알림 | (D-1 알림에 동의한 경우만) 패널 **🔔 D-1 알림 보내기** → 워니 버튼 자세 변화 + 빨간 배지 → 누르기<br>"내일 왓챠 7,900원이 결제돼요. 계속 볼 거예요?" |
| 4 | "아니" → 해지 | "아니" → 해지 페이지 연결 카드 → 모의 해지 화면에서 해지 |
| 5 | 날짜 넘기기 | 패널 **📅 하루 넘기기** |
| 6 | 결제 안 된 것 확인 | 워니 버튼 알림 → "결제가 들어오지 않았어요 ✅" + 오늘 결제내역 |
| 7 | 적금 제안 · 모의 이체 | "왓챠 7,900원 아꼈어요. 그만큼 적금에 넣을까요?" → "응, 넣어줘" → 이체 시트 |
| 8 | 누적 절약액 | 대화 속 누적 절약 카드, 홈 잔액 1,284,500 → 1,276,600원 |
| + | 구독료 인상 알림 | (인상 알림에 동의한 경우만) 패널 **📈 구독료 인상 알림** → 인상 안내 + 요금제 비교 (예시 시나리오) |

- **처음부터**: 패널 ↺ 버튼
- **알림 원칙**: 동의한 종류만, 보낼 중요한 일(내일 결제·요금 인상)이 있는 날에만 13~14시 사이 1회. 매일 보내는 알림 없음 (`hooks/useSubscriptionNudge.js`)
- 해지 화면·이체는 모두 **모의**예요. 실제 해지는 해당 서비스 앱/웹으로 딥링크하는 게 맞아요.

### 주요 파일
- `demo/useDemo.js` 시나리오 상태 · `demo/DemoPanel.jsx` 시연 패널
- `data/subscriptions.js` 고객 · 구독 7개 · 계좌 데이터 (예시 금액)
- `components/Consent/` 동의 시트/폼
- `components/WonnieChatScreen/` 상담 화면(대화 시나리오), `CancelPage.jsx` 모의 해지, `TransferSheet.jsx` 모의 이체
- `components/WonnieChatScreen/NotifConsent.jsx` 알림 받기 동의 카드 · 기기 알림 권한(모의)
- `components/WonnieCast.jsx` 홈 장면 워니들 (왼쪽 측면 · 가운데 뒷모습 · 오른쪽 노래 · 창가) — 변형 이미지 `assets/wonnie-3d-*.png`, 생성 스크립트 `tools/faces3d.py`, `tools/armsdown.py`, `tools/views3d.py`

## 시나리오별 절약 제안 (팀 3.agent-proposals 통합)

상담 화면 안에서 김우리 데모와 함께 **9가지 제안 시나리오**를 고를 수 있어요.
팀원이 만든 데이터·응답 엔진 파일을 수정 없이 `src/proposals/`로 옮겨 그대로 쓰고, 화면만 상담 디자인에 맞췄어요.

| 들어가는 곳 | 동작 |
|---|---|
| 상담 화면 상단 **시나리오** 버튼 · 입력창 왼쪽 **+** · 시연 패널 **🧩 절약 제안 시나리오** | 시나리오 고르기 시트 (김우리 기본 데모 / 절약 제안 6종 / 캘린더 연계 3종) |
| 웰컴 카드 **오늘의 절약 제안 보여줘** | 아직 안 본 제안을 무작위로 (엔진 `next`) |
| **캘린더 예시 일정 3건** 띠 | 시험 · 여행 · 마감 예시 일정 → 일정 맞춤 제안 |

- 캘린더 외 제안의 후속 질문은 선택한 주제와 함께 상담 API로 보내요. 캘린더 예시는 기존 엔진 `resolve`로 처리해요. 첫 제안 카드와 시나리오 선택은 기존 예시예요.
- "지금 방식 유지할게요"처럼 답하면 그 제안은 이번 시연의 자동 추천에서 빠져요.
- D-1 알림 · 구독료 인상 알림 · 결제일 결과가 오면 김우리 데모 대화로 자동 전환돼요. "김우리", **처음으로**도 같은 동작이에요.
- 원본 설명: `docs/agent-proposals/README.md`, `docs/agent-proposals/SCENARIOS.md`

| 파일 | 내용 |
|---|---|
| `proposals/data/scenarios.js` | 절약 제안 6종 (원본 그대로) |
| `proposals/data/calendar-events.js` · `calendar-scenarios.js` | 예시 일정 3건 · 일정 연계 제안 3종 (원본 그대로) |
| `proposals/scenario-engine.js` | 응답 엔진 (원본 그대로) |
| `proposals/index.js` | 위 파일을 순서대로 불러와 `engine`, `calendar`로 내보내요 |
| `components/WonnieChatScreen/Proposals.jsx` | 제안 카드 · 답변 · 시나리오 고르기 시트 |
