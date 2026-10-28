# Production deployment

`main`에 push되면 `.github/workflows/deploy.yml`이 테스트와 빌드를 통과한 소스를 Oracle 서버로 전송하고 Docker Compose로 배포합니다.

## 주소와 서버 포트

- 웹: `https://wonnie.minngyuseong.com` → 서버 `127.0.0.1:3003`
- API: `https://api.wonnie.minngyuseong.com` → 서버 `127.0.0.1:8083`
- 웹의 `/api`, `/oauth2`, `/login`, `/logout` 요청은 컨테이너 내부에서 API로 전달됩니다.
- MCP는 컨테이너 내부 `mcp:8081`에서 API와 연결됩니다.
- Redis는 컨테이너 내부에서 Spring Session과 인증 토큰 상태를 저장합니다.
- MySQL, RabbitMQ, MCP는 외부 포트를 열지 않습니다.

## GitHub Actions secrets

저장소 Actions 시크릿(또는 `production` 환경 시크릿)으로 다음 값을 사용합니다.

- `SSH_HOST`: Oracle 서버 IP
- `SSH_USER`: SSH 사용자
- `SSH_PRIVATE_KEY`: 배포용 개인 키
- `SSH_KNOWN_HOSTS`: 서버의 SSH host key
- `OPENAI_API_KEY`: OpenAI API 키. GitHub Actions 배포에서는 필수입니다.

workflow는 `OPENAI_API_KEY`를 SSH 표준 입력으로 전달하고 서버의 `/srv/wonnie/shared/.openai-api-key`에 권한 `600`으로 저장합니다. 키는 배포 명령의 인자나 소스 압축 파일에 넣지 않습니다. Secret이 비어 있으면 배포를 중단합니다.

배포 시 이 파일을 `OPENAI_API_KEY` 환경변수로 읽어 Docker Compose가 API 컨테이너에 전달합니다. 실행 프로필은 `prod,google-oidc,openai`로 지정되어 채팅과 RAG 임베딩이 함께 활성화됩니다. GitHub Secret을 변경하고 다시 배포하면 서버 키도 갱신됩니다.

배포 소스로 API·MCP 이미지를 빌드하고 컨테이너를 재생성하므로 새 RAG 문서와 설정도 함께 반영됩니다.

## 통합 스키마 전환

운영 Compose는 통합 V1/V2 전용 `mysql-data-v2` 볼륨을 사용합니다. 이전 V1~V4가 적용된 `mysql-data` 볼륨에 새 V1/V2를 연결하면 Flyway 체크섬 검증이 실패하므로, 기존 이력을 `repair`하거나 기존 볼륨을 삭제하지 않습니다.

처음 배포할 때 새 볼륨에 통합 스키마와 데모 데이터가 생성됩니다. Compose 프로젝트명이 `wonnie`이면 서버에는 새 `wonnie_mysql-data-v2`와 기존 `wonnie_mysql-data`가 함께 남습니다. 기존 데이터가 필요하면 이전 볼륨에서 별도로 내보내 검증된 이관 절차로 옮깁니다.

## 서버 최초 설정

```bash
sudo mkdir -p /srv/wonnie/{releases,shared}
sudo chown -R ubuntu:ubuntu /srv/wonnie

cp infra/.env.production.example /srv/wonnie/shared/.env
chmod 600 /srv/wonnie/shared/.env
# 세 비밀번호를 서로 다른 긴 난수로 교체
```

호스트 Nginx에는 `infra/nginx/host.conf`를 설치합니다.

```bash
sudo cp infra/nginx/host.conf /etc/nginx/sites-available/wonnie
sudo ln -s /etc/nginx/sites-available/wonnie /etc/nginx/sites-enabled/wonnie
sudo nginx -t
sudo systemctl reload nginx
```

Certbot이 인증서를 발급하면 같은 웹 `server` 블록에 `listen 443 ssl`과 인증서 설정을 추가합니다. `location = /api/assistant/events`는 그 HTTPS 웹 블록 안에 유지되어야 합니다. 이 경로는 장시간 열린 SSE 응답을 즉시 전달하도록 두 프록시 계층 모두에서 버퍼링과 캐시를 끕니다.

## 호스트 Nginx 설정 갱신

배포 workflow는 컨테이너를 재배포하고 활성 호스트 설정에 SSE location이 있는지는 검증하지만, 호스트 설정을 설치하거나 Nginx를 reload하지는 않습니다. 호스트 설정을 변경할 때는 현재 Certbot 인증서 설정을 보존한 채 활성 HTTPS 웹 블록을 먼저 확인하고 수동 반영합니다. `sites-enabled/wonnie`가 symlink가 아닌 일반 파일일 수도 있으므로 실제 활성 파일을 확인해야 합니다. 백업 파일은 `sites-enabled`에 두면 include되어 충돌하므로 `sites-available` 같은 비활성 경로에 저장합니다.

```bash
sudo nginx -T
active_config="$(readlink -f /etc/nginx/sites-enabled/wonnie)"
sudo cp "$active_config" \
  "/etc/nginx/sites-available/wonnie.active.bak.$(date +%Y%m%d%H%M%S)"
sudoedit "$active_config"
sudo nginx -t
sudo systemctl reload nginx
```

적용 후 로그인 세션으로 웹 도메인의 SSE 경로를 확인합니다. SSE 요청이 계속 pending인 것은 정상이며, 응답 헤더와 `connected` 이벤트가 즉시 도착해야 합니다. 채팅을 전송하면 이어서 `reply.delta`와 `reply.completed`가 도착해야 합니다.

```bash
curl --no-buffer --include \
  --cookie 'SESSION=<로그인 세션 쿠키>' \
  https://wonnie.minngyuseong.com/api/assistant/events
```

Cloudflare DNS에 프록시를 끈 상태로 다음 A 레코드를 먼저 추가합니다.

| 이름 | 값 |
| --- | --- |
| `wonnie` | Oracle 서버 IP |
| `api.wonnie` | Oracle 서버 IP |

DNS 전파 후 인증서를 발급합니다.

```bash
sudo certbot --nginx \
  -d wonnie.minngyuseong.com \
  -d api.wonnie.minngyuseong.com
```

## 선택 프로필

Compose를 수동 실행할 때 기본 `SPRING_PROFILES_ACTIVE=prod`는 외부 모델 키 없이 실행됩니다. GitHub Actions는 위에서 설명한 대로 `prod,google-oidc,openai`를 지정합니다.

- OpenAI 수동 실행: 서버 `.env`에 `OPENAI_API_KEY`를 넣고 프로필을 `prod,openai`로 변경. GitHub Actions 배포는 GitHub Secret을 사용하므로 `.env`에 키를 중복 입력할 필요가 없습니다.
- Google OIDC: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`을 넣고 프로필에 `google-oidc` 추가

`.env`는 저장소에 커밋하지 않습니다.
