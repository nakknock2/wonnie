-- Wonnie 최종 통합 스키마 / MySQL 8.4 / 새 DB 전용
-- 기반: 사용자 제공 Wonnie_통합_schema (1).sql (26개 테이블 + 뷰 2개)
-- 추가: 대화방, 가격 관측, Outbox, 구독 부가 근거, 대안, 또래 집계 (6개 테이블)
-- 총 32개 테이블. assistant_message/finance_customer/finance_card/finance_transaction/merchant_directory 중복 생성 없음.
-- 날짜 DATE는 KST 업무일자, 모든 시각 DATETIME(6)은 UTC. 앱은 입출력 시 명시적으로 변환한다.
-- docker 프로파일의 정식 Flyway bootstrap. 기존 V1~V4 DB와 이력을 재사용하지 않는다.
-- 현재 사용하는 JPA 엔티티/조회 모델은 이 구조에 매핑한다. 이전 SQL은 migration-archive에 보관한다.
-- DB 생성·삭제·기존 테이블 삭제·Flyway clean 명령 없음.

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET time_zone = '+00:00';
-- =====================================================================
-- Wonnie MVP schema (MySQL 8.x)
-- 기준: Wonnie_프로젝트_컨텍스트.md (2026-10-27 최종 기획)
--
-- 설계 원칙
--  1. 모든 데이터는 모의(mock) 데이터. 실제 우리카드·우리은행과 연동하지 않는다.
--  2. "카드 데이터로 물어볼 순간을 찾는다" → 승인 내역(card_transaction)이 출발점.
--  3. 판단 규칙(코드)이 만든 '순간'은 detected_signal 에, AI가 만든 질문은 suggestion 에 남긴다.
--  4. 금액·날짜는 DB 값만 AI에 넘긴다. AI가 만든 숫자는 저장 전에 검증한다.
--  5. 고객 성별은 저장하지 않는다. 캘린더 일정 원문은 저장하지 않는다.
--  6. 결제 잠금(가상카드·지갑·Saga)은 보류 → 이 스키마에 포함하지 않는다.
--
-- 테이블 그룹
--  [고객]      customer, customer_preference, consent
--  [카드]      card_product, card_product_benefit, card, card_monthly_stat, point_wallet
--  [은행·모의] bank_account, saving_transfer
--  [가맹점]    service, merchant
--  [결제]      card_transaction
--  [구독]      subscription
--  [AI]        detected_signal, suggestion, suggestion_response, chat_message
--  [캘린더]    calendar_connection, calendar_signal, calendar_write
--  [데모]      demo_clock
-- =====================================================================

-- ---------------------------------------------------------------------
-- [데모] 날짜 넘기기용 시계. 앱의 "오늘"은 항상 이 값을 쓴다.
-- ---------------------------------------------------------------------
CREATE TABLE demo_clock (
    id          TINYINT     PRIMARY KEY,               -- 항상 1
    today       DATE        NOT NULL,
    updated_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_demo_clock_single CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ---------------------------------------------------------------------
-- [고객] 카드 신청 때 받은 기본 정보. 성별 컬럼 없음(의도).
-- ---------------------------------------------------------------------
CREATE TABLE customer (
    id              VARCHAR(64)  PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL,
    birth_date      DATE         NOT NULL,              -- 나이·연령대 계산용 (AI에는 age, age_band만 전달)
    region_sido     VARCHAR(20)  NOT NULL,              -- 예: 서울특별시
    region_sigungu  VARCHAR(30)  NOT NULL,              -- 예: 마포구
    occupation      VARCHAR(30)  NOT NULL,              -- OFFICE_WORKER | STUDENT | SELF_EMPLOYED | FREELANCER | HOMEMAKER | RETIRED | OTHER
    income_band     VARCHAR(20)  NOT NULL,              -- UNDER_30M | 30M_40M | 40M_60M | 60M_80M | OVER_80M  (AI는 대화에서 숫자로 말하지 않음)
    monthly_income  DECIMAL(15,4) NULL,               -- 확인된 모의 월소득만. income_band에서 임의 환산 금지
    easy_mode       BOOLEAN      NOT NULL DEFAULT FALSE, -- 우리WON뱅킹 '쉬운 홈' 사용 여부 → 정중·자세한 말투
    member_since    DATE         NOT NULL,              -- 우리카드 첫 가입일
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 알림 시간·알림 끄기 등 개인 설정 (말투·알림 개인화 R11)
CREATE TABLE customer_preference (
    customer_id         VARCHAR(64) PRIMARY KEY,
    notify_start_hour   TINYINT     NOT NULL DEFAULT 13,   -- 선알림 가능 시작 시각
    notify_end_hour     TINYINT     NOT NULL DEFAULT 14,   -- 선알림 가능 종료 시각
    muted_rows          JSON        NULL,                  -- 끈 순간 행 목록. 예: ["R06","R10"]
    updated_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_pref_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT ck_pref_hours CHECK (notify_start_hour BETWEEN 0 AND 23 AND notify_end_hour BETWEEN 0 AND 23)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 동의 항목별 상태. consent_type 목록은 아래 주석 참고.
--   CARD_TX_READ          [필수] 카드 결제내역 조회
--   SUBSCRIPTION_ANALYSIS [필수] 구독 분석을 위한 정보 이용
--   AI_NOTICE             [필수 고지] AI(생성형) 사용 안내 확인
--   PUSH_D1               [선택] 결제 D-1 등 선알림 받기
--   CALENDAR_READ         [선택] 구글 캘린더 읽기
--   CALENDAR_WRITE        [선택] 구글 캘린더 쓰기 (등록은 매번 미리보기 확인 후)
--   MARKETING             [선택] 마케팅 활용 (카드 추천)
--   AFFILIATE_BANK_SHARE  [순간 동의] 우리은행 계좌·적금 연결 (적금 넣기를 누르는 순간 받음)
CREATE TABLE consent (
    id              VARCHAR(64)  PRIMARY KEY,
    customer_id     VARCHAR(64)  NOT NULL,
    consent_type    VARCHAR(40)  NOT NULL,
    required        BOOLEAN      NOT NULL,
    agreed          BOOLEAN      NOT NULL DEFAULT FALSE,
    agreed_at       DATETIME(6)    NULL,
    withdrawn_at    DATETIME(6)    NULL,
    CONSTRAINT fk_consent_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT uq_consent UNIQUE (customer_id, consent_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ---------------------------------------------------------------------
-- [가맹점] 서비스 카탈로그. rag_doc_key 로 RAG 지식 문서의 서비스 카드와 연결.
-- ---------------------------------------------------------------------
CREATE TABLE service (
    id                VARCHAR(64)   PRIMARY KEY,
    name              VARCHAR(60)   NOT NULL,
    category          VARCHAR(30)   NOT NULL,     -- OTT | MUSIC | CLOUD | PRODUCTIVITY | MEMBERSHIP | AI | TELECOM | FITNESS | OTHER
    billing_channel   VARCHAR(20)   NOT NULL,     -- WEB | APP_STORE | GOOGLE_PLAY | MIXED | AUTOPAY
    typical_price     DECIMAL(15,2) NULL,
    has_trial         BOOLEAN       NOT NULL DEFAULT FALSE,
    trial_days        INT           NULL,
    usage_merchant_group VARCHAR(40) NULL,        -- 멤버십 이용도 추정용 가맹점 그룹. 예: NAVER_SHOPPING
    rag_doc_key       VARCHAR(80)   NOT NULL,     -- 예: SVC-WATCHA
    brand_color       VARCHAR(20)   NULL,         -- 프론트 표시용
    initial_text      VARCHAR(4)    NULL          -- 프론트 표시용
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 승인 데이터에 찍히는 가맹점명 → 서비스 매핑 (merchant master)
CREATE TABLE merchant (
    id              VARCHAR(64)  PRIMARY KEY,
    raw_name        VARCHAR(100) NOT NULL,      -- 승인 내역 가맹점명 그대로. 예: APPLE.COM/BILL
    category        VARCHAR(40)  NOT NULL,      -- 업종: SUBSCRIPTION | CAFE | CONVENIENCE | DELIVERY | ONLINE_SHOPPING | GROCERY | RESTAURANT | TRANSPORT | BEAUTY | TELECOM | ETC
    service_id      VARCHAR(64)  NULL,          -- 알 수 있으면 매핑. APPLE.COM/BILL 처럼 모르면 NULL
    is_aggregator   BOOLEAN      NOT NULL DEFAULT FALSE, -- 애플·구글·간편결제처럼 안의 서비스가 안 보이는 가맹점
    merchant_group  VARCHAR(40)  NULL,          -- 업종별 소비 집계 그룹. 예: COUPANG, BAEMIN, NAVER_SHOPPING
    display_name    VARCHAR(100) NULL,
    candidate_services JSON      NULL,          -- 불명확한 결제명의 후보 배열, 확정 서비스 아님
    verification_hint VARCHAR(500) NULL,
    CONSTRAINT fk_merchant_service FOREIGN KEY (service_id) REFERENCES service(id),
    CONSTRAINT uq_merchant_raw UNIQUE (raw_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ---------------------------------------------------------------------
-- [카드] 상품·혜택 (공개 기사 기준 요약 · 실제 약관 확인 필요)
-- ---------------------------------------------------------------------
CREATE TABLE card_product (
    id                    VARCHAR(64)   PRIMARY KEY,
    name                  VARCHAR(60)   NOT NULL,
    card_type             VARCHAR(10)   NOT NULL,   -- CREDIT | CHECK
    annual_fee            DECIMAL(15,2) NOT NULL DEFAULT 0,
    summary               VARCHAR(300)  NOT NULL,
    rag_doc_key           VARCHAR(80)   NOT NULL,   -- 예: CARD-ROUTINE
    issuer_name           VARCHAR(60)   NULL,
    minimum_age           INT           NULL,
    maximum_age           INT           NULL,
    required_region_sido  VARCHAR(20)   NULL,         -- NULL이면 저장된 지역 제한 없음
    required_occupation   VARCHAR(30)   NULL,         -- NULL이면 저장된 직업 제한 없음
    CONSTRAINT ck_product_age CHECK (minimum_age IS NULL OR minimum_age >= 0),
    CONSTRAINT ck_product_age_range CHECK (maximum_age IS NULL OR minimum_age IS NULL OR maximum_age >= minimum_age)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE card_product_benefit (
    id                    VARCHAR(64)   PRIMARY KEY,
    card_product_id       VARCHAR(64)   NOT NULL,
    benefit_area          VARCHAR(40)   NOT NULL,   -- DIGITAL_SUBSCRIPTION | MEMBERSHIP | OTT | LIVING | OVERSEAS ...
    service_id            VARCHAR(64)   NULL,       -- 특정 서비스 대상이면 지정, 영역 전체면 NULL
    discount_rate         DECIMAL(5,2)  NOT NULL,   -- 30.00 = 30%
    monthly_cap           DECIMAL(15,2) NOT NULL,   -- 영역별 월 최대 할인액
    min_prev_month_spend  DECIMAL(15,2) NOT NULL,   -- 전월실적 조건
    note                  VARCHAR(200)  NULL,
    CONSTRAINT fk_benefit_product FOREIGN KEY (card_product_id) REFERENCES card_product(id),
    CONSTRAINT fk_benefit_service FOREIGN KEY (service_id) REFERENCES service(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ---------------------------------------------------------------------
-- [은행·모의] WON 통장, 워니 절약 적금 (모의 계좌)
-- ---------------------------------------------------------------------
CREATE TABLE bank_account (
    id              VARCHAR(64)   PRIMARY KEY,
    customer_id     VARCHAR(64)   NOT NULL,
    account_type    VARCHAR(20)   NOT NULL,   -- CHECKING | SAVING
    name            VARCHAR(60)   NOT NULL,   -- 예: WON 통장, 워니 절약 적금
    account_no      VARCHAR(30)   NOT NULL,   -- 모의 계좌번호
    balance         DECIMAL(15,2) NOT NULL DEFAULT 0,
    goal_amount     DECIMAL(15,2) NULL,       -- 적금 목표액 (예: 100000)
    opened_on       DATE          NOT NULL,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE card (
    id                      VARCHAR(64)   PRIMARY KEY,
    customer_id             VARCHAR(64)   NOT NULL,
    card_product_id         VARCHAR(64)   NOT NULL,
    masked_number           VARCHAR(20)   NOT NULL,     -- ****1234
    expires_on              DATE          NOT NULL,     -- 유효기간 (해당 월 말일)
    status                  VARCHAR(20)   NOT NULL,     -- ACTIVE | SUSPENDED | LOST | EXPIRED
    credit_limit            DECIMAL(15,2) NULL,         -- 체크카드는 NULL
    available_limit         DECIMAL(15,2) NULL,
    billing_day             TINYINT       NULL,         -- 카드대금 결제일 (체크카드 NULL)
    settlement_account_id   VARCHAR(64)   NULL,         -- 결제계좌 / 체크카드 연결 계좌
    revolving               BOOLEAN       NOT NULL DEFAULT FALSE,
    issued_on               DATE          NOT NULL,
    annual_fee_month        TINYINT       NULL,         -- 연회비 청구 월
    outstanding_amount      DECIMAL(15,4) NULL,          -- 미청구/미결제 금액. 자료 없으면 NULL
    revolving_balance       DECIMAL(15,4) NULL,
    revolving_apr           DECIMAL(9,6)  NULL,          -- 비율: 0.15 = 연 15%. 할인율(30=30%)과 단위가 다름
    CONSTRAINT fk_card_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_card_product FOREIGN KEY (card_product_id) REFERENCES card_product(id),
    CONSTRAINT fk_card_settlement FOREIGN KEY (settlement_account_id) REFERENCES bank_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 월별 이용실적 (전월실적·할인 한도 판단용). 승인 내역에서 집계.
CREATE TABLE card_monthly_stat (
    card_id                 VARCHAR(64)   NOT NULL,
    ym                      CHAR(7)       NOT NULL,     -- 2026-09
    spend_amount            DECIMAL(15,2) NOT NULL,
    tx_count                INT           NOT NULL,
    benefit_discount_used   DECIMAL(15,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (card_id, ym),
    CONSTRAINT fk_stat_card FOREIGN KEY (card_id) REFERENCES card(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 꿀머니
CREATE TABLE point_wallet (
    customer_id         VARCHAR(64)   PRIMARY KEY,
    balance             DECIMAL(15,2) NOT NULL DEFAULT 0,
    expiring_amount     DECIMAL(15,2) NOT NULL DEFAULT 0,
    expiring_on         DATE          NULL,
    CONSTRAINT fk_point_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ---------------------------------------------------------------------
-- [구독] 판단 규칙이 승인 내역에서 추정한 구독
-- ---------------------------------------------------------------------
CREATE TABLE subscription (
    id                    VARCHAR(64)   PRIMARY KEY,
    customer_id           VARCHAR(64)   NOT NULL,
    card_id               VARCHAR(64)   NOT NULL,
    merchant_id           VARCHAR(64)   NOT NULL,
    service_id            VARCHAR(64)   NULL,          -- APPLE.COM/BILL 등 모르면 NULL
    display_name          VARCHAR(60)   NOT NULL,      -- 화면 표시명
    user_label            VARCHAR(60)   NULL,          -- 고객이 알려준 이름 (결제명 해독 R02 결과)
    category              VARCHAR(30)   NOT NULL,
    billing_cycle         VARCHAR(10)   NOT NULL,      -- MONTHLY | YEARLY
    amount                DECIMAL(15,2) NOT NULL,      -- 최근 결제 금액(원화). TRIAL은 예상 첫 결제 금액
    currency              CHAR(3)       NOT NULL DEFAULT 'KRW',
    original_amount       DECIMAL(15,2) NULL,          -- 외화 금액 (USD 등)
    previous_amount       DECIMAL(15,2) NULL,          -- 직전 금액. 가격 인상 판단(R05)
    charge_day            TINYINT       NOT NULL,      -- 매달 결제일
    first_charged_on      DATE          NULL,          -- 구독 기간 계산용
    last_charged_on       DATE          NULL,
    next_charge_on        DATE          NOT NULL,
    status                VARCHAR(20)   NOT NULL,      -- ACTIVE | TRIAL | CANCEL_REQUESTED | CANCEL_CONFIRMED | EXCLUDED
    status_changed_at     DATETIME(6)     NULL,
    created_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    detection_confidence  DECIMAL(5,4)  NOT NULL,      -- 0~1
    user_confirmed        BOOLEAN       NOT NULL DEFAULT FALSE,
    detected_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sub_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_sub_card FOREIGN KEY (card_id) REFERENCES card(id),
    CONSTRAINT fk_sub_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id),
    CONSTRAINT fk_sub_service FOREIGN KEY (service_id) REFERENCES service(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ---------------------------------------------------------------------
-- [결제] 카드 승인 내역 (모든 판단의 출발점)
-- ---------------------------------------------------------------------
CREATE TABLE card_transaction (
    id                VARCHAR(64)   PRIMARY KEY,
    card_id           VARCHAR(64)   NOT NULL,
    customer_id       VARCHAR(64)   NOT NULL,         -- 조회 편의용 비정규화
    approved_at       DATETIME      NOT NULL,
    merchant_id       VARCHAR(64)   NOT NULL,
    merchant_name_raw VARCHAR(100)  NOT NULL,
    category          VARCHAR(40)   NOT NULL,
    amount            DECIMAL(15,2) NOT NULL,         -- 원화 승인 금액
    currency          CHAR(3)       NOT NULL DEFAULT 'KRW',
    original_amount   DECIMAL(15,2) NULL,             -- 원통화 금액. 원화 결제이면 amount와 같음
    exchange_rate     DECIMAL(15,6) NULL,             -- 1 원통화 단위당 원화 금액. 실시간 환율 아님
    status            VARCHAR(20)   NOT NULL,         -- APPROVED | CANCELED | DECLINED
    canceled_at       DATETIME      NULL,
    decline_reason    VARCHAR(40)   NULL,             -- LIMIT_EXCEEDED | INSUFFICIENT_BALANCE | CARD_SUSPENDED | EXPIRED
    subscription_id   VARCHAR(64)   NULL,             -- 구독으로 판정된 결제면 연결
    CONSTRAINT fk_tx_card FOREIGN KEY (card_id) REFERENCES card(id),
    CONSTRAINT fk_tx_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_tx_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id),
    CONSTRAINT fk_tx_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_tx_customer_time ON card_transaction (customer_id, approved_at);
CREATE INDEX idx_tx_card_time     ON card_transaction (card_id, approved_at);
CREATE INDEX idx_tx_merchant_time ON card_transaction (merchant_id, approved_at);

-- ---------------------------------------------------------------------
-- [AI] 판단 규칙(코드)이 찾은 '순간' = 매트릭스의 행(R01~R12)
--  signal_type ↔ 행
--   SUBSCRIPTION_OVERVIEW R01 | MERCHANT_UNKNOWN R02 | CHARGE_UPCOMING R03 | TRIAL_ENDING R04
--   PRICE_CHANGED R05 | DUPLICATE_CATEGORY R06 | ANNUAL_RENEWAL R07 | CANCEL_CHECK R08
--   USAGE_CHECK R09 | CARD_RECOMMEND R10 | CARD_SCHEDULE R12   (R11은 말투 수정자라 신호 없음)
--  열(C·P·G 데이터)과의 결합은 AI가 컨텍스트를 보고 정한다.
-- ---------------------------------------------------------------------
CREATE TABLE detected_signal (
    id               VARCHAR(64)  PRIMARY KEY,
    customer_id      VARCHAR(64)  NOT NULL,
    signal_type      VARCHAR(40)  NOT NULL,
    matrix_row       CHAR(3)      NOT NULL,          -- R01 ~ R12
    subscription_id  VARCHAR(64)  NULL,
    card_id          VARCHAR(64)  NULL,
    target_date      DATE         NULL,              -- 결제 예정일, 만료일 등
    detected_on      DATE         NOT NULL,
    priority         TINYINT      NOT NULL,          -- 1 주의 · 2 기한 임박 · 3 축하 · 4 제안 · 5 정보
    facts            JSON         NOT NULL,          -- AI에 그대로 전달할 사실 (숫자는 여기 값만 사용)
    status           VARCHAR(20)  NOT NULL DEFAULT 'NEW', -- NEW | USED | EXPIRED | SUPPRESSED
    dedupe_key       VARCHAR(160) NOT NULL,          -- 같은 순간 중복 생성 방지
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_signal_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_signal_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_signal_card FOREIGN KEY (card_id) REFERENCES card(id),
    CONSTRAINT uq_signal_dedupe UNIQUE (dedupe_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_signal_customer_status ON detected_signal (customer_id, status, priority);

-- AI가 만든 질문·답변 (선알림 PROACTIVE / 평소 대화 CHAT)
CREATE TABLE suggestion (
    id                    VARCHAR(64)   PRIMARY KEY,
    customer_id           VARCHAR(64)   NOT NULL,
    mode                  VARCHAR(10)   NOT NULL,     -- PROACTIVE | CHAT
    signal_ids            JSON          NULL,         -- 사용한 detected_signal id 목록
    primary_moment_id     VARCHAR(20)   NULL,         -- 예: M-R03-S
    modifier_moment_ids   JSON          NULL,         -- 예: ["M-R03-G1","M-R11-P1"]
    novel_combination     BOOLEAN       NOT NULL DEFAULT FALSE,
    tone                  VARCHAR(20)   NOT NULL,     -- CELEBRATE | SUGGEST | CAUTION | FORMAL | STEP_BACK | ANSWER
    message               VARCHAR(4000) NOT NULL,     -- 현재 상담 답변의 최대 길이 보존
    buttons               JSON          NOT NULL,     -- [{"label":"아니","action":"CANCEL_GUIDE","target_id":"sub-watcha"}]
    calendar_event        JSON          NULL,         -- CALENDAR_PREVIEW 버튼용 일정 초안
    status                VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- PENDING | SHOWN | ANSWERED | DISMISSED | EXPIRED | REJECTED
    validation_passed     BOOLEAN       NOT NULL,     -- 숫자·동의·금지 표현 검증 결과
    model                 VARCHAR(60)   NOT NULL,
    raw_output            JSON          NULL,         -- 모델 원본 출력 (로그)
    created_at            DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    shown_at              DATETIME(6)     NULL,
    CONSTRAINT fk_suggestion_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_suggestion_customer_time ON suggestion (customer_id, created_at);

-- 고객 응답 (버튼). 응답 이력은 다음 질문의 빈도·말투를 바꾸는 데이터가 된다.
CREATE TABLE suggestion_response (
    id              VARCHAR(64)  PRIMARY KEY,
    suggestion_id   VARCHAR(64)  NOT NULL,
    customer_id     VARCHAR(64)  NOT NULL,
    action          VARCHAR(40)  NOT NULL,   -- KEEP | CANCEL_GUIDE | REMIND_LATER | SAVE_TO_DEPOSIT ...
    button_label    VARCHAR(60)  NOT NULL,
    target_id       VARCHAR(64)  NULL,
    responded_at    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_response_suggestion FOREIGN KEY (suggestion_id) REFERENCES suggestion(id),
    CONSTRAINT fk_response_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_response_customer_time ON suggestion_response (customer_id, responded_at);

-- 평소 질의응답 대화 기록
CREATE TABLE assistant_conversation (
    id              VARCHAR(64) PRIMARY KEY,
    customer_id     VARCHAR(64) NOT NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_conversation_customer UNIQUE (customer_id),
    CONSTRAINT uq_conversation_owner UNIQUE (id, customer_id),
    CONSTRAINT fk_conversation_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 대화의 단일 저장소. assistant_message를 별도로 만들지 않는다.
CREATE TABLE chat_message (
    id              VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    customer_id     VARCHAR(64) NOT NULL,
    role            VARCHAR(10) NOT NULL,       -- USER | ASSISTANT (현재 API 역할 값 유지)
    source          VARCHAR(32) NOT NULL,       -- USER_INPUT | ASSISTANT_REPLY | PRICE_INCREASE | FINANCE_NOTICE
    content         VARCHAR(4000) NOT NULL,
    source_key      VARCHAR(180) COLLATE utf8mb4_bin NULL,
    suggestion_id   VARCHAR(64) NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at         DATETIME(6) NULL,
    CONSTRAINT uq_chat_source UNIQUE (source_key),
    CONSTRAINT fk_chat_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_chat_conversation FOREIGN KEY (conversation_id, customer_id)
        REFERENCES assistant_conversation(id, customer_id),
    CONSTRAINT fk_chat_suggestion FOREIGN KEY (suggestion_id) REFERENCES suggestion(id),
    CONSTRAINT ck_chat_role CHECK (role IN ('USER','ASSISTANT')),
    CONSTRAINT ck_chat_source CHECK (
        (role = 'USER' AND source = 'USER_INPUT') OR
        (role = 'ASSISTANT' AND source IN ('ASSISTANT_REPLY','PRICE_INCREASE','FINANCE_NOTICE'))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_chat_customer_time ON chat_message (customer_id, created_at, id);
CREATE INDEX idx_chat_conversation_time ON chat_message (conversation_id, created_at, id);
CREATE INDEX idx_chat_unread ON chat_message (conversation_id, role, read_at);

-- 아낀 만큼 적금 이체 (모의)
CREATE TABLE saving_transfer (
    id                VARCHAR(64)   PRIMARY KEY,
    customer_id       VARCHAR(64)   NOT NULL,
    subscription_id   VARCHAR(64)   NULL,
    suggestion_id     VARCHAR(64)   NULL,
    from_account_id   VARCHAR(64)   NOT NULL,
    to_account_id     VARCHAR(64)   NOT NULL,
    amount            DECIMAL(15,2) NOT NULL,
    status            VARCHAR(20)   NOT NULL,   -- MOCK_COMPLETED | CANCELED
    consent_id        VARCHAR(64)   NOT NULL,   -- AFFILIATE_BANK_SHARE 동의
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_transfer_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_transfer_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_transfer_suggestion FOREIGN KEY (suggestion_id) REFERENCES suggestion(id),
    CONSTRAINT fk_transfer_from FOREIGN KEY (from_account_id) REFERENCES bank_account(id),
    CONSTRAINT fk_transfer_to FOREIGN KEY (to_account_id) REFERENCES bank_account(id),
    CONSTRAINT fk_transfer_consent FOREIGN KEY (consent_id) REFERENCES consent(id),
    CONSTRAINT ck_transfer_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ---------------------------------------------------------------------
-- [캘린더] 원문은 저장하지 않고 '신호'로만 저장
-- ---------------------------------------------------------------------
CREATE TABLE calendar_connection (
    customer_id          VARCHAR(64)  PRIMARY KEY,
    read_granted         BOOLEAN      NOT NULL DEFAULT FALSE,
    write_granted        BOOLEAN      NOT NULL DEFAULT FALSE,
    wonnie_calendar_id   VARCHAR(200) NULL,       -- Wonnie 전용 캘린더 id (쓰기는 여기에만)
    connected_at         DATETIME(6)    NULL,
    CONSTRAINT fk_calconn_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE calendar_signal (
    id               VARCHAR(64)  PRIMARY KEY,
    customer_id      VARCHAR(64)  NOT NULL,
    signal_type      VARCHAR(20)  NOT NULL,       -- BUSY | TRIP | RECURRING
    label            VARCHAR(30)  NOT NULL,       -- 일반화된 이름만: 출장 | 시험 | 마감 | 여행 | 운동 | 수업
    starts_on        DATE         NULL,
    ends_on          DATE         NULL,
    month            CHAR(7)      NULL,           -- RECURRING: 집계 월
    monthly_count    INT          NULL,           -- RECURRING: 해당 월 일정 횟수
    confidence       DECIMAL(5,4) NOT NULL,
    created_at       DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_calsig_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_calsig_customer_dates ON calendar_signal (customer_id, starts_on, ends_on);

-- 워니가 캘린더에 넣은(넣으려는) 일정. 미리보기 → 고객 확인 → 등록.
CREATE TABLE calendar_write (
    id                     VARCHAR(64)  PRIMARY KEY,
    customer_id            VARCHAR(64)  NOT NULL,
    suggestion_id          VARCHAR(64)  NULL,
    event_type             VARCHAR(30)  NOT NULL,   -- SUBSCRIPTION_CHARGE | TRIAL_END | CANCEL_REMINDER | RENEWAL | PRICE_CHANGE | REJOIN_REMINDER | CARD_SCHEDULE | CUSTOM
    title                  VARCHAR(100) NOT NULL,
    event_date             DATE         NOT NULL,
    remind_minutes_before  INT          NOT NULL DEFAULT 1440,
    show_amount            BOOLEAN      NOT NULL DEFAULT FALSE,  -- 공유 캘린더 노출 방지: 기본 숨김
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PREVIEW', -- PREVIEW | CONFIRMED | CANCELED
    google_event_id        VARCHAR(200) NULL,
    previewed_at           DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at           DATETIME(6)    NULL,   -- 고객이 [등록]을 누른 시각. NULL이면 등록 금지
    canceled_at            DATETIME(6)    NULL,
    CONSTRAINT fk_calwrite_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_calwrite_suggestion FOREIGN KEY (suggestion_id) REFERENCES suggestion(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ---------------------------------------------------------------------
-- 조회용 뷰 (AI 컨텍스트 만들 때 사용)
-- ---------------------------------------------------------------------
-- 고객별 이용 중 구독 요약 (R01)
CREATE VIEW v_subscription_overview AS
SELECT s.customer_id,
       SUM(CASE WHEN s.status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_count,
       SUM(CASE WHEN s.status = 'ACTIVE' THEN
           CASE WHEN s.billing_cycle = 'YEARLY' THEN ROUND(s.amount / 12, 2) ELSE s.amount END
           ELSE 0 END) AS monthly_total,
       SUM(CASE WHEN s.status = 'TRIAL' THEN 1 ELSE 0 END) AS trial_count,
       SUM(CASE WHEN s.billing_cycle = 'YEARLY' THEN ROUND(s.amount / 12, 2) ELSE s.amount END)
           AS projected_monthly_total
FROM subscription s
WHERE s.status IN ('ACTIVE','TRIAL')
GROUP BY s.customer_id;

-- 카드별 전월·당월 이용실적 (R03-C2, R10-C2)
CREATE VIEW v_card_spend_now AS
SELECT c.id AS card_id,
       c.customer_id,
       DATE_FORMAT(DATE_SUB(dc.today, INTERVAL 1 MONTH), '%Y-%m') AS prev_ym,
       COALESCE(p.spend_amount, 0)                                 AS prev_month_spend,
       DATE_FORMAT(dc.today, '%Y-%m')                              AS this_ym,
       COALESCE(t.spend_amount, 0)                                 AS this_month_spend
FROM card c
JOIN demo_clock dc ON dc.id = 1
LEFT JOIN card_monthly_stat p ON p.card_id = c.id AND p.ym = DATE_FORMAT(DATE_SUB(dc.today, INTERVAL 1 MONTH), '%Y-%m')
LEFT JOIN card_monthly_stat t ON t.card_id = c.id AND t.ym = DATE_FORMAT(dc.today, '%Y-%m');


-- Wonnie / MySQL 8.4 / 신규 로그 테이블 전용 마이그레이션
-- 모든 DATETIME 값은 UTC. business_at은 데모에서 이동시킨 업무 시각.
-- 팀원 V1/V2 적용 후 실행. 참조 ID는 팀원 스키마 VARCHAR(64)와 일치.
-- DB 기본 문자셋/collation은 팀원 테이블과 동일하게 utf8mb4_0900_ai_ci 사용.

CREATE TABLE wonnie_event_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL COMMENT '생산자가 생성한 불변 UUID, 재전송에도 동일',
    customer_id VARCHAR(64) NULL COMMENT '고객 없는 시스템 이벤트만 NULL',
    demo_run_id CHAR(36) NOT NULL COMMENT '처음부터 실행할 때 새 UUID',
    trace_id CHAR(36) NOT NULL COMMENT '한 사용자 요청 또는 배치 실행 추적',
    event_type VARCHAR(64) NOT NULL,
    actor_type VARCHAR(16) NOT NULL,
    actor_id VARCHAR(64) NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    reason_code VARCHAR(64) NULL,
    related_event_id CHAR(36) NULL COMMENT '앞선 이벤트의 event_id, 비동기 순서 허용',
    schema_version SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    details JSON NULL COMMENT '이벤트별 허용된 키만, 원문 및 개인정보 금지',
    business_at DATETIME(6) NOT NULL,
    occurred_at DATETIME(6) NOT NULL COMMENT '실제로 이벤트가 발생한 UTC 시각',
    recorded_at DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    PRIMARY KEY (id),
    UNIQUE KEY uq_event_id (event_id),
    KEY ix_event_customer (customer_id, business_at, id),
    KEY ix_event_target (target_type, target_id, occurred_at),
    KEY ix_event_trace (trace_id, id),
    KEY ix_event_type (event_type, recorded_at),
    KEY ix_event_run (demo_run_id, business_at),
    CONSTRAINT fk_event_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT ck_event_actor CHECK (actor_type IN ('CUSTOMER','SYSTEM','ADMIN','AGENT')),
    CONSTRAINT ck_event_outcome CHECK (outcome IN ('SUCCESS','FAILURE','SKIPPED','PENDING')),
    CONSTRAINT ck_event_details CHECK (details IS NULL OR JSON_TYPE(details) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wonnie_decision_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    subscription_id VARCHAR(64) NULL,
    suggestion_id VARCHAR(64) NULL,
    signal_ids JSON NULL COMMENT 'detected_signal ID 배열, 서비스가 소유 고객 검증',
    primary_moment_id VARCHAR(20) NULL,
    modifier_moment_ids JSON NULL,
    used_fact_refs JSON NULL COMMENT '검증된 signal/subscription/card/kb 참조만',
    knowledge_refs JSON NULL COMMENT 'RAG key, 문서 버전, section hash, 검색 순위',
    validator_version VARCHAR(48) NULL,
    validation_passed BOOLEAN NULL,
    validation_errors JSON NULL COMMENT '오류 코드와 필드 경로만, 원문 금지',
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    decision_type VARCHAR(48) NOT NULL COMMENT 'CHARGE_UPCOMING, TRIAL_ENDING, CANCEL_CHECK 등 팀원 signal_type',
    decision VARCHAR(24) NOT NULL,
    reason_code VARCHAR(64) NOT NULL,
    rule_version VARCHAR(48) NOT NULL,
    model_name VARCHAR(96) NULL COMMENT 'LLM을 사용한 경우만',
    prompt_version VARCHAR(48) NULL,
    evidence JSON NOT NULL COMMENT '근거 레코드 ID/버전 및 판단에 사용한 최소 값',
    confidence DECIMAL(5,4) NULL,
    latency_ms INT UNSIGNED NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_decision_event (event_id),
    KEY ix_decision_subscription (subscription_id, id),
    KEY ix_decision_suggestion (suggestion_id, id),
    CONSTRAINT fk_decision_event FOREIGN KEY (event_id) REFERENCES wonnie_event_log(event_id),
    CONSTRAINT fk_decision_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_decision_suggestion FOREIGN KEY (suggestion_id) REFERENCES suggestion(id),
    CONSTRAINT ck_decision_signals CHECK (signal_ids IS NULL OR JSON_TYPE(signal_ids) = 'ARRAY'),
    CONSTRAINT ck_decision_modifiers CHECK (modifier_moment_ids IS NULL OR JSON_TYPE(modifier_moment_ids) = 'ARRAY'),
    CONSTRAINT ck_decision_factrefs CHECK (used_fact_refs IS NULL OR JSON_TYPE(used_fact_refs) = 'ARRAY'),
    CONSTRAINT ck_decision_knowledge CHECK (knowledge_refs IS NULL OR JSON_TYPE(knowledge_refs) = 'ARRAY'),
    CONSTRAINT ck_decision_errors CHECK (validation_errors IS NULL OR JSON_TYPE(validation_errors) = 'ARRAY'),
    CONSTRAINT ck_decision CHECK (decision IN ('ASK','SUPPRESS','DEFER','CONFIRM','UNCONFIRMED')),
    CONSTRAINT ck_decision_evidence CHECK (JSON_TYPE(evidence) = 'OBJECT'),
    CONSTRAINT ck_decision_confidence CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wonnie_action_attempt_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    action_request_id VARCHAR(64) NOT NULL COMMENT '도메인 요청 ID, 재시도에도 동일',
    saving_transfer_id VARCHAR(64) NULL,
    calendar_write_id VARCHAR(64) NULL,
    consent_id VARCHAR(64) NULL COMMENT '현재 상태 참조; 동의 당시 근거는 consent_event_id',
    action_type VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    attempt_no SMALLINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL,
    confirmation_event_id CHAR(36) NULL COMMENT '해당 미리보기 버전에 대한 고객 확인 이벤트',
    consent_event_id CHAR(36) NULL COMMENT '실행에 필요한 동의 근거; 전체 동의는 evidence에서 참조',
    preview_version VARCHAR(64) NULL,
    result_ref VARCHAR(128) NULL COMMENT '모의 이체/캘린더 결과 ID',
    error_code VARCHAR(64) NULL COMMENT '정규화된 코드, 예외 원문 금지',
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_attempt_event (event_id),
    UNIQUE KEY uq_action_attempt (action_request_id, attempt_no),
    UNIQUE KEY uq_idempotency_attempt (idempotency_key, attempt_no),
    KEY ix_action_result (action_type, status, started_at),
    CONSTRAINT fk_attempt_event FOREIGN KEY (event_id) REFERENCES wonnie_event_log(event_id),
    CONSTRAINT fk_attempt_transfer FOREIGN KEY (saving_transfer_id) REFERENCES saving_transfer(id),
    CONSTRAINT fk_attempt_calendar FOREIGN KEY (calendar_write_id) REFERENCES calendar_write(id),
    CONSTRAINT fk_attempt_domain_consent FOREIGN KEY (consent_id) REFERENCES consent(id),
    CONSTRAINT fk_attempt_confirmation FOREIGN KEY (confirmation_event_id) REFERENCES wonnie_event_log(event_id),
    CONSTRAINT fk_attempt_consent FOREIGN KEY (consent_event_id) REFERENCES wonnie_event_log(event_id),
    CONSTRAINT ck_attempt_no CHECK (attempt_no > 0),
    CONSTRAINT ck_attempt_type CHECK (action_type IN ('GUIDE_LOOKUP','CALENDAR_CREATE','CALENDAR_UNDO','SAVING_TRANSFER_MOCK','NOTIFICATION_SEND')),
    CONSTRAINT ck_attempt_status CHECK (status IN ('SUCCESS','FAILURE','UNKNOWN','SKIPPED')),
    CONSTRAINT ck_attempt_time CHECK (finished_at IS NULL OR finished_at >= started_at),
    CONSTRAINT ck_attempt_confirm CHECK (
        status <> 'SUCCESS' OR action_type NOT IN ('CALENDAR_CREATE','CALENDAR_UNDO','SAVING_TRANSFER_MOCK')
        OR (confirmation_event_id IS NOT NULL AND preview_version IS NOT NULL)
    ),
    CONSTRAINT ck_attempt_consent CHECK (
        status <> 'SUCCESS' OR action_type NOT IN ('CALENDAR_CREATE','CALENDAR_UNDO','SAVING_TRANSFER_MOCK')
        OR consent_event_id IS NOT NULL
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- 선택 확장: 고객센터/사내 열람 이력. 001 다음 적용.
CREATE TABLE wonnie_staff_access_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    access_id CHAR(36) NOT NULL,
    staff_id VARCHAR(64) NOT NULL COMMENT '인증 서버가 제공한 직원 ID',
    staff_role VARCHAR(40) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    trace_id CHAR(36) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    purpose_code VARCHAR(40) NOT NULL,
    ticket_ref VARCHAR(64) NULL COMMENT '상담 건 ID, 상담 본문 저장 금지',
    outcome VARCHAR(16) NOT NULL,
    result_count INT UNSIGNED NOT NULL DEFAULT 0,
    recorded_at DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6)),
    UNIQUE KEY uq_staff_access (access_id),
    KEY ix_staff_access_customer (customer_id, recorded_at),
    KEY ix_staff_access_staff (staff_id, recorded_at),
    CONSTRAINT ck_staff_access_outcome CHECK (outcome IN ('ALLOWED','DENIED','FAILURE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 거부된 조회도 남기기 위해 customer FK는 의도적으로 두지 않는다.
-- 검색어/결과 본문 대신 목적 코드와 건수만 기록. 권한 검사는 서비스가 수행.


-- =====================================================================
-- 현재 가격 관측/메시징/분석 로직을 위한 추가 테이블
-- 고객·카드·거래·가맹점의 finance_* 복제 테이블은 만들지 않는다.
-- =====================================================================

CREATE TABLE subscription_price_observation (
    id              VARCHAR(64) PRIMARY KEY,
    event_id        VARCHAR(100) COLLATE utf8mb4_bin NOT NULL,
    subscription_id VARCHAR(64) NOT NULL,
    previous_amount DECIMAL(15,2) NOT NULL,
    observed_amount DECIMAL(15,2) NOT NULL,
    observed_at     DATETIME(6) NOT NULL,       -- UTC
    applied         BOOLEAN NOT NULL,
    price_increased BOOLEAN NOT NULL,
    currency        CHAR(3) NULL,
    original_amount DECIMAL(15,2) NULL,
    exchange_rate   DECIMAL(15,6) NULL,
    change_reason   VARCHAR(255) NULL,
    CONSTRAINT uq_price_event UNIQUE (event_id),
    CONSTRAINT fk_price_observation_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT ck_price_amounts CHECK (previous_amount >= 0 AND observed_amount > 0),
    CONSTRAINT ck_price_evidence CHECK ((original_amount IS NULL OR original_amount > 0)
        AND (exchange_rate IS NULL OR exchange_rate > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_price_observation_subscription_date
    ON subscription_price_observation (subscription_id, applied, observed_at);

-- 감사 로그와 별개인 RabbitMQ 전달 상태. 업무 변경과 같은 트랜잭션으로 INSERT한다.
CREATE TABLE outbox_event (
    id                VARCHAR(64) PRIMARY KEY,
    aggregate_type    VARCHAR(60) NOT NULL,
    aggregate_id      VARCHAR(64) NOT NULL,
    event_type        VARCHAR(80) NOT NULL,
    payload           TEXT NOT NULL,
    deduplication_key VARCHAR(180) COLLATE utf8mb4_bin NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at      DATETIME(6) NULL,
    CONSTRAINT uq_outbox_deduplication_key UNIQUE (deduplication_key),
    CONSTRAINT ck_outbox_payload CHECK (JSON_VALID(payload)),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING','PUBLISHED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_outbox_status_created ON outbox_event (status, created_at);

-- 구독별 부가 근거. 카드와 카테고리는 subscription/service에서 읽는다.
-- NULL은 미확인. 이용 횟수 미연동을 0회로 바꾸지 않는다.
CREATE TABLE subscription_context (
    id                      VARCHAR(64) PRIMARY KEY,
    plan_name               VARCHAR(100) NULL,
    trial_ends_on           DATE NULL,
    trial_end_confirmed     BOOLEAN NOT NULL DEFAULT FALSE,
    annual_renewal_on       DATE NULL,
    contract_status         VARCHAR(30) NULL,
    contract_verified_on    DATE NULL,
    cancelled_on            DATE NULL,
    usage_count30_days      INT NULL,
    usage_evidence          VARCHAR(500) NULL,
    bundle_description      VARCHAR(500) NULL,
    family_covered          BOOLEAN NULL,
    youth_plan_amount       DECIMAL(15,4) NULL,
    pay_per_use_amount      DECIMAL(15,4) NULL,
    CONSTRAINT fk_context_subscription FOREIGN KEY (id) REFERENCES subscription(id),
    CONSTRAINT ck_context_usage CHECK (usage_count30_days IS NULL OR usage_count30_days >= 0),
    CONSTRAINT ck_context_prices CHECK ((youth_plan_amount IS NULL OR youth_plan_amount >= 0)
        AND (pay_per_use_amount IS NULL OR pay_per_use_amount >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE finance_alternative (
    id           VARCHAR(64) PRIMARY KEY,
    category     VARCHAR(50) NOT NULL,
    region       VARCHAR(60) NOT NULL,
    name         VARCHAR(100) NOT NULL,
    monthly_cost DECIMAL(15,4) NOT NULL,
    conditions   VARCHAR(500) NOT NULL,
    CONSTRAINT ck_alternative_cost CHECK (monthly_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE finance_cohort_benchmark (
    id                           VARCHAR(64) PRIMARY KEY,
    minimum_age                  INT NOT NULL,
    maximum_age                  INT NOT NULL,
    region                       VARCHAR(60) NOT NULL,
    monthly_subscription_average DECIMAL(15,4) NOT NULL,
    sample_count                 INT NOT NULL,
    CONSTRAINT ck_cohort_age CHECK (minimum_age >= 0 AND maximum_age >= minimum_age),
    CONSTRAINT ck_cohort_values CHECK (monthly_subscription_average >= 0 AND sample_count > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE INDEX idx_subscription_customer_status_due ON subscription (customer_id, status, next_charge_on);
CREATE INDEX idx_subscription_card ON subscription (card_id);
