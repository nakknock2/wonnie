# Wonnie 카드·구독 금융 비서

React 채팅 → Spring Boot API / JPA / Spring AI → 별도 MCP 서버 → 금융 조회 API 구조의 해커톤 MVP다. 카드·구독 데이터는 모의 데이터이며 실제 금융기관·서비스 계정과 연결하지 않는다. 금융 보고서의 내부 출처 메타데이터는 `dataSource: DATABASE`이며 상담 답변에 데모·샘플 머리말을 붙이지 않는다.

## 실행

Java 17+, Node.js, pnpm이 필요하다. 웹 5173, API 8080, MCP 8081을 사용한다. 기존 개발 서버를 종료한 뒤 실행한다.

```bash
pnpm install
# 새 스키마의 H2 데모: 웹 + API, LLM 없이 규칙 기반 상담
pnpm dev
```

OpenAI·MCP 상담은 같은 터미널에 `OPENAI_API_KEY`를 설정한다. 키를 프론트 변수나 YAML에 직접 저장하지 않는다. IntelliJ에만 설정한 키는 터미널에 자동 전달되지 않으며 `.env`도 자동으로 읽지 않는다. 모델은 `OPENAI_MODEL`로 지정할 수 있다.

```bash
export OPENAI_API_KEY='실제 발급받은 키'
pnpm dev:openai
```

## RAG 문서 연결

`openai` 프로파일은 `apps/api/src/main/resources/rag`의 최신 문서를 함께 사용한다.

- `01_system_instructions.md`, `02_moment_matrix_guide.md`, `03_tone_persona_guide.md`, `05_examples.md`: 시작 시 읽어 프롬프트 지침으로 재사용한다.
- `04_knowledge_base.md`: `## [KEY]` 단위로 나눠 Spring AI `SimpleVectorStore`에서 검색한다. 현재 24개 섹션이며, 질문과 가까운 상위 3개에 CHAT 공통 FAQ·데이터 이용 안내를 더한다. 출처와 KB 키도 모델에 전달한다.
- README, 샘플 컨텍스트 JSON, 응답 스키마는 실행 지침이나 고객 데이터로 자동 주입하지 않는다. 예시의 금액·이름은 현재 고객의 사실이 아니다. 현재 고객 정보와 계산값은 기존 MCP 도구에서 조회한다.

임베딩 모델은 `text-embedding-3-small`이며 기존 `OPENAI_API_KEY`를 사용한다. 첫 채팅에서 지식 문서를 한 번의 배치 요청으로 임베딩한 뒤 메모리에 보관하고, 이후에는 질문만 임베딩한다. 서버 재시작 후 첫 채팅은 다시 색인하므로 시간이 더 걸리고 OpenAI 임베딩 사용료도 발생한다. 별도 벡터 DB·MySQL 테이블은 추가하지 않는다. `SimpleVectorStore`는 해커톤용 메모리 구성이다.

기존 채팅 API·프론트 디자인은 유지한다. 문서의 JSON·버튼 출력 규칙에는 현재 서버의 텍스트 응답 계약을 우선 적용한다. 문서에 적힌 자동 적립·꿀머니 전환·캘린더·설정 변경은 실행하지 않으며, 선알림 생성 경로도 이번 RAG 연결 대상이 아니다.

```bash
# 필요할 때만 설정: 임베딩 모델 변경 / RAG 끄기
export OPENAI_EMBEDDING_MODEL='text-embedding-3-small'
export WONNIE_RAG_ENABLED=false
```

기본 경로는 JAR에도 포함되는 `classpath:rag/`다. MD를 수정한 뒤 API를 재시작하면 반영된다(배포 JAR은 재빌드). 외부 문서를 쓰려면 `WONNIE_RAG_LOCATION='file:/절대경로/rag/'`로 다섯 MD 파일이 있는 디렉터리를 지정한다. 원문 요청·고객 질문은 로그에 남기지 않고 `RAG documents loaded`, `RAG index ready`, `RAG context selected: keys=...`로 적용을 확인한다.

## MySQL·RabbitMQ와 새 스키마

`docker` 프로파일은 **`db/migration`의 새 V1/V2**를 실행한다. 기존 V1~V4는 `migration-archive`에 보관했다. 별도 `unified` 프로파일은 없다.

Compose는 새 `mysql-data-v2` 볼륨을 사용한다. 이전 기본 볼륨 `infra_mysql-data`는 삭제하지 않았고 자동 이관하지 않는다. 처음 적용할 때 MySQL 컨테이너가 새 볼륨으로 재생성되므로 API를 먼저 종료한다. 기존 볼륨이나 Flyway 이력을 지우거나 `repair`로 우회하지 않는다.

```bash
docker compose -f infra/docker-compose.yml up -d --wait
pnpm dev:infra:openai
# LLM 없이 MySQL API와 웹 확인
pnpm dev:infra
```

새 DB가 준비되면 API 시작 시 `V1__wonnie_schema.sql` → `V2__wonnie_seed_demo.sql`을 한 번 적용한다. 재시작 시 시드를 다시 삽입하지 않는다. 사용자 지정 MySQL DB를 사용하는 경우 새 빈 DB와 계정 권한을 준비해야 한다. 기존 V1~V4가 적용된 DB를 새 V1/V2에 직접 연결하면 Flyway가 검증 실패로 중단한다.

기존 Java 데모 데이터 로더는 제거했다. JPA는 `ddl-auto: validate`로 새 엔티티와 실제 구조를 검증하며, 테이블을 임의 생성·수정하지 않는다. [최종 스키마·매핑 설명](infra/db/final/README.md)에 데이터·시간·계산 계약이 있다.

## 데이터와 프론트

- 고객 `cust-kimwoori`, 업무일 `demo_clock.today = 2026-10-27`.
- 활성 구독 7개·월 61,300원, 무료체험 추정 1개 포함 예상 69,190원.
- 거래 184건, 카드 2개, 계좌 2개. 정확한 소득·실제 서비스 이용량 등 미연동 값은 미확인으로 안내한다.
- 최종 진입 화면은 `apps/web/src/main.jsx` → `App.jsx`다. CSS·이미지·배치는 유지한다.
- 일반 상담·대화 기록·읽음·미확인 알림·구독 조회는 API와 연결한다. 기존 프론트의 해지·이체·캘린더·알림 동의/기기 권한 설정은 시연 동작이다.
- H2는 API 종료 시 초기화된다. MySQL은 새 볼륨에 대화와 읽음 상태가 남는다.

## 확인할 주소와 명령

- 웹: http://localhost:5173
- API 문서: http://localhost:8080/swagger-ui.html
- MCP: http://127.0.0.1:8081/mcp
- MCP 상태: http://127.0.0.1:8081/actuator/health
- H2 Console(local): http://localhost:8080/h2-console (`jdbc:h2:mem:wonnie`, `sa`, 비밀번호 없음)

```bash
pnpm api:dev:infra:openai
pnpm mcp:dev
pnpm --dir apps/web dev
```

채팅 실패 시 API 콘솔의 `ASSISTANT_UNAVAILABLE` 로그를 확인한다. `stage=MCP_TOOLS`는 초기화·도구 조회, `stage=RAG_RETRIEVAL`은 문서 임베딩·검색, `stage=OPENAI_CHAT`는 모델 요청·도구 실행·응답 처리 단계이며 `cause=`에 원인이 나온다. 모델/MCP/RAG 오류는 503으로 반환하며 고정 답변으로 바꾸지 않는다.

## 주요 API와 검증

`GET /api/dashboard`, `GET /api/finance/{topic}`, `GET /api/assistant/conversation`, `POST /api/assistant/messages`, `POST /api/assistant/conversation/read`, `GET /api/assistant/unread-count`를 제공한다. 거래 수집기는 `POST /api/internal/subscription-price-observations`로 가격 관측을 전달한다. 이 입력의 `observedAt`은 기존 API처럼 KST의 오프셋 없는 업무 시각이며 큐·DB에는 UTC로 변환한다. 같은 사건은 재처리하지 않는다.

Docker에서는 업무 변경과 Outbox를 함께 기록하고 RabbitMQ 워커가 처리한다. Local은 같은 로직을 동기로 실행한다. Local의 수동 안내 스캔 경로 `POST /api/internal/finance-alerts/scan`은 테스트용이며 Docker에는 노출하지 않는다.

```bash
./apps/api/gradlew -p apps/api test bootJar
./apps/mcp/gradlew -p apps/mcp test bootJar
pnpm --dir apps/web test
pnpm --dir apps/web build
python3 scripts/verify-final-schema.py
python3 scripts/verify-docker-migration.py
pnpm verify:mcp-flow
```

MySQL 검증 스크립트는 기존 컨테이너 안에 새 임시 DB/계정만 만들고 정리한다. MCP 흐름 검증은 로컬 모델 HTTP 스텁을 사용하며 실제 OpenAI를 호출하지 않는다. H2용 SQL은 `python3 scripts/generate-h2-migrations.py`로 MySQL 마이그레이션에서 생성한다. [기능 범위](docs/feature-matrix.md)도 참고할 수 있다.

OpenAI 통합 테스트는 로컬 HTTP 응답 서버를 사용하고 실제 외부 모델·과금을 사용하지 않습니다. MCP 테스트는 실제 Streamable HTTP 클라이언트와 MCP 서버를 연결합니다.

참고: [OpenAI Function Calling](https://developers.openai.com/api/docs/guides/function-calling), [Spring AI 도구 호출](https://docs.spring.io/spring-ai/reference/api/tools.html).

## 배포

`main` 브랜치에 push되면 테스트와 빌드 후 Oracle 서버에 자동 배포됩니다. 프로덕션 Docker Compose, GitHub Actions 시크릿, DNS와 HTTPS 최초 설정은 [배포 문서](infra/DEPLOYMENT.md)를 참고하세요.
