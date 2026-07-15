# Task Spec: 관리자 계정 대신 30일짜리 서비스 access token 도입

## 배경

- 이 프로젝트는 `server/AI_GUIDE.md`, `server/AI_SKILLS.md`를 반드시 먼저 읽고, 해당하는
  `server/skills/*/SKILL.md`(architecture-patterns, junit)를 따라 작업할 것.
- 직전 작업(`TASK_SPEC_flyway_migration.md`)에서 `db/seed/common/R__seed_admin_account.sql`에
  로그인 가능한 관리자 계정(`members`+`auth` row, phone `01000000000`)을 만들어뒀는데,
  다시 생각해보니 **관리자 계정 자체가 필요 없다**고 판단했다. 어드민 API를 호출할 수 있는
  **30일 유효기간의 access token 하나**만 있으면 충분하다.
- 조사 결과, 이 프로젝트의 인증 구조는 완전히 stateless라서 이게 가능하다:
  - `JwtAuthenticationFilter`(`infrastructure/security/JwtAuthenticationFilter.java`)는
    토큰 서명/만료/`type=access`만 검증하고, `memberId`와 `roles`를 **JWT claim에서 직접
    읽는다**. DB에서 회원 존재 여부를 조회하지 않는다.
  - 어드민 권한 체크는 `@PreAuthorize`가 아니라 커스텀 `@RoleAdminAuth` 어노테이션 +
    `RoleAuthInterceptor`(`infrastructure/security/authorization/`)가 담당하며,
    `SecurityContextHolder`의 authority가 `ROLE_ADMIN`인지만 본다 — 역시 DB 조회 없음.
  - 즉 `roles` claim에 `ADMIN`만 들어있으면, `members` 테이블에 해당 subject가 실재하지
    않아도 어드민 엔드포인트 호출이 통과한다.
  - 현재 어드민 엔드포인트: `AdminMatchingController`, `AdminGameSyncController`
    (`infrastructure/adapter/in/web/admin/`).
  - 토큰 발급/검증은 `JwtProvider`(`infrastructure/adapter/out/token/JwtProvider.java`)가
    전담. `app.jwt.refresh-token-expiration`이 이미 2592000초(30일)로 설정돼 있음 —
    참고용 상수.

## 목표

- 로그인 가능한 관리자 계정을 DB에 두지 않는다.
- 대신 `roles=[ADMIN]`, `type=access`, 만료 30일짜리 JWT를 **한 번 실행해서 생성**할 수 있는
  도구를 추가한다. 이 토큰은 실제 `members` row와 무관하게 독립적으로 동작해야 한다.
- 기존 정상 로그인 흐름(`TokenIssuer.issue(memberId, role)`)은 건드리지 않는다.

## 단계별 작업

### 1. `JwtProvider`에 만료시간을 직접 지정하는 발급 메서드 추가

`server/src/main/java/com/sportsmate/server/infrastructure/adapter/out/token/JwtProvider.java`에
아래와 같은 메서드를 추가한다 (기존 `TokenIssuer` 포트 인터페이스는 변경하지 않음 — 이건
포트 계약 밖의 운영 도구용 메서드이므로 `TokenIssuer`에 넣지 않는다):

```java
public String issueServiceToken(String subject, Role role, long expirationSeconds) {
    return buildToken(subject, List.of(role), ACCESS_TOKEN_TYPE, expirationSeconds);
}
```

기존 `buildToken(...)` private 메서드를 그대로 재사용해서, 정상 로그인 토큰과 클레임
구조(`sub`, `type`, `roles`, `iat`, `exp`)가 완전히 동일하게 만든다 — 검증 경로를 하나로
유지하기 위함.

### 2. 토큰 생성 도구 추가

`server/src/main/java/com/sportsmate/server/tools/AdminTokenGenerator.java` 신규 생성
(Spring 빈이 아닌 순수 Java, `public static void main`):

- `JWT_SECRET` 값을 다음 우선순위로 읽는다:
  1. 이미 환경변수 `JWT_SECRET`이 설정돼 있으면 그걸 그대로 쓴다.
  2. 없으면 저장소 루트의 `env/server-local-env.txt`(`server/`의 부모 디렉토리 —
     레포 루트 기준 `env/server-local-env.txt`)를 찾아서 파싱한다. 이 파일은 이미
     로컬 개발에서 `JWT_SECRET=...` 형태의 `KEY=VALUE` 줄들로 쓰이고 있는 실존 파일이다
     (`#`으로 시작하는 줄과 빈 줄은 무시). 여기서 `JWT_SECRET` 값을 추출해서 쓴다.
     경로는 `AdminTokenGenerator` 실행 시점의 working directory(`server/`)를 기준으로
     `../env/server-local-env.txt`를 우선 시도하고, 못 찾으면 프로젝트 루트를 몇 단계
     위로 탐색해서라도 `env/server-local-env.txt`를 찾는다.
  3. 둘 다 없으면 사람이 읽을 수 있는 에러 메시지(어디를 찾아봤는지 포함)를 내고 종료한다.
  - 이 파일 파싱 로직은 아주 단순하게 — 외부 dotenv 라이브러리를 새로 추가하지 말고,
    `KEY=VALUE` 줄을 직접 split해서 읽는 몇 줄짜리 코드로 충분하다.
  - `ADMIN_TOKEN_SUBJECT` (선택, 기본값 `"admin-service"`) — 환경변수로만 받는다(파일에서
    읽지 않음).
  - `ADMIN_TOKEN_DAYS` (선택, 기본값 `30`) — 환경변수로만 받는다.
- `new JwtProvider(secret, 0L, 0L)`로 인스턴스를 만든 뒤(access/refresh 만료값은
  `issueServiceToken`에서 안 쓰이므로 더미 값 허용) `issueServiceToken(subject, Role.ADMIN,
  days * 86400)`을 호출해서 토큰을 stdout에 출력한다.
- 토큰 값 외에 subject/만료일시(사람이 읽을 수 있는 형태)도 함께 stderr나 stdout에 로그로
  남겨서, 언제 만료되는지 나중에 확인할 수 있게 한다.
- 절대 토큰 값을 파일로 저장하거나 로그 파일에 남기지 않는다 — stdout 출력만.

`server/build.gradle`에 실행용 Gradle task 추가:

```groovy
tasks.register('generateAdminToken', JavaExec) {
    group = 'application'
    description = 'Mint a 30-day ADMIN-role JWT that is not tied to any member row'
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'com.sportsmate.server.tools.AdminTokenGenerator'
}
```

실행 예시(문서화만 하면 됨, 별도 스크립트 파일 불필요):
- `env/server-local-env.txt`에 이미 `JWT_SECRET`이 있으므로 보통은 그냥
  `./gradlew generateAdminToken -q`만 실행하면 된다.
- 다른 환경(예: prod 시크릿)으로 발급하고 싶을 때는 `JWT_SECRET=... ./gradlew
  generateAdminToken -q`처럼 환경변수를 직접 넘기면 파일보다 우선 적용된다.

### 3. 기존 공통 관리자 계정 시드 제거

- `server/src/main/resources/db/seed/common/R__seed_admin_account.sql`을 열어보면
  `terms` INSERT 블록과 관리자 계정(`members`/`auth`) INSERT 블록이 같이 들어있다.
  `server/src/main/resources/db/seed/common/R__seed_terms.sql`이 이미 동일한 `terms` 시드를
  독립적으로 갖고 있는지 먼저 확인한다.
  - 갖고 있다면(중복 확인됨): `R__seed_admin_account.sql` 파일을 **통째로 삭제**한다.
  - 만약 `R__seed_terms.sql`에 없는 내용이 있다면 그 차이를 `R__seed_terms.sql`로 옮긴 뒤
    삭제한다.
- `server/src/main/resources/db/seed/dev/R__seed_dev_1_members.sql`의 member 9("운영자",
  phone `01099990000`, role ADMIN)는 **건드리지 않는다** — 이건 매칭 기능 테스트용 dev
  전용 목데이터이고, 이번 작업은 "로그인 가능한 관리자 계정" 자체를 없애는 것이지 dev 테스트
  시나리오 데이터를 바꾸는 게 아니다.
- 저장소 전체에서 `01000000000`(신규 관리자 계정 phone)을 grep해서, 다른 문서/설정/스크립트가
  이 계정을 참조하고 있으면 같이 정리한다.

### 4. 테스트 추가

`server/src/test/java/com/sportsmate/server/infrastructure/adapter/out/token/JwtProviderTest.java`에
기존 스타일(`@DisplayName`, AssertJ)을 따라 케이스 추가:

- `issueServiceToken`으로 발급한 토큰이 `validate()`를 통과한다.
- `issueServiceToken`으로 발급한 토큰에서 `extractRoles()`가 지정한 role을 반환한다.
- `issueServiceToken`으로 발급한 토큰에서 `extractMemberId()`가 지정한 subject 문자열을
  그대로 반환한다(DB의 실제 회원 ID 형식이 아니어도 상관없음을 검증).
- 만료시간을 짧게(예: -1초, 이미 만료) 줘서 발급하면 `validate()`가 `false`를 반환한다.

### 5. 검증 (반드시 실제로 실행 — 코드만 작성하고 끝내지 말 것)

1. `./gradlew test`로 신규/기존 테스트 통과 확인.
2. `./gradlew generateAdminToken -q` 실행(`env/server-local-env.txt`의 `JWT_SECRET`을
   자동으로 읽음) → 토큰 문자열 획득.
3. 앱을 dev 프로필로 기동(같은 `env/server-local-env.txt` 기반이므로 동일한
   `JWT_SECRET`이 자동으로 맞물림) → 발급받은 토큰을
   `Authorization: Bearer <token>`으로 실어서 `AdminMatchingController` 또는
   `AdminGameSyncController`의 엔드포인트 중 하나를 실제로 호출 → 401/403이 아니라 정상
   응답(또는 최소한 인증/인가는 통과하고 그 다음 비즈니스 로직 단계에서 실패하는 것)을 확인.
   이게 "DB에 관리자 회원 row가 없어도 어드민 API 호출이 가능하다"는 걸 증명하는
   핵심 검증 단계다.
4. 아무 회원도 아닌 임의의 subject(예: `"admin-service"`)로 발급된 토큰이 일반 회원 전용
   엔드포인트(`memberId` 기반으로 실제 회원을 조회하는 API)를 호출했을 때 어떻게 동작하는지도
   한 번 확인해서(그런 API가 있다면) 결과를 보고에 남긴다 — 존재하지 않는 회원 ID로 조회
   시도 시 500이 나는지 404가 나는지 등은 이 도구의 사용 범위(어드민 전용 API 호출)를
   벗어나지 않게 쓰는 데 참고 정보가 된다.

### 6. 마무리 보고

작업 완료 후 다음을 정리해서 알려줄 것:

- 추가/수정/삭제된 파일 전체 목록.
- `./gradlew generateAdminToken` 실행 방법과 실제 실행 결과(토큰 값 자체는 보고에 포함하지
  않아도 됨 — 생성됐다는 사실과 claim 구조만 보고).
- 3단계 검증(어드민 엔드포인트 실제 호출 결과)에서 사용한 정확한 요청/응답.
- 이 토큰은 재발급(rotate) 전까지 30일간 유효하고, 조기 폐기 수단이 없다는 점(시크릿 전체를
  바꾸지 않는 한 개별 토큰만 무효화할 방법이 없음)을 사용자가 인지하도록 다시 한번 언급.
