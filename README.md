# wonnie

React 채팅 → Spring Boot API / Spring AI → OpenAI → 별도 MCP 서버 → 금융 조회 API / JPA 구조의 해커톤 MVP입니다. 캘린더를 제외한 카드·구독 상담과 사전 안내를 구현합니다.

금융기관 API는 아직 연결하지 않았습니다. 카드 상품, 고객, 가맹점, 계약 상태, 이용 로그, 또래 집계는 **가상 샘플 데이터**이며 실제 우리카드 상품·고객 통계가 아닙니다. 모든 금융 보고서에 `dataSource: DEMO`가 붙습니다.

## 실행

Java 17, Node.js와 pnpm이 필요합니다. API는 8080, MCP는 8081, 웹은 5173입니다. 이전 개발 프로세스가 해당 포트를 사용 중이면 먼저 종료하세요.

```bash
pnpm install
# 키 없이 화면·계산·대화 저장 확인: API(local H2) + 웹
pnpm dev
```

OpenAI와 **별도 MCP 서버를 실제로 연결**해 상담하려면 같은 터미널에 키를 설정합니다.

```bash
export OPENAI_API_KEY='본인 API 키'
# 선택 사항: 기본 모델은 gpt-5.6-terra
export OPENAI_MODEL='gpt-5.6-terra'
pnpm dev:openai
```

`dev:openai`는 웹, API(`local,openai`), MCP를 함께 실행합니다. OpenAI 키를 YAML·프론트 `VITE_*` 변수에 넣지 마세요. IntelliJ 실행 설정에만 저장한 키는 터미널에서 실행하는 pnpm에 자동 전달되지 않습니다. `.env` 파일도 API 스크립트에서 자동으로 읽지 않습니다.

현재 Spring AI Chat 모델은 Chat Completions의 도구 호출을 사용합니다. 기본 모델은 공식 Function Calling 예시의 호환 모델로 설정했고 reasoning은 `none`입니다. 모델을 바꿀 때 해당 API의 도구 호출 지원을 확인하세요. 실제 모델 호출에는 본인 계정의 접근 권한과 사용량이 적용됩니다.

```bash
# MCP 프로토콜만 확인: 웹 + 로컬 API + MCP, 상담은 규칙 기반
pnpm dev:mcp
# 개별 실행
pnpm api:dev:openai
pnpm mcp:dev
pnpm --dir apps/web dev
```

- 웹: http://localhost:5173
- API Swagger: http://localhost:8080/swagger-ui.html
- MCP Streamable HTTP: http://127.0.0.1:8081/mcp
- MCP Health: http://127.0.0.1:8081/actuator/health
- H2 Console: http://localhost:8080/h2-console (`jdbc:h2:mem:wonnie`, 사용자 `sa`, 비밀번호 없음)

MCP는 첫 상담 요청에서 연결합니다. 따라서 API와 MCP 시작 순서에 의존하지 않습니다. 모델·MCP 오류는 `503 ASSISTANT_UNAVAILABLE`로 표시하며 고정 응답으로 조용히 대체하지 않습니다. 기본 `pnpm dev`는 별도의 규칙 기반 모드입니다.

채팅 전송이 503으로 실패하면 API 콘솔의 `ASSISTANT_UNAVAILABLE method=POST path=/api/assistant/messages` 로그를 확인합니다. `stage=MCP_TOOLS`는 MCP 초기화·도구 목록 조회 단계이며, `stage=OPENAI_CHAT`는 OpenAI 요청·도구 실행·응답 처리 단계입니다. `cause=`에 원인 예외와 오류 내용이 표시되고 키·Bearer 토큰은 마스킹됩니다. 이 로그가 없는 이전 API 프로세스는 중지 후 다시 실행해야 합니다.

병합된 최종 프론트는 `apps/web/src/main.jsx` → `App.jsx`를 사용합니다. 기존 화면에서 일반 상담 전송·대화 기록·읽지 않은 알림·읽음 처리·동의 후 구독 조회를 API에 연결했습니다. 캐릭터 알림은 15초 간격과 창 포커스 시 갱신됩니다. CSS·이미지·화면 배치는 유지했습니다. 실행 중인 개발 서버는 병합 후 한 번 재시작하세요. 해지·이체·캘린더·알림 동의 설정의 시연 동작에는 아직 실행/설정 저장 API가 없어 기존 모의 동작을 유지합니다. 자세한 연결 경로는 [프론트 README](apps/web/README.md)에 있습니다.

## MySQL·RabbitMQ 실행

```bash
docker compose -f infra/docker-compose.yml up -d --wait
pnpm dev:infra:openai
# LLM 없이 MySQL·RabbitMQ를 확인하려면 pnpm dev:infra
```

H2는 API 종료 시 데이터가 사라집니다. MySQL 데이터는 Compose의 `mysql-data` 볼륨에 남습니다. `docker` 프로파일의 가격 관측 이벤트는 Outbox → RabbitMQ → 가격 분석 워커가 처리합니다. `local`은 같은 분석 로직을 동기 실행합니다.

## 상담 예시

- “이번 달 구독 총액과 고정비를 알려줘”
- “APPLE.COM/BILL은 어떤 앱 결제야?”
- “구독료가 오른 내역에서 환율 영향도 구분해줘”
- “통신 결합이나 가족 혜택과 겹치는 구독이 있어?”
- “내 카드로 결제하기 전에 잔액·한도·실적을 확인해줘”
- “무료체험 종료와 연간 갱신일 알려줘”
- “결제 거절이 구독 해지인지 확인해줘”
- “멤버십 이용 횟수로 1회권과 손익을 비교해줘”
- “보유 카드와 청년 카드의 구독 혜택을 비교해줘”
- “소득 대비 구독비와 샘플 또래 평균을 비교해줘”
- “카드대금 결제일, 유효기간, 포인트 소멸일 알려줘”

세부 기능·데이터 범위는 [기능 매트릭스](docs/feature-matrix.md), 서버 간 흐름은 [아키텍처](docs/architecture.md)에 있습니다.

## API로 직접 확인

```bash
curl http://localhost:8080/api/finance/subscriptions
curl http://localhost:8080/api/finance/card-recommendations
curl http://localhost:8080/api/finance/price-changes
# 안내 후보를 대화에 저장. 같은 사건은 중복 저장하지 않습니다.
curl -X POST http://localhost:8080/api/internal/finance-alerts/scan
curl http://localhost:8080/api/assistant/conversation
```

사용자별 알림 선호 시각(KST)에 무료체험·갱신·카드 대금·만료·포인트·연회비 안내 후보를 스캔합니다. 알림은 DB 채팅에 저장되고, 앱이 닫혀 있어도 다음 접속 때 조회됩니다. 기기 푸시 알림은 아직 연결하지 않았습니다.

실제 거래 수집기가 구독을 식별한 후 가격 관측 API로 넘기는 예시:

```bash
observed_at=$(date '+%Y-%m-%dT%H:%M:%S')
curl -X POST http://localhost:8080/api/internal/subscription-price-observations \
  -H 'Content-Type: application/json' \
  -d "{\"eventId\":\"transaction-unique-id\",\"subscriptionId\":\"sub-netflix\",\"amount\":18000,\"observedAt\":\"$observed_at\",\"currency\":\"KRW\",\"originalAmount\":18000,\"exchangeRate\":1}"
```

같은 `eventId`는 다시 처리하지 않습니다. 과거 시각의 이벤트는 현재 금액을 덮어쓰지 않습니다. 원통화 금액이 같고 원화만 증가하면 환율·수수료 변화 후보로 안내합니다. 원통화 자료가 없으면 결제액 증가를 알리되 구독료 인상으로 확정하지 않습니다.

## 데이터와 실행 범위

현재는 `customer-demo` 단일 데모 고객입니다. 실제 고객 인증 전달, 다른 카드사 거래 수집, 실시간 상품 약관·환율, 서비스 사용 이력, 실제 계약 상태 확인은 공급자 API에 연결해야 합니다. 읽기 MCP 도구만 제공하며 금융 거래·카드 발급·구독 해지는 실행하지 않습니다.

Flyway V4는 카드·고객·거래·구독 부가 정보·가맹점·대안·샘플 집계 테이블을 추가합니다. 기존 V1/V2는 수정하지 않았습니다. 기존 V3는 이전 지갑·인증 계획·중지 Saga 테이블과 그 기록을 제거하므로 기존 DB에 적용할 때 유의하세요. `app.finance.demo-data-enabled=false`로 샘플 데이터 초기화를 끌 수 있습니다.

## 검증

```bash
pnpm test
pnpm build
# 빌드된 API·MCP를 별도 포트에서 띄워 모의 모델과 왕복 검증
pnpm verify:mcp-flow
```

OpenAI 통합 테스트는 로컬 HTTP 응답 서버를 사용하고 실제 외부 모델·과금을 사용하지 않습니다. MCP 테스트는 실제 Streamable HTTP 클라이언트와 MCP 서버를 연결합니다.

참고: [OpenAI Function Calling](https://developers.openai.com/api/docs/guides/function-calling), [Spring AI 도구 호출](https://docs.spring.io/spring-ai/reference/api/tools.html).

## 배포

`main` 브랜치에 push되면 테스트와 빌드 후 Oracle 서버에 자동 배포됩니다. 프로덕션 Docker Compose, GitHub Actions 시크릿, DNS와 HTTPS 최초 설정은 [배포 문서](infra/DEPLOYMENT.md)를 참고하세요.
