# Task Spec: GitHub Actions CI/CD 구성 (Lightsail 배포)

## 배경

- 이 프로젝트는 `server/AI_GUIDE.md`, `server/AI_SKILLS.md`를 반드시 먼저 읽고, 해당하는
  `server/skills/*/SKILL.md`를 따라 작업할 것. 이번 작업은 인프라/워크플로우 작업이라
  도메인 아키텍처 규칙과는 무관하지만, `AI_GUIDE.md` 갱신은 포함되므로 형식은 맞출 것.
- 이 저장소(`server/`)는 GitHub `najakchwi/jikmyeodeum-server`(public)를 origin으로 하는
  독립 git repo다. 현재 `main` 브랜치만 있고 GitHub Actions 워크플로우가 전혀 없다.
- 배포 대상: AWS Lightsail Ubuntu 인스턴스 1대 (IP: `16.184.61.119`). Docker는 쓰지 않고,
  Gradle로 빌드한 jar를 서버에 올려 직접 `java -jar`로 실행한다. 앱 환경변수는 서버에서
  `.env` 파일로 관리한다 (GitHub Actions가 앱 시크릿을 직접 주입하지 않는다).
- 사용자가 확정한 워크플로우 (반드시 그대로 따를 것 — 더 "정교하게" 만들겠다고 임의로
  자동 PR 생성이나 dev 브랜치 트리거를 추가하지 말 것):
  1. `dev` 브랜치에서 바로 작업한다 (feature 브랜치 없음).
  2. `origin/dev`로 push해도 **어떤 GitHub Action도 실행되지 않는다.**
  3. `dev → main` PR은 **사용자가 GitHub에서 직접 생성한다.** 자동 PR 생성 기능은
     만들지 않는다. PR 제목은 버전(예: `v1.3.0`), 본문은 짧은 요약 — 이것도 사용자가
     직접 작성하므로 템플릿이나 자동 생성 로직이 필요 없다.
  4. **CI(테스트/빌드)와 CD(배포)는 `dev → main` PR이 merge되어 `main`에 push가
     발생할 때만 실행된다.** PR이 열려 있는 동안(merge 전)에는 아무 워크플로우도
     돌지 않는다. CI와 CD는 **별도 파일**(`ci.yml`, `cd.yml`)로 분리하되, CD는
     반드시 CI가 통과한 뒤에만 실행되어야 한다 (테스트 실패 시 배포가 되면 안 됨).
  5. 버전은 **main에서만 관리**한다 (`server/build.gradle`의 `version` 필드). dev용
     SNAPSHOT 버전이나 merge 후 dev로 버전을 자동으로 되돌리는 로직 등은 만들지 않는다.
     사용자가 PR을 만들 때 `build.gradle`의 버전을 직접 원하는 값으로 바꿔서 커밋한다.
  6. 로컬에서 사용자가 "push해줘" 등으로 지시하면, AI 에이전트는 커밋 + `git push origin
     dev`까지만 수행한다. PR 생성/merge는 AI가 하지 않는다.

## 최종 목표 상태

- `origin`에 `dev` 브랜치가 존재한다 (현재 `main`을 그대로 분기한 것 — `main`의
  커밋되지 않은 기존 작업 내용에는 손대지 않는다).
- `server/.github/workflows/ci.yml`(테스트+빌드, `push: branches: [main]`)과
  `server/.github/workflows/cd.yml`(태깅+배포, `ci.yml` 성공 후에만 실행) 두 파일만
  존재한다. 다른 워크플로우 파일은 만들지 않는다.
- `server/deploy/jikmyeodeum-server.service` systemd 유닛 템플릿이 존재한다.
- `server/deploy/README.md`에 Lightsail 쪽에서 사용자가 수동으로 해야 할 절차와,
  GitHub Secrets/서버 `.env`에 필요한 값 목록이 정리되어 있다.
- `server/AI_GUIDE.md`에 "로컬에서 push 지시 시 동작" 섹션이 짧게 추가되어 있다.

## 단계별 작업

### 1. `dev` 브랜치 생성

- 현재 `main`에는 커밋되지 않은 변경사항이 매우 많다 (`git status`로 확인 가능).
  이 변경들을 커밋하거나 정리하지 말 것 — 이 작업의 범위가 아니다.
- `main`의 현재 커밋 기준으로 `dev` 브랜치를 새로 만들어 `origin/dev`로 push한다.
  ```
  git checkout -b dev
  git push -u origin dev
  ```
- 워크스페이스에 이미 있는 미커밋 변경사항이 `dev` 체크아웃 과정에서 사라지거나
  충돌하지 않도록 주의할 것 (필요하면 먼저 `git stash`로 보존했다가 `dev`로 전환 후
  다시 적용하되, 그 변경을 이 작업의 일부로 커밋하지는 말 것 — working tree 상태만
  보존하면 된다).

### 2. `server/.github/workflows/ci.yml` 작성 (테스트 + 빌드)

```yaml
name: CI

on:
  push:
    branches: [main]

jobs:
  test-and-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: gradle

      - name: Run tests
        run: ./gradlew test

      - name: Build jar
        run: ./gradlew bootJar

      - name: Upload jar artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: build/libs/*.jar
          retention-days: 7
```

`ci.yml`은 빌드 산출물을 GitHub Actions 아티팩트(`app-jar`)로 남긴다 — `cd.yml`이
같은 jar를 재빌드하지 않고 그대로 받아쓰기 위함이다 (커밋 기준으로 빌드가 두 번
달라질 위험 제거).

### 3. `server/.github/workflows/cd.yml` 작성 (태깅 + 배포)

`ci.yml`이 `main` push에 대해 성공적으로 끝난 경우에만 실행되어야 하므로
`workflow_run`으로 연결한다:

```yaml
name: CD

on:
  workflow_run:
    workflows: ["CI"]
    branches: [main]
    types: [completed]

permissions:
  contents: write

jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          ref: ${{ github.event.workflow_run.head_sha }}

      - name: Download jar artifact from CI run
        uses: actions/download-artifact@v4
        with:
          name: app-jar
          path: build/libs
          github-token: ${{ secrets.GITHUB_TOKEN }}
          run-id: ${{ github.event.workflow_run.id }}

      - name: Resolve version
        id: version
        run: |
          VERSION=$(grep "^version" build.gradle | sed -E "s/version = '(.*)'/\1/")
          echo "version=$VERSION" >> "$GITHUB_OUTPUT"

      - name: Tag release
        run: |
          TAG="v${{ steps.version.outputs.version }}"
          if git rev-parse "$TAG" >/dev/null 2>&1; then
            echo "Tag $TAG already exists, skipping"
          else
            git tag "$TAG"
            git push origin "$TAG"
          fi

      - name: Locate built jar
        id: jar
        run: echo "path=$(ls build/libs/*.jar | head -n1)" >> "$GITHUB_OUTPUT"

      - name: Upload jar to Lightsail
        uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.LIGHTSAIL_HOST }}
          username: ${{ secrets.LIGHTSAIL_USER }}
          key: ${{ secrets.LIGHTSAIL_SSH_KEY }}
          source: ${{ steps.jar.outputs.path }}
          target: /opt/jikmyeodeum-server
          strip_components: 2
          rename: app.jar.new

      - name: Restart service
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.LIGHTSAIL_HOST }}
          username: ${{ secrets.LIGHTSAIL_USER }}
          key: ${{ secrets.LIGHTSAIL_SSH_KEY }}
          script: |
            set -e
            mv /opt/jikmyeodeum-server/app.jar.new /opt/jikmyeodeum-server/app.jar
            sudo systemctl restart jikmyeodeum-server

      - name: Health check
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.LIGHTSAIL_HOST }}
          username: ${{ secrets.LIGHTSAIL_USER }}
          key: ${{ secrets.LIGHTSAIL_SSH_KEY }}
          script: |
            for i in $(seq 1 10); do
              if curl -sf http://localhost:8080/actuator/health; then
                exit 0
              fi
              sleep 3
            done
            echo "Health check failed"
            exit 1
```

주의사항:

- `cd.yml`은 `workflow_run` 트리거를 쓰므로 `github.event.workflow_run.conclusion`이
  `success`일 때만 `deploy` job이 실제로 돌게 `if` 조건을 반드시 넣는다 — 이게 빠지면
  CI 실패 시에도 CD가 실행되어 버린다.
- `checkout`의 `ref`를 `github.event.workflow_run.head_sha`로 명시해야 CD가 CI와
  **동일한 커밋**을 대상으로 태깅/배포한다 (그냥 `main` HEAD를 checkout하면 그 사이에
  다른 push가 있었을 때 다른 커밋을 배포하게 되는 레이스가 생길 수 있음).
- `appleboy/scp-action`의 `source`/`strip_components`/`rename` 조합은 실제 아티팩트
  다운로드 경로(`build/libs/server-<version>.jar` 형태)에 맞춰 검증하고 필요하면
  조정할 것 — 목표는 서버의 `/opt/jikmyeodeum-server/app.jar.new`에 정확히 하나의
  jar 파일이 업로드되는 것이다.
- action 버전(`@v4`, `@v0.1.7`, `@v1.0.3` 등)은 작성 시점 기준이며, 실제 구현 시
  최신 안정 버전으로 확인 후 고정할 것.
- 두 워크플로우 모두 트리거가 `main` 관련으로 한정되어 있으므로, 실패해도(예: 시크릿
  미등록) 다른 브랜치나 PR에는 영향이 없다.

### 4. `server/deploy/jikmyeodeum-server.service` 작성

```ini
[Unit]
Description=Jikmyeodeum Server
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/jikmyeodeum-server
EnvironmentFile=/opt/jikmyeodeum-server/.env
ExecStart=/usr/bin/java -jar /opt/jikmyeodeum-server/app.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

`User=ubuntu`는 Lightsail 기본 유저를 가정한 것이며, 실제 서버에 별도 배포 유저를
쓴다면 그에 맞게 조정 가능하다는 점을 README에 언급할 것.

### 5. `server/deploy/README.md` 작성

다음 내용을 포함할 것 (직접 실행하지 말고 문서만 작성 — Lightsail 서버 접근 권한이
에이전트에게 없다는 전제):

1. **디렉터리 준비**: `/opt/jikmyeodeum-server/` 생성, 소유자를 systemd 서비스의
   `User=`와 맞출 것.
2. **`.env` 파일**: `/opt/jikmyeodeum-server/.env`에 아래 변수를 채워야 한다는 것을
   `server/src/main/resources/application.yaml`, `application-prod.yml`을 근거로
   목록화 (변수명만 나열, 값은 서버 운영자가 직접 채움):
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
   - `SERVER_URL`
   - `JWT_SECRET`
   - `KAKAO_REST_API_KEY`, `KAKAO_APP_KEY`
   - `GOOGLE_CLIENT_ID`
   - `SOLAPI_API_KEY`, `SOLAPI_SECRET_KEY`, `SOLAPI_FROM_NUMBER` (선택값, 비어도 기동은 됨)
   - `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET`, `R2_PUBLIC_BASE_URL`
3. **systemd 등록**: `jikmyeodeum-server.service`를 `/etc/systemd/system/`에 복사 →
   `sudo systemctl daemon-reload && sudo systemctl enable jikmyeodeum-server`.
4. **배포용 SSH 키**: 배포 전용 키페어를 새로 생성하고(예: `ssh-keygen -t ed25519 -f
   deploy_key -N ""`), 공개키를 Lightsail 서버의 배포 유저
   `~/.ssh/authorized_keys`에 추가, 개인키는 GitHub repo → Settings → Secrets and
   variables → Actions에 `LIGHTSAIL_SSH_KEY`로 등록. `LIGHTSAIL_HOST`는
   `16.184.61.119`, `LIGHTSAIL_USER`는 실제 사용할 유저명(예: `ubuntu`)으로 등록.
5. **보안 그룹/방화벽**: 앱이 리스닝하는 포트(기본 8080, 리버스 프록시를 쓸 경우
   그쪽 포트)가 Lightsail 방화벽에서 필요한 범위만 열려 있는지 확인하라는 안내
   (자동화하지 않음 — 운영자 판단 영역).
6. 이 문서는 사람이 수동으로 1회 세팅하는 절차이며, 이후 배포는 `ci.yml`/`cd.yml`이
   전담한다는 점을 명시.

### 6. `server/AI_GUIDE.md` 갱신

파일 끝(또는 적절한 섹션)에 짧은 섹션을 추가한다. 내용 요지:

> 사용자가 로컬에서 "push해줘" 등으로 지시하면, 변경사항을 커밋하고
> `git push origin dev`까지만 수행한다. `dev → main` PR 생성, PR 제목/본문 작성,
> merge는 전부 사용자가 GitHub에서 직접 한다 — AI가 대신 만들지 않는다. `main`으로
> merge되는 순간 `ci.yml`(테스트→빌드) → `cd.yml`(태깅→Lightsail 배포) 순으로
> 자동 실행된다.

기존 문서 구조/톤을 깨지 않는 선에서 자연스럽게 삽입할 것.

### 7. 하지 말아야 할 것 (명시적 금지 — 혼동 방지)

- `dev` push에 반응하는 워크플로우를 만들지 말 것.
- 자동으로 PR을 생성/수정하는 워크플로우(`gh pr create` 등)를 만들지 말 것.
- PR 템플릿 파일(`.github/pull_request_template.md`)을 만들지 말 것 — 사용자가
  "템플릿 안 쓸 것"이라고 명시함.
- dev 브랜치의 버전을 SNAPSHOT 등으로 자동 관리/자동 bump하는 로직을 만들지 말 것.
- `main` 브랜치의 기존 미커밋 변경사항을 임의로 커밋하거나 되돌리지 말 것 — 이 작업
  범위 밖이다.
- `ci.yml`과 `cd.yml`을 하나로 합치지 말 것 — 사용자가 명시적으로 두 파일 분리를
  요청했다. 단, `cd.yml`은 반드시 `ci.yml` 성공 시에만 실행되도록 연결할 것
  (`workflow_run` + `conclusion == 'success'` 체크).

## 검증

1. `./gradlew test`를 로컬에서 실행해 통과하는지 확인 (워크플로우가 의존하는 것과
   동일한 커맨드).
2. 가능하면 YAML 문법 검증 도구(`yamllint ci.yml cd.yml` 등, 없으면 최소한 육안으로
   들여쓰기/키 구조 재검토)로 `ci.yml`, `cd.yml` 둘 다 점검한다. 특히 `cd.yml`의
   `if: ${{ github.event.workflow_run.conclusion == 'success' }}` 조건과
   `workflows: ["CI"]` 이름이 `ci.yml`의 `name:` 값과 정확히 일치하는지 확인.
3. `systemd-analyze verify`가 사용 가능한 환경이면 `jikmyeodeum-server.service`
   문법을 검증한다. 안 되면 생략하고 그 사실을 보고에 남긴다.
4. `git push -u origin dev`가 실제로 성공했는지 (`git ls-remote origin dev` 등으로)
   확인한다.
5. 실제 Lightsail 배포/헬스체크는 GitHub Secrets가 아직 등록되지 않았으므로 이번
   작업 범위에서 실행할 수 없다 — 사용자가 위 4단계(README) 절차를 완료하고
   실제 PR을 merge해야 최초로 검증 가능하다는 점을 마무리 보고에 명시한다.

## 마무리 보고

작업 완료 후 다음을 정리해서 보고할 것:

- 신규/수정 파일 전체 목록 (`server/.github/workflows/ci.yml`,
  `server/.github/workflows/cd.yml`, `server/deploy/jikmyeodeum-server.service`,
  `server/deploy/README.md`, `server/AI_GUIDE.md` diff).
- `dev` 브랜치가 `origin`에 push되었는지 여부와 확인 커맨드/결과.
- 로컬에서 실행한 검증 커맨드와 결과 (테스트, YAML/systemd 검증 등).
- 사용자가 아직 해야 할 수동 작업 체크리스트: Lightsail 디렉터리/`.env`/systemd 등록,
  배포용 SSH 키 생성 및 `LIGHTSAIL_HOST`/`LIGHTSAIL_USER`/`LIGHTSAIL_SSH_KEY` secrets
  등록, 보안 그룹 포트 확인.
