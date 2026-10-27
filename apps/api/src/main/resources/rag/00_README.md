# 00. 워니 AI 연동 가이드 (README)

> 워니가 카드 데이터를 보고 매트릭스처럼 "먼저 물을 순간"을 고르고 답하도록 OpenAI API를 붙이는 방법.
> 대상: 백엔드(Spring AI) · AI 프롬프트 담당. DB는 `db/` 폴더의 스키마·시드를 쓴다.

---

## 1. 핵심 설계 — "사실은 코드가, 조합과 말은 AI가"

| 누가 | 하는 일 | 왜 |
| --- | --- | --- |
| **판단 규칙 (백엔드 코드)** | 승인 내역에서 구독을 찾고, 매트릭스의 **행(순간)** 신호를 만든다 → `detected_signal` | 금액·날짜·횟수는 틀리면 안 된다. 결정적으로 계산 |
| **ContextBuilder (백엔드 코드)** | 신호 + 고객·카드·캘린더 데이터를 **컨텍스트 JSON**으로 묶는다 | 동의 안 한 데이터는 여기서 빠진다 |
| **OpenAI (워니)** | 어느 순간을 주로 쓰고 어떤 **열(데이터)을 곁들일지** 고르고, 말투에 맞게 질문·버튼을 만든다 | 매트릭스 조합은 상황마다 달라서 자유롭게 판단하는 게 낫다 |
| **Validator (백엔드 코드)** | 출력의 스키마·숫자 출처·동의·금지 표현을 검사한다 | AI가 틀이 벗어나면 막는다 |

→ 02 문서(매트릭스)는 **지도**다. AI는 지도 안에서 자유롭게 조합하고, 지도 밖 조합도 조건을 지키면 쓸 수 있다 (`novel_combination`).

---

## 2. 파일 구성과 넣는 곳

| 파일 | 내용 | OpenAI 호출에서 넣는 곳 |
| --- | --- | --- |
| `01_system_instructions.md` | 역할, 생각 순서, 절대 규칙, 동의 게이트, 출력 규칙 | **instructions (항상)** |
| `02_moment_matrix_guide.md` | 행 13·열 18 정의, 69칸(활성 63 · 확장 6) 순간 카드, 아낀 돈 선택 블록 | **instructions (항상)** |
| `03_tone_persona_guide.md` | 워니 캐릭터, 톤 6종, 페르소나별 말투, 금지 표현, 아낀 돈 선택 말투 | **instructions (항상)** |
| `05_examples.md` | 검증된 입출력 예시 13개 | **instructions (항상)** |
| `04_knowledge_base.md` | 서비스별 해지 경로, 카드 혜택, 워니 절약 적금·꿀머니, 규정, 통계, FAQ | **RAG** — 필요한 섹션만 `knowledge`로 첨부 |
| `wonnie_response.schema.json` | 출력 JSON 스키마 (Structured Outputs, strict) | **response format** |
| `sample_context_proactive.json` · `sample_context_chat.json` | 시드 DB로 만든 실제 컨텍스트 예시 | 테스트 입력 |

01·02·03·05를 합치면 약 2.3만 토큰이다. 매번 넣어도 되는 크기이고, 같은 앞부분이 반복되어 프롬프트 캐시도 잘 맞는다. 04만 RAG로 분리한 이유는 서비스·상품이 늘어날수록 길어지기 때문이다.

---

## 3. 전체 흐름

```
[카드 승인 내역 card_transaction]
        │  판단 규칙 (매일 / 날짜 넘기기 때 실행)
        ▼
[구독 subscription] ──► [신호 detected_signal: R01~R13]
        │                 (해지 확인·요금제 하향 등 → 누적 절약 원장 saving_ledger)
        │
        ▼  ContextBuilder  (동의 안 한 데이터 제외)
[컨텍스트 JSON] + [04 지식 섹션]
        │
        ▼  OpenAI  instructions = 01+02+03+05 / response_format = schema
[워니 출력 JSON]
        │  Validator  (실패 → 오류를 알려 1회 재시도 → 그래도 실패면 템플릿 문장)
        ▼
[suggestion 저장] ──► 프론트: 워니 버튼 위 "!" (PROACTIVE) / 채팅 말풍선 (CHAT)
        │
        ▼  고객이 버튼 누름
[suggestion_response] ──► 다음 컨텍스트의 response_history (덜 묻고, 더 정확하게)
        │
        ▼  아낀 돈 선택 (적금 / 꿀머니 / 안 할래요)
[saving_ledger.choice] → [saving_plan] → [saving_transfer 회차별 적립] ──► R13 신호 (매달 쌓임·목표·건너뜀·재가입)
```

---

## 4. 입력 컨텍스트 (ContextBuilder 출력)

> 실제 예시: `sample_context_proactive.json` · 참고 구현: `../tools/build_context.py`

| 필드 | 내용 | DB 출처 | 비고 |
| --- | --- | --- | --- |
| `mode` | `PROACTIVE` / `CHAT` | — | |
| `today`, `today_weekday` | 오늘 날짜·요일 | `demo_clock` | 날짜 넘기기 대응 |
| `user_message`, `recent_chat` | 고객 메시지, 최근 대화 6개 | `chat_message` | CHAT만 |
| `customer` | `name`, `age_band`(20s…), `region`, `occupation`, `income_band`, `easy_mode`, `member_since` | `customer` | 생년월일·성별은 넘기지 않음 |
| `preferences` | `notify_window`, `muted_rows` | `customer_preference` | |
| `consents` | 동의 항목별 true/false | `consent` | |
| `signals[]` | `id`, `signal_type`, `matrix_row`, `priority`, `target_date`, `facts` | `detected_signal` (status NEW) | **숫자는 facts에서** |
| `overview` | `active_count`, `monthly_total`, `change_vs_last_month`, `new_subscriptions`, `fixed_cost_total`, `latest_statement`, `income_share_level` | `v_subscription_overview`, `card_statement` 등 | 비율·합계는 백엔드가 계산 |
| `subscriptions[]` | 구독별 금액·다음 결제일·카드·상태·구독 개월 수·`kb_key` | `subscription` + `service` | |
| `cards[]` | 카드별 상품·혜택·만료·한도·실적·최근 거절·혜택 사용액 | `card`, `card_product(_benefit)`, `card_monthly_stat`, `card_benefit_usage` | |
| `statements[]` | 최근 청구서 (청구액·구독 합계·확정 할인·결제일) | `card_statement` | |
| `fx` | 최신 환율 | `fx_rate` | 외화 구독이 있을 때만, 아니면 null |
| `points` | 꿀머니 잔액·소멸 예정 | `point_wallet` | |
| `accounts` | 은행 계좌 | `bank_account` | `AFFILIATE_BANK_SHARE` 동의 시에만, 아니면 null |
| `savings` | `cumulative_saved`, `deposit_status`(NOT_OPENED·ACTIVE), `deposit_balance`, `goal_amount`, `kkulmoney_received`, `plans[]`, `upcoming_transfers[]` | `v_saving_summary`, `saving_plan`, `bank_account` | W열·R13 |
| `spending_by_group` | 이번 달 가맹점 그룹별 결제 횟수 | `card_transaction` + `merchant` | 멤버십 이용도 |
| `calendar` | `busy`, `trips`, `recurring`, `write_granted`, `registered`(이미 등록한 일정) | `calendar_signal`, `calendar_write` | `CALENDAR_READ` 동의 시에만, 아니면 null |
| `family` | 가족카드 중복 | — | MVP는 null |
| `response_history[]` | 최근 응답 20개 | `suggestion_response` + `suggestion` | |
| `knowledge` | 이번에 붙인 04 섹션 원문 | `04_knowledge_base.md` | 아래 5절 |

---

## 5. RAG — 04 지식 문서 붙이는 법

04 문서는 섹션마다 `## [KEY]`로 시작하고 `---`로 나뉜다. 두 가지를 같이 쓴다.

**① KEY 직접 조회 (기본 · 결정적)**
- `subscriptions[].kb_key`, `cards[].kb_key` (DB의 `rag_doc_key`)와 같은 KEY의 섹션을 그대로 붙인다.
- 신호 유형별로 항상 붙일 섹션:

| 신호 | 붙일 KEY |
| --- | --- |
| `MERCHANT_UNKNOWN` (R02) | `KB-MERCHANT-DECODE`, `KB-HOWTO-APPLE`, `KB-HOWTO-GOOGLE` |
| `CANCEL_CHECK` (R08·R04), `PRICE_CHANGED`의 `PLAN_DOWNGRADE`, `SAVING_PROGRESS` (R13) | `PRODUCT-WONNIE-SAVING`, `POINT-KKULMONEY` |
| `PRICE_CHANGED` (R05), `TRIAL_ENDING` (R04) | `REG-DARKPATTERN` |
| `CARD_RECOMMEND` (R10) | 해당 `CARD-*` |
| `SUBSCRIPTION_OVERVIEW` (R01) | `STAT-SUBSCRIPTION` |
| CHAT 모드 | `FAQ-WONNIE`, `REG-DATA-USE` |

**② 벡터 검색 (CHAT 자유 질문 보완)**
- Spring AI `MarkdownDocumentReader`로 04를 `---` 기준으로 나눠 OpenAI 임베딩 → `SimpleVectorStore` (해커톤은 메모리 저장으로 충분).
- CHAT에서 `user_message`로 상위 3개 섹션을 검색해 ①에 더한다.
- 섹션마다 제목에 KEY와 핵심어가 있어 잘 걸린다.

> Spring AI 대신 OpenAI Responses API를 직접 쓴다면 04를 벡터 스토어에 올리고 `file_search` 도구를 켜도 된다. 단, Structured Outputs와 함께 쓸 수 있는지 사용하는 API 버전에서 확인한다.

---

## 6. OpenAI 호출 (Spring AI 기준 개요)

- `application-openai.yml`의 모델 설정을 그대로 쓴다.
- **system**: `01 + 02 + 03 + 05`를 순서대로 이어 붙인 문자열 (앱 시작 시 한 번 읽어 캐시)
- **user**: 컨텍스트 JSON 문자열. `knowledge`에 4절에서 고른 섹션 원문을 넣는다.
- **response format**: `wonnie_response.schema.json`의 `schema`를 JSON Schema(strict)로 지정 → `OpenAiChatOptions`의 response format 설정 (사용하는 Spring AI 버전의 API 이름 확인)
- 창의성 설정: 가능하면 temperature 0.3~0.5. 같은 순간이어도 문장이 조금씩 달라지는 정도가 적당하다.
- CHAT 모드는 최근 대화 6턴 정도를 user 앞에 함께 보낸다 (`chat_message`).

---

## 7. 검증과 대체 (Validator)

참고 구현: `../tools/validate_output.py`

1. JSON 스키마 검사
2. 메시지 속 숫자가 컨텍스트·knowledge에 있는지 (카드 마스킹 번호 제외)
3. 동의 게이트 (캘린더 읽기·쓰기, 마케팅, 선알림)
4. 금지 표현 ("마지막 시청", "해지해 드릴게요", "발급 가능해요" 등)
5. 버튼 `target_id`가 컨텍스트에 있는 id인지
6. `CALENDAR_PREVIEW` 버튼과 `calendar_event`가 짝인지
7. 아낀 돈 선택지는 세 개(적금·꿀머니·안 할래요)를 함께 줬는지, 무료체험(R04-W)에 선택지를 주지 않았는지
8. MVP 제외(확장) 칸을 쓰지 않았는지

실패하면 오류 목록을 붙여 1회 재시도하고, 그래도 실패하면 신호별 템플릿 문장(02 문서의 예시 문장)으로 대체한다. 결과는 `suggestion.validation_passed`에 남긴다.

> 숫자 검사는 "컨텍스트 어디에도 없는 숫자"만 잡는다. "2주"처럼 작은 숫자는 우연히 다른 값과 겹칠 수 있으니, 01 지침에서 계산·추정을 금지해 앞단에서 막는다.

---

## 8. 판단 규칙 (백엔드가 만드는 신호)

| signal_type (행) | 만드는 조건 | priority |
| --- | --- | --- |
| **구독 탐지** (신호 아님) | 같은 가맹점 2개월 연속, 결제일 ±3일, 금액 ±10% (외화는 원금액 동일) → `subscription` ACTIVE. 100원 이하 승인 후 취소 → TRIAL, 다음 결제 예정 = 승인일 + 1개월 | — |
| `SUBSCRIPTION_OVERVIEW` (R01) | 첫 동의 직후, 매월 1일 | 5 |
| `MERCHANT_UNKNOWN` (R02) | `service_id`와 `user_label`이 모두 NULL인 구독 | 4 |
| `CHARGE_UPCOMING` (R03) | ACTIVE 구독의 `next_charge_on = 오늘 + 1` | 2 |
| `TRIAL_ENDING` (R04) | TRIAL 구독의 `next_charge_on`이 1~3일 뒤 | 2 |
| `PRICE_CHANGED` (R05) | 직전 결제와 금액이 다름 → `change_type`: 오르면 `PRICE_INCREASE`, 내리면 `PLAN_DOWNGRADE`(+ 원장에 차액 기록), 외화 원금액은 같고 원화만 다르면 `FX_ONLY`. `is_foreign_currency` 필수 | 4 |
| `DUPLICATE_CATEGORY` (R06) | 같은 카테고리 ACTIVE 3개 이상, 월 1회 | 4 |
| `ANNUAL_RENEWAL` (R07) | YEARLY 구독 갱신 7일 전 | 2 |
| `CANCEL_CHECK` (R08·R04) | `CANCEL_REQUESTED` 구독의 예정일+1일까지 승인 없음 → 같은 가맹점 거절 기록이 있으면 `DECLINED_NOT_CANCELED`, 없으면 `CONFIRMED` + **`saving_ledger` 기록**(`ledger_id`를 facts에). 무료체험이면 `saving_type: TRIAL_BLOCKED`(선택지 없음) · 해지 후 승인 발생 → `CHARGE_AFTER_CANCEL` · 같은 달 같은 금액 2회 → `DOUBLE_CHARGE` | 3 (거절·해지 후 결제·이중 1) |
| `USAGE_CHECK` (R09) | 멤버십 구독인데 `usage_merchant_group` 이번 달 결제 1회 이하, 월말(25일) | 4 |
| `CARD_RECOMMEND` (R10) | 혜택 없는 카드로 내는 구독이 있고, 혜택 상품이 존재 (`MARKETING` 동의 시) | 5 |
| `SAVING_PROGRESS` (R13) | 적립일 실행 결과 → `TRANSFERRED` · 누적액 구간 돌파 → `STAGE_UP` · 목표일 전 목표 금액 도달 → `GOAL_REACHED` · 잔액 부족 → `DEFERRED`(알리지 않음) / `SKIPPED` · 고객이 재가입 확인 또는 해지 가맹점 결제 재개 → `RESUBSCRIBED` | 2·3·4 |
| `CARD_SCHEDULE` (R12) | 구독이 걸린 카드 만료 60일 이내 (만료 후 첫 결제 14일 이내면 priority 1, 아니면 4) · 꿀머니 소멸 30일 이내 · 연회비 청구 전월 · 카드대금 결제일 3일 전 | 1·4·5 |

- `dedupe_key`로 같은 순간이 두 번 만들어지지 않게 한다 (예: `R03:sub-watcha:2026-10-28`).
- 고객이 "아니"(CANCEL_GUIDE)를 누르고 해지 화면에서 해지를 마치면 `subscription.status = CANCEL_REQUESTED`, `cancel_requested_on` 기록.
- 아낀 돈 버튼: `SAVE_TO_DEPOSIT` → (적금 미개설이면 개설 + `AFFILIATE_BANK_SHARE` 동의) → `saving_plan`(DEPOSIT) · `RECEIVE_KKULMONEY` → `saving_plan`(KKULMONEY) · `DECLINE_SAVING` → `saving_ledger.choice = DECLINED`.
- 적립일에 같은 날 결제가 있거나 잔액이 부족하면 `saving_transfer.status = DEFERRED`(하루 미룸) → 다음 날도 부족하면 `SKIPPED`.

---

## 9. 데모 운영

| 버튼 | 백엔드 동작 |
| --- | --- |
| D-1 알림 보내기 | 판단 규칙 실행 → `PROACTIVE` 호출 → 결과가 있으면 "!" 표시 |
| 하루 넘기기 | `UPDATE demo_clock SET today = today + INTERVAL 1 DAY` → 해지한 구독은 승인 내역을 만들지 않음 → 판단 규칙 실행 (R08 `CONFIRMED` + `saving_ledger` 기록) → `PROACTIVE` 호출 → 아낀 돈 선택 → 고른 곳으로 첫 회 적립 |
| 처음부터 | DB를 V1 + V2로 다시 만든다 (`flyway clean` → `migrate`, 로컬 전용) |

---

## 10. 고칠 때

- **새 순간 추가**: 02에 순간 카드 추가 → (행이 새로 생기면) 판단 규칙에 signal_type 추가 → 필요하면 04에 지식 섹션 추가 → 05에 예시 1개 추가 → `tools/make_examples.py`로 검증
- **말투 수정**: 03만 고친다. 01의 절대 규칙과 충돌하지 않게
- **버튼 종류 추가**: 01의 action 표 + 스키마 `enum` + 프론트 처리 세 곳을 같이 고친다
