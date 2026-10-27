# Wonnie MCP 서버

캘린더를 제외한 카드·구독 읽기 도구 13개를 Streamable HTTP로 제공합니다. 금융 조회는 API 서버의 `/api/finance/*`로 위임합니다. MCP 자체는 DB와 OpenAI 키를 사용하지 않습니다.

```bash
pnpm mcp:dev
```

기본 주소는 `http://127.0.0.1:8081/mcp`, 상태는 `/actuator/health`입니다. `WONNIE_API_BASE_URL`로 API 주소, `MCP_PORT`로 포트를 바꿀 수 있습니다. API와 MCP를 함께 켜고 프로토콜만 확인하려면 루트의 `pnpm dev:mcp`, LLM 상담은 `pnpm dev:openai`를 사용하세요.

카드·구독 상품 조건과 거래는 가상 샘플이며 실제 금융기관 데이터가 아닙니다. 이전 결제 중지·캘린더 도구는 제공하지 않습니다. 도구 목록·계산 범위는 [기능 매트릭스](../../docs/feature-matrix.md)를 참고하세요.

```bash
pnpm mcp:test
```

실제 MCP 서버와 SDK 클라이언트 사이의 도구 검색·호출·입력 검증·상위 API 장애를 검사합니다. 실제 OpenAI API를 호출하지 않습니다.
