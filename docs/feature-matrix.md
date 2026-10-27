# 기능과 데이터 범위

금융 항목은 샘플 고객과 가상 카드 약관으로 실행합니다. 실제 금융기관 연동이 없으므로 `DEMO` 표시를 유지합니다. Google 로그인 사용자에게는 이번 달 Calendar Events 읽기만 사용한 구독 중단 고려 알림을 제공합니다.

| 기능 | 구현 내용 | 조회 도구 |
|---|---|---|
| 구독 목록·총액 | 동일 카드·가맹점 20~40일 반복 결제 후보, 월 환산액, 무료체험 전환 예정분, 카드 소비 중 구독 비중, 생활 고정비 합산 | get_subscriptions |
| 결제명 해독 | 가맹점 후보·확인 방법. Apple 결제만으로 특정 앱 확정 금지 | explain_merchant |
| 결제 전 확인 | 계좌 잔액, 카드 상태·한도, 전월실적 부족, 보유 카드 혜택, 포인트 사용 후보, 기록 환율 기준 예상액 | get_payment_readiness |
| 무료체험 종료 | 저장된 종료일과 전환 예정 금액, 시간 기반 채팅 안내 | get_renewal_alerts |
| 가격 인상 | 거래·관측 이력 비교, 원통화 가격과 환율/수수료 변화 구분, 청년 요금제 샘플 후보 | get_price_changes, recommend_cards |
| 중복 구독 | 같은 용도·통신 결합·가족 혜택 중복 후보. 실제 불필요 여부는 단정하지 않음 | find_subscription_overlaps |
| 연간 갱신 | 갱신 예정일, 연결 카드 상태, 포인트 소멸 등 결합 안내 | get_renewal_alerts, get_card_schedule |
| 해지 확인 | 계약 상태·확인일·이후 결제와 거절 구분. 지출 감소, 적금으로 돌릴 수 있는 예산, 리볼빙 상환 가정 이자·전월실적 영향 | get_cancellation_summary |
| 이용도 판단 | 이용 로그가 있는 항목의 1회당 비용·멤버십 손익분기, 지역 대안 샘플. 미연동 이용량은 미확인 | get_usage_analysis |
| 카드 추천·조건 | 이미 보유한 카드와 후보 상품 비교. 전월실적·한도·연회비·실제 청구 주기·연령·지역·직업 조건 | recommend_cards |
| 말투·알림 개인화 | 연령·지역·직업 기반 문맥, 소득 대비 구독비, 가상 또래 집계 비교, 선호 시간 알림 | get_customer_context |
| 카드 일정 | 대금 결제일, 만료, 포인트 소멸, 연회비 예정일. 사전 채팅 안내 | get_card_schedule, get_financial_alerts |
| 캘린더 기반 중단 제안 | webhook 즉시 재조회 + 매일 전체 재검증. 시험·디지털 디톡스 기간과 활성 OTT·음악·게임 구독을 연결해 채팅 알림 | 별도 Google Calendar 어댑터(읽기) |

실행 동작은 상담·조회·안내입니다. 카드 결제수단 변경·해지·계좌 이체·발급·적금 가입은 도구로 제공하지 않습니다. 해당 행위의 실제 외부 API와 사용자 인증·승인·거래 처리는 별도 구현이 필요합니다.

## 실제 데이터 연결 지점

- 거래 수집기가 구독을 식별한 후 `/api/internal/subscription-price-observations`로 금액·원통화·시각·고유 사건 ID를 전달합니다.
- `finance_customer`, `finance_card`, `finance_transaction`, `subscription_context`, `merchant_directory`, `finance_alternative`, `finance_cohort_benchmark`의 샘플 초기화 코드를 실제 공급자 어댑터로 대체해야 합니다.
- 실시간 환율, 카드 상품 약관·발급 심사, 서비스 이용/해지 상태, 정확한 가족 혜택 적용 범위는 해당 공급자 데이터로 확인해야 합니다.

금액·자격 계산은 API가 수행하고 LLM은 MCP 도구 결과를 근거로 대화와 설명을 생성합니다. 도구가 자료 없음으로 반환한 항목을 LLM이 채워 넣도록 하지 않습니다.
