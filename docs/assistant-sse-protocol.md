# Assistant SSE 프로토콜 v1

상태: **1~9단계 완료**. 이벤트 계약, SSE 연결과 알림, 모델 스트리밍, 비동기 요청·상태·취소 API, 프론트 임시 답변 표시와 재연결 복구가 연결됐다. 기존 `POST /api/assistant/messages`는 호환성을 위해 유지한다.

구현 계약:

- Java: `apps/api/src/main/java/com/woori/wonnie/assistant/AssistantStreamEvent.java`
- 프론트 타입: `apps/web/src/services/assistantEvents.ts`

## 연결과 전송 방향

- 서버 → 브라우저: `GET /api/assistant/events`, `Content-Type: text/event-stream`.
- 질문 전송·읽음 처리는 POST를 사용한다. EventSource로 질문을 전송하지 않는다.
- `POST /api/assistant/requests`는 `{ requestId, message, topic }`을 받고 새 요청에 `202`와 요청·대화·예약 메시지 ID 및 상태를 반환한다. 같은 요청 ID·같은 입력의 재요청은 기존 상태를 `200`으로 반환하며, 같은 ID에 다른 입력을 보내면 `409`다. 현재 `POST /api/assistant/messages`는 그대로 유지한다.
- `GET /api/assistant/requests/{requestId}`는 작업 상태, 마지막 delta 순번, 진행 중 답변 스냅샷, 완료된 대화 쌍을 반환한다.
- `POST /api/assistant/requests/{requestId}/cancel`은 취소를 요청한다. 이미 대기 중인 작업은 바로 취소하고, 실행 중이면 `CANCELLING`을 반환한 뒤 작업이 멈춘 후 `reply.cancelled`를 보낸다. 커밋 단계에 먼저 진입했으면 취소하지 않고 현재 상태를 반환한다.
- 연결은 앱이 활성화된 동안 유지한다. 채팅창을 닫아도 알림을 받으며, 창을 닫는 동작 자체가 답변 생성 취소는 아니다.
- 고객은 서버 세션·인증 정보로 식별한다. 클라이언트가 보낸 고객 ID나 conversationId만으로 접근을 허용하지 않는다.

## 공통 이벤트 봉투

SSE 프레임의 `id`는 JSON의 `eventId`, `event`는 JSON의 `type`과 같아야 한다. `data`에는 이벤트 봉투 JSON 하나를 UTF-8로 직렬화한다. JSON 문자열 안의 줄바꿈은 이스케이프한다.

```text
id: 10000000-0000-4000-8000-000000000001
event: reply.delta
data: {"version":1,"eventId":"10000000-0000-4000-8000-000000000001","type":"reply.delta","occurredAt":"2026-10-27T08:00:00Z","conversationId":"conversation-id","requestId":"20000000-0000-4000-8000-000000000001","messageId":"assistant-message-id","data":{"sequence":1,"delta":"안녕하세요"}}

```

| 필드 | 규약 |
| --- | --- |
| `version` | 정수 `1`. 미지원 버전은 처리하지 않고 다시 동기화한다. |
| `eventId` | 서버가 생성한 UUID. 논리적 이벤트 하나의 ID이며 재전송할 때 유지한다. 정렬용 번호가 아니다. |
| `type` | 아래 8개 이벤트 중 하나. SSE `event`와 일치한다. |
| `occurredAt` | 서버 발생 시각. UTC RFC 3339 문자열이며 `Z`를 포함한다. 이벤트 정렬·중복 판정 키로 쓰지 않는다. |
| `conversationId` | 서버가 확인한 대화 ID. 모든 이벤트에 포함한다. |
| `requestId` | 답변 생성 요청의 UUID. 모든 `reply.*`에 필수. 요청과 무관한 알림·연결·읽음 이벤트는 null. |
| `messageId` | 생성 중인 답변의 예약 ID 또는 저장된 메시지 ID. 연결·읽음 이벤트는 null. |
| `data` | 이벤트 종류별 데이터. |

모든 공통 키를 항상 포함하고 해당 없는 값은 null로 보낸다. 저장된 메시지는 기존 REST `AssistantMessageResponse` 형식을 그대로 쓴다. 그 안의 `createdAt`은 기존 계약인 **Asia/Seoul 오프셋 없는 시각**이고, 봉투의 `occurredAt`과 형식이 다르다.

## 이벤트 종류

| `type` | `data` | 발생 시점 |
| --- | --- | --- |
| `connected` | `{ connectionId, unreadCount }` | SSE 연결 수립. requestId·messageId는 null. |
| `message.created` | `{ message }` | 메시지 DB 커밋 완료. 봉투 messageId는 message.id와 같다. 생성 요청에서 온 메시지는 requestId를 포함하고, 독립 알림은 null. |
| `unread-count.changed` | `{ unreadCount }` | 메시지 저장 또는 읽음 처리의 커밋 완료. 증감량 대신 최신 개수를 전달한다. |
| `reply.started` | `{ userMessageId }` | 요청 수락 후 생성 시작. 봉투 messageId는 예약된 AI 메시지 ID. |
| `reply.delta` | `{ sequence, delta }` | 표시할 답변 조각. sequence는 요청별 1부터 연속 증가. 공백·줄바꿈도 보존한다. |
| `reply.completed` | `{ exchange: { userMessage, assistantMessage }, unreadCount }` | 완성된 대화 쌍의 커밋 완료. assistantMessage.id는 예약 messageId와 같다. |
| `reply.failed` | `{ code, message, retryable }` | 생성 실패. 사용자용 오류만 보내며 키·프롬프트·상위 서비스 원문 오류는 보내지 않는다. |
| `reply.cancelled` | `{ reason }` | 생성 취소 확정. 사용자 취소의 reason은 `USER_REQUESTED`. |

Heartbeat는 `: heartbeat` SSE 주석으로 20초마다 보낸다. 메시지·읽음 수·답변 조각으로 처리하지 않는다. 연결은 MVC async timeout 없이 유지되며 브라우저가 연결을 닫거나 서버가 종료할 때 정리한다. 전송 오류·timeout·완료 콜백 모두 고객별 연결 목록에서 제거하고 heartbeat 작업을 취소한다.

### 연결 관리 구현

- 연결마다 UUID `connectionId`를 발급하고 즉시 `connected` 이벤트로 현재 unreadCount를 보낸다.
- 연결 저장소는 API 프로세스 안에서 고객 ID → 연결 ID → SSE emitter로 관리한다. 같은 고객의 여러 탭/창에 이벤트를 각각 전송할 수 있다.
- 연결이 끊기면 해당 연결만 제거한다. 같은 고객의 다른 탭 연결에는 영향을 주지 않는다.
- 현재 저장소는 프로세스 로컬이다. API를 여러 인스턴스로 늘리면 알림을 다른 인스턴스의 브라우저 연결까지 전달하도록 공유 pub/sub가 필요하다. Redis 세션 저장만으로 SSE 이벤트가 인스턴스 간 전달되지는 않는다.
- 로컬 개발 프로필은 기존처럼 인증을 끈 상태이며, Google OIDC 프로필은 보호된 API에 로그인 세션이 필요하다. 아직 API는 기존 구현과 동일하게 `cust-kimwoori` 시드 고객을 사용하므로 실제 로그인별 고객 매핑은 별도 작업이다.
- `Last-Event-ID`로 과거 이벤트를 재생하지 않는다. 재연결 이후 대화 상태 복구는 이후 프론트/요청 동기화 단계에서 처리한다.

### 커밋 이후 발행 구현

- 구독료 인상·금융 알림은 메시지 저장과 대화 갱신을 포함한 트랜잭션이 커밋된 뒤 `message.created` → `unread-count.changed` 순서로 발행한다.
- 사용자가 알림을 읽어 실제 unread 메시지가 갱신된 경우, 커밋 이후 최신 미읽음 수를 발행한다. 읽을 메시지가 없으면 중복 이벤트를 보내지 않는다.
- 구독 이벤트 처리처럼 바깥 트랜잭션이 있는 경우에도 그 바깥 트랜잭션의 커밋을 기다린다. 롤백되면 SSE 이벤트를 보내지 않는다.
- SSE 연결이 없거나 전달이 실패해도 이미 커밋된 DB 작업은 되돌리지 않는다. 재연결 시에는 대화 조회로 상태를 복구한다.
- 기존 동기식 `POST /api/assistant/messages`의 응답 이벤트는 requestId 상관관계가 아직 없으므로 이 단계에서 발행하지 않는다. AI 요청·답변 이벤트는 스트리밍 작업 단계에서 추가한다.

### 프론트 연결 구현

- 앱이 마운트된 동안 `EventSource('/api/assistant/events')`를 유지한다. 채팅창을 닫아도 SSE 연결은 계속 열려 있다.
- `connected`를 받을 때마다 기존 대화 API를 다시 조회해 재연결 중 누락된 메시지와 unread 수를 복구한다. 브라우저가 포그라운드로 돌아올 때도 REST로 한 번 동기화한다.
- `message.created`는 기존 `message.id` 기준 병합으로 중복을 막고, 채팅창이 열려 있으면 대화를 다시 불러와 알림을 읽음 처리한다. `unread-count.changed`는 서버가 보낸 최신 값을 반영한다.
- 동일 `eventId`는 프론트에서 무시하며 최근 1,024개 ID만 보관한다. 오래된 REST 응답이 새 SSE unread 이벤트를 덮어쓰지 않도록 응답 시작 이후 이벤트 수신 여부를 확인한다.
- EventSource가 지원되지 않거나 최초 연결에 실패하면 unread 수를 REST로 한 번 조회한다. 주기 폴링은 제거했다. 브라우저의 네이티브 EventSource가 연결을 재시도하며, 재연결 시 서버의 `connected` 이벤트가 전체 대화 동기화를 촉발한다.

### 알림 SSE 검증

- API 통합 테스트가 실제 HTTP SSE 연결에서 `connected` 봉투와 SSE `id` 일치를 확인한다.
- 바깥 DB 트랜잭션을 롤백했을 때 이벤트가 오지 않고, 구독 가격 관측을 커밋한 뒤 환율 알림·가격 인상 알림이 각각 메시지 이벤트와 unread 이벤트로 오는지 확인한다.
- 읽음 처리 커밋 뒤 `unread-count.changed: 0`이 오는지 확인한다.
- 프론트 통합 테스트가 이벤트 수신 후 배지·대화 내용을 갱신하고, 같은 eventId 재전달을 무시하며, 재연결 `connected` 이후 REST 대화 조회로 누락 메시지를 복구하는지 확인한다.

## 요청·메시지 ID와 중복 처리

1. 프론트는 새 생성 요청마다 requestId(UUID)를 하나 만든다. 네트워크 오류로 **같은 요청을 다시 전송할 때는 같은 ID**를 사용한다.
2. 서버는 `(고객, requestId)`로 소유권·중복을 확인한다. 같은 ID·같은 입력은 기존 실행 상태를 반환하고 LLM을 다시 실행하지 않는다. 같은 ID·다른 입력은 `409`로 거절한다. requestId는 인증 수단이 아니다.
3. 서버는 수락할 때 userMessageId와 assistantMessageId를 예약한다. 임시 표시부터 최종 DB 저장까지 같은 ID를 유지한다. 예약 자체는 저장 완료를 뜻하지 않는다.
4. 프론트는 같은 eventId를 재수신하면 무시한다. `message.created`·REST 조회·`reply.completed`가 같은 메시지를 포함해도 message.id 기준으로 하나만 유지한다.
5. 답변 조각은 `(requestId, sequence)` 기준으로 처리한다. 같은 순번을 두 번 붙이지 않고, 누락·역순이면 임의로 이어 붙이지 말고 생성 상태와 저장 대화를 다시 확인한다.
6. `reply.completed`의 전체 답변으로 임시 답변을 **교체**한다. 조각 뒤에 최종 답변을 추가로 붙이지 않는다.

요청 실행 레지스트리와 중복 실행 차단은 현재 API 프로세스 메모리 안에서 동작한다. 완료 시 예약된 메시지 ID를 사용해 사용자 메시지와 최종 답변을 한 DB 트랜잭션으로 저장하고, 커밋 뒤 `reply.completed`를 발행한다. 작업 상태·예약 ID·중복 방지 기록은 API 재시작 뒤 보존되지 않으며, 여러 API 인스턴스 간에도 공유되지 않는다. 여러 인스턴스·재시작에도 안전한 재시도에는 영속 요청 테이블과 공유 작업 조정이 필요하다.

## 생성 상태와 저장 경계

```text
ACCEPTED → RUNNING → COMPLETING → COMPLETED
    │          │           └────→ FAILED
    │          ├────────→ FAILED
    │          └→ CANCELLING → CANCELLED
    └───────────────────────→ CANCELLED
```

- `reply.started` 뒤에 조각은 0개 이상 올 수 있다. RAG/MCP 또는 모델 초기 호출 실패 시 조각 없이 실패할 수 있다.
- 요청 하나의 최종 상태는 완료·실패·취소 중 하나다. 취소와 완료가 겹치면 서버가 원자적으로 한 상태만 확정한다. `COMPLETING`에 진입하면 취소는 더 이상 승인하지 않으며, 완료 이후에도 취소 이벤트를 보내지 않는다.
- 실행 중 취소는 작업 스레드를 interrupt한다. 모델 응답기가 종료를 확인한 뒤에만 `CANCELLED`로 바꾸고 취소 이벤트를 보낸다. 취소가 확인되기 전에는 API가 `CANCELLING` 상태를 돌려준다.
- 생성 중 조각은 프론트 임시 상태다. 완료 시 사용자 메시지와 최종 답변을 기존 방식대로 함께 저장한다. 실패·취소된 조각을 정상 답변으로 저장하지 않는다.
- DB 커밋 성공 전에는 `message.created`나 `reply.completed`를 보내지 않는다. 전송 실패 때문에 이미 커밋된 답변을 롤백하거나 다시 생성하지 않는다.
- 실패·취소된 질문의 입력 복구·재시도 UI는 프론트에서 처리한다. 명시적으로 새로 시도하면 새 requestId를 만든다.
- 쓰기 작업 동안 SSE 연결을 DB 트랜잭션 안에서 계속 유지하지 않는다. Spring AI/MCP 스트리밍도 장시간 DB 트랜잭션 밖에서 실행한다.

## 재연결과 복구

- eventId는 UUID이며 eventId만으로 이벤트를 DB에서 재생하는 기능은 v1에 없다. `Last-Event-ID`만으로 누락 복구가 보장된다고 가정하지 않는다.
- 연결·재연결 때 기존 `GET /api/assistant/conversation`을 조회하고 메시지 ID로 병합한다. 진행 중인 요청은 상태 스냅샷으로 이어 붙이고, 초기 조회와 실시간 수신이 겹쳐도 중복을 제거한다. 대화 조회 뒤 서버의 최신 읽음 수도 반영한다.
- 진행 중인 요청은 `GET /api/assistant/requests/{requestId}`로 확인한다. 완료면 저장된 대화 쌍을 받고, 진행 중이면 `lastSequence`와 `partialAnswer`로 화면을 복구한다. 이벤트 자체의 과거 재생은 지원하지 않는다.
- 요청 상태 레지스트리는 프로세스 메모리라 서버 재시작 뒤에는 요청 조회가 `404`가 될 수 있다. 먼저 대화 REST 조회로 완료 메시지가 저장됐는지 확인하고, 접속이 끊겼다는 이유만으로 같은 질문을 자동 재생성하지 않는다.

## 다음 구현 순서

완료: 1단계 계약 정의, 2단계 SSE 연결 API·연결 관리, 3단계 커밋 이후 알림·읽음 이벤트 발행, 4단계 프론트 EventSource 연결, 5단계 알림 SSE 검증, 6단계 모델 텍스트 스트리밍, 7단계 비동기 요청·SSE 답변 이벤트, 8단계 상태 조회·취소 API, 9단계 프론트 임시 답변·재연결 복구와 통합 검증. 다중 인스턴스와 재시작을 넘는 요청 상태 보존은 영속 레지스트리 작업으로 남는다.
