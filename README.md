# woonie

우리금융그룹 구독 관리 MVP입니다. 구독별 전용 지갑과 가상카드를 시뮬레이션하고,
AI가 만든 결제 중지 계획을 Mock Face ID로 승인한 뒤 Saga 워크플로로 실행합니다.

## 구성

- `apps/web`: React 19, Vite, Tailwind CSS 4, TanStack Query
- `apps/api`: Spring Boot 4, Spring AI, JPA, Flyway, RabbitMQ, OpenAPI
- `infra/docker-compose.yml`: MySQL 8.4, RabbitMQ 4

## 빠른 시작

```bash
pnpm install
pnpm dev
```

`pnpm dev`는 React 개발 서버와 H2 기반 Spring Boot API를 함께 실행합니다.

- Web: http://localhost:5173
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

### MySQL/RabbitMQ로 실행

```bash
docker compose -f infra/docker-compose.yml up -d
pnpm dev:infra
```

### Google OIDC

`google-oidc` 프로파일을 켜고 아래 환경 변수를 설정합니다.

```bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
```

자세한 아키텍처와 API 생성 흐름은 `docs/architecture.md`를 참고하세요.
