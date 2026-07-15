# Lightsail 배포 준비

이 문서는 Lightsail Ubuntu 인스턴스에서 사람이 1회 수동으로 준비해야 하는 절차다. 이후 배포는 `main` push 때 실행되는 `.github/workflows/ci.yml`과 `.github/workflows/cd.yml`이 담당한다. CI가 테스트와 빌드를 완료하고 jar artifact를 남기면, CD가 해당 artifact를 내려받아 태깅, jar 업로드, 서비스 재시작, 헬스체크를 수행한다.

## 1. 디렉터리 준비

배포 대상 서버에서 애플리케이션 디렉터리를 만든다.

```bash
sudo mkdir -p /opt/jikmyeodeum-server
sudo chown -R ubuntu:ubuntu /opt/jikmyeodeum-server
```

`deploy/jikmyeodeum-server.service`의 `User=ubuntu`는 Lightsail 기본 유저를 가정한 값이다. 별도 배포 유저를 사용할 경우 systemd 유닛의 `User=`와 `/opt/jikmyeodeum-server` 소유자를 같은 유저로 맞춘다.

## 2. 서버 `.env` 파일

`/opt/jikmyeodeum-server/.env` 파일을 만들고 아래 변수명을 채운다. 값은 서버 운영자가 직접 입력한다.

필수:

```bash
DB_URL=
DB_USERNAME=
DB_PASSWORD=
SERVER_URL=
JWT_SECRET=
KAKAO_REST_API_KEY=
KAKAO_APP_KEY=
GOOGLE_CLIENT_ID=
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_BUCKET=
R2_PUBLIC_BASE_URL=
```

선택:

```bash
SOLAPI_API_KEY=
SOLAPI_SECRET_KEY=
SOLAPI_FROM_NUMBER=
```

`SOLAPI_*` 값은 `application.yaml`에 빈 기본값이 있어 비어 있어도 기동은 가능하다. 나머지 값은 `application.yaml`과 `application-prod.yml`에서 prod 실행 시 참조한다.

## 3. systemd 등록

저장소의 `deploy/jikmyeodeum-server.service`를 서버의 systemd 경로로 복사한 뒤 등록한다.

```bash
sudo cp jikmyeodeum-server.service /etc/systemd/system/jikmyeodeum-server.service
sudo systemctl daemon-reload
sudo systemctl enable jikmyeodeum-server
```

초기 배포 전까지 `/opt/jikmyeodeum-server/app.jar`가 없으면 서비스 시작은 실패할 수 있다. 첫 GitHub Actions 배포 이후 `sudo systemctl status jikmyeodeum-server`로 상태를 확인한다.

## 4. 배포용 SSH 키

로컬에서 배포 전용 키페어를 새로 만든다.

```bash
ssh-keygen -t ed25519 -f deploy_key -N ""
```

공개키 `deploy_key.pub`는 Lightsail 서버의 배포 유저 `~/.ssh/authorized_keys`에 추가한다. 개인키 `deploy_key` 내용은 GitHub repo의 Settings -> Secrets and variables -> Actions에 `LIGHTSAIL_SSH_KEY`로 등록한다.

GitHub Actions secret:

```text
LIGHTSAIL_HOST=16.184.61.119
LIGHTSAIL_USER=ubuntu
LIGHTSAIL_SSH_KEY=<deploy_key private key content>
```

`LIGHTSAIL_USER`는 실제 systemd 서비스와 디렉터리 소유자에 맞춘 배포 유저명으로 등록한다.

## 5. 보안 그룹과 방화벽

앱 기본 포트는 `8080`이다. 리버스 프록시를 사용하지 않는다면 Lightsail 방화벽에서 필요한 접근 범위에만 8080 포트를 연다. Nginx 같은 리버스 프록시를 사용할 경우 외부 공개 포트는 프록시 포트로 제한하고, 8080은 서버 내부 통신만 허용하는 구성을 검토한다.
