# 최종 통합 DB — 서버 마이그레이션으로 이동

최종 SQL의 정식 위치는 다음과 같다. 별도 `unified` 프로파일은 사용하지 않는다.

- [MySQL 스키마](../../../apps/api/src/main/resources/db/migration/V1__wonnie_schema.sql)
- [MySQL 데모 시드](../../../apps/api/src/main/resources/db/migration/V2__wonnie_seed_demo.sql)
- [H2 변환본](../../../apps/api/src/main/resources/db/migration-h2/)
- [이전 V1~V4 원본](../../../apps/api/src/main/resources/db/migration-archive/)

`docker` 프로파일의 Flyway가 새 V1 → V2를 최초 한 번 실행한다. `local` 프로파일은 같은 데이터·관계를 가진 H2 변환본을 사용한다. H2 파일은 `python3 scripts/generate-h2-migrations.py`로 생성하며 직접 수정하지 않는다. MySQL의 JSON 제약은 H2에서 그대로 재현하지 않으므로 실제 MySQL 검증도 수행한다.

## 최종 구조

통합 원본 26개에 필요한 런타임 테이블과 Google OIDC 연결 테이블을 더해 **33개 테이블·뷰 2개**다.

| 추가 테이블 | 역할 |
|---|---|
| `assistant_conversation` | 고객별 대화방 |
| `subscription_price_observation` | 가격 관측 이력·사건 중복 방지 |
| `outbox_event` | RabbitMQ 발행 대기·완료 상태 |
| `subscription_context` | 계약 확인·이용 횟수·무료체험·갱신·번들 등 부가 근거 |
| `finance_alternative` | 가상 대안 비용 비교 |
| `finance_cohort_benchmark` | 가상 또래 집계 비교 |

채팅 본문은 `chat_message` 하나에 저장한다. 대화방/고객 소유 FK, `source`, `source_key`, `read_at`을 보완해 기존 API의 대화 저장·알림 구분·중복 방지·읽음 처리를 유지한다.

고객·카드·거래·가맹점은 정규화된 통합 테이블을 사용한다. 기존 이름의 `finance_customer`, `finance_card`, `finance_transaction`, `merchant_directory`, `assistant_message` 테이블을 중복 생성하지 않는다. 금융 조회 모델은 JPA `@Subselect`/`@Immutable`로 이 테이블들을 조합하며 쓰기 엔티티로 사용하지 않는다.

## 반영한 코드 매핑

- `Subscription`: `display_name`, `next_charge_on`, 카드/가맹점/서비스 연결, `MONTHLY/YEARLY`, 새 상태값. 브랜드는 `service`에서 읽는다.
- `AssistantMessage`: `chat_message.role`, 고객·대화방·읽음·중복 키 매핑.
- `CalendarSignal`: `title` 속성을 `label`에 매핑. 캘린더 API 기능을 추가한 것은 아니다.
- 금융 조회: 고객·계좌·설정, 상품·혜택·카드·실적·포인트, 승인 내역, 가맹점, 구독 부가 근거를 통합 구조에서 조회한다.
- 할인율은 DB의 `30 = 30%`를 계산 시 100으로 나누고 같은 혜택 영역의 한도는 중복 합산하지 않는다.
- 현재 API 경로와 프론트 응답 필드는 유지한다. 활성 구독 카드에는 활성 금액을, 무료체험 포함 예상액은 별도 보고서 필드로 반환한다.
- Java 데모 데이터 로더를 제거했다. 초기 데이터는 Flyway V2가 삽입하며 서버 재시작 시 반복 삽입하지 않는다.

## 시드와 시간

- 고객: `cust-kimwoori`, 업무일: `demo_clock.today = 2026-10-27`.
- 활성 구독 7개 **61,300원**, 무료체험 추정 1개 포함 예상 **69,190원**.
- 거래 184건(승인 183·취소 1), 카드 2개, 계좌 2개.
- 주력 카드 실적은 9월 470,000원·10월 412,000원.
- 원본 동의 8개에 가격 인상·일반 금융 안내 선택을 추가해 10개 모두 최초 미동의.
- 계약 상태·실제 이용량·소득·상품 자격이 확인되지 않은 값은 NULL/미확인으로 유지한다. 무료체험 종료일도 추정임을 구분한다.
- 비교용 대안·또래 평균은 가상 예시이며 실제 상품·시설·이용자 통계가 아니다.
- `DATE`는 KST 업무일, DB 시각은 UTC. 제공 시드의 시간대 없는 업무 시각은 KST로 해석해 UTC로 변환하고 실적은 KST 월로 집계한다.
- JDBC는 UTC와 Java 날짜/시각 직접 매핑을 사용한다. 기존 프론트 계약을 유지하기 위해 채팅 응답 `createdAt`은 UTC 저장값을 KST로 변환해 반환한다.
- 캘린더·모의 이체·직원 로그 테이블이 존재해도 해당 기능의 API가 자동 구현되는 것은 아니다. 프론트의 동의/기기 권한 설정은 기존 시연 동작이며 새 동의 테이블에 저장하는 API는 별도 작업이다.

## 기존 DB와의 관계

새 V1/V2는 기존 V1~V4에 추가하는 ALTER 마이그레이션이 아니라 새 DB의 부트스트랩이다. Compose가 `mysql-data-v2`라는 새 볼륨을 사용하도록 변경해 이전 기본 볼륨 `infra_mysql-data`를 삭제하지 않고 분리한다. 기존 데이터의 자동 이관이나 기존 실행 DB 변경은 수행하지 않았다.

설정 변경 후 Compose를 실행하면 MySQL 컨테이너가 새 볼륨으로 재생성되므로 기존 API를 먼저 종료해야 한다. 이전 볼륨은 수동으로 삭제하지 않는다. 사용자 지정 `MYSQL_DATABASE`를 사용하는 경우 DB 이름과 계정 권한을 일치시켜야 한다.

## 검증

저장소 루트에서 실행한다. 임시 MySQL DB와 임시 API 프로세스만 사용하며 앱 DB나 볼륨을 변경하지 않는다.

```bash
python3 scripts/verify-final-schema.py
./apps/api/gradlew -p apps/api test bootJar
python3 scripts/verify-docker-migration.py
```

첫 스크립트는 SQL·시드·FK·중복 키·월 환산 등 28개 제약을 확인한다. 마지막 스크립트는 실제 `docker` 프로파일의 Flyway/JPA 기동, API 조회/채팅/가격 관측, 재기동 시 데이터·읽음 상태 보존과 시드 중복 방지를 확인한다. 검증용 DB·계정·프로세스는 종료 시 정리한다.
