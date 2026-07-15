# Task Spec: Flyway 마이그레이션 도입 + 환경별 시드 분리

## 배경

- 이 프로젝트는 `server/AI_GUIDE.md`, `server/AI_SKILLS.md`를 반드시 먼저 읽고, 해당하는
  `server/skills/*/SKILL.md`(architecture-patterns, junit)를 따라 작업할 것.
- 환경: `dev`(로컬 1인 개발, PostgreSQL), `prod`(아직 첫 배포 전 — 스키마도 데이터도 없음),
  `test`(H2, `ddl-auto: create-drop`, Flyway 대상 아님).
- 현재 `server/init.sql`이 dev 부팅마다 `DROP TABLE ... CASCADE` 후 전체 스키마 재생성 +
  시드 삽입을 수행 중(`spring.sql.init.mode: always`). `ddl-auto`는 dev/prod 모두 `validate`.
- `server/src/main/resources/db/migration/`에 `V1`~`V16` SQL 파일이 이미 존재하지만
  Flyway 의존성이 build.gradle에 없어서 **실제로 실행된 적이 없다**. 이 파일들은 전부
  기존 테이블에 대한 `ALTER`/`UPDATE`라서, 빈 DB에 바로 재생하면 실패한다(예: `V1`은
  `member_preferences` 테이블이 이미 존재한다고 가정).
- 목표: `init.sql` 기반의 "매번 전체 재생성" 방식을 걷어내고, Flyway로 스키마 이력을
  관리한다. 동시에 시드 데이터를 "모든 환경 공통(운영에도 필요한 데이터)"과
  "dev 전용(테스트/더미 데이터)"으로 분리한다.

## 최종 목표 상태

- `V0` 베이스라인 마이그레이션이 추가되어 `V0 → V1 → ... → V16`을 빈 DB에 순서대로
  적용하면 현재 엔티티가 요구하는 스키마와 정확히 일치한다(`ddl-auto: validate` 통과).
- 시드 데이터는 세 그룹으로 분리된다:
  1. **공통 시드** (dev + prod 모두 적용): 약관(`terms`), KBO 마스터 데이터
     (`leagues`/`stadiums`/`teams`), 운영 콘텐츠(`banners`/`faqs`/`avatar_presets`/
     `content_assets`), 그리고 **신규로 만들 관리자 계정 1개**.
  2. **dev 전용 시드**: 테스트 회원 1~8번과 관련된 모든 데이터(auth, 약관 동의,
     위치인증, 선호도, 통계, 스타일, 관람스타일, 알림설정, mock 경기/매칭/채팅/리뷰/알림).
  3. (신규 관리자 계정은 공통 시드에 속하며, 기존 `init.sql`의 member 9 "운영자"와는
     별개다. member 9는 dev 전용 테스트 데이터의 일부로 그대로 남는다 — 삭제하지 말 것.)
- `init.sql`과 `spring.sql.init.*` 설정은 삭제된다.
- prod에는 테스트 회원/mock 경기/mock 매칭 데이터가 전혀 들어가지 않는다.

## 단계별 작업

### 1. Flyway 의존성 추가

`server/build.gradle`의 `dependencies` 블록에 추가 (버전은 Spring Boot의
dependency-management 플러그인이 관리하므로 버전 명시하지 않음):

```groovy
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
```

### 2. `V0__baseline_schema.sql` 작성

`server/src/main/resources/db/migration/V0__baseline_schema.sql` 신규 생성.

- `server/init.sql`에서 **DDL만** 추출한다: `CREATE TABLE`, `CREATE UNIQUE INDEX`,
  `CREATE INDEX`, `ALTER TABLE ... ADD CONSTRAINT` (auth ↔ members 순환 FK 부분 포함).
- `DROP TABLE ...` 문은 전부 제외한다(V0는 빈 DB에만 적용됨).
- `INSERT` 문(시드 데이터)은 전부 제외한다 — 3단계에서 별도 처리.
- 컬럼 타입/제약조건/기본값은 `init.sql`과 **한 글자도 다르지 않게** 그대로 옮긴다.
  (이후 V1~V16이 이 스키마를 전제로 ALTER를 수행하므로 여기서 어긋나면 replay가 깨짐.)

### 3. 시드 데이터 분리

디렉토리 신규 생성:

- `server/src/main/resources/db/seed/common/`
- `server/src/main/resources/db/seed/dev/`

**공통 시드** (`db/seed/common/`), 파일을 성격별로 나눠서 작성 (Repeatable 마이그레이션,
`R__` 접두사):

- `R__seed_terms.sql` — `init.sql`의 `terms` INSERT 5건 그대로 이동.
- `R__seed_kbo_master_data.sql` — `leagues`(1건), `stadiums`(9건), `teams`(10건) INSERT
  그대로 이동.
- `R__seed_content.sql` — `banners`, `faqs`, `avatar_presets`, `content_assets` INSERT
  그대로 이동.
- 관리자 계정 시드는 더 이상 만들지 않는다. 어드민 API 호출은 별도 서비스 access token
  발급 도구를 사용한다.

**dev 전용 시드** (`db/seed/dev/`), 남은 나머지를 몇 개 파일로 나눠 이동
(FK 의존 순서를 지킬 것 — 회원 → 약관동의/부가정보 → 경기 → 매칭 → 채팅/리뷰/알림
순서). 파일명에 정렬 순서를 보장하기 위한 접두 숫자를 붙인다:

- `R__seed_dev_1_members.sql` — 테스트 회원 1~8번 `members` INSERT (member 9 "운영자"도
  여기 포함 — 기존 동작 보존을 원하면 유지, 아니면 공통 관리자 계정으로 충분하다고
  판단되면 제거해도 됨. 판단이 애매하면 **유지**하는 쪽으로 결정할 것).
- `R__seed_dev_2_auth.sql` — 위 회원들의 `auth` INSERT + `auth_terms_agreements` 채우기
  로직(`init.sql`의 CROSS JOIN 방식 그대로).
- `R__seed_dev_3_member_profile.sql` — `member_location_verifications`,
  `member_preferences`, `member_stats`, `member_styles`, `member_watch_styles`,
  `notification_settings`.
- `R__seed_dev_4_games_and_matches.sql` — `games`, `matches`, `match_applications`,
  `match_participants`, `chat_messages`.
- `R__seed_dev_5_reviews_and_notifications.sql` — `reviews`, `review_tags`,
  `notifications`.
- `R__seed_dev_6_sequence_reset.sql` — 명시적 ID로 INSERT한 모든 테이블에 대해
  `setval(pg_get_serial_sequence(...), MAX(id))` 재설정 (`init.sql` 하단 로직 그대로,
  공통 시드 테이블 몫도 포함해서 한 번에 정리).

**모든 시드 파일은 재실행 안전(idempotent)하게 작성한다** — `ON CONFLICT (...) DO
NOTHING`을 각 테이블의 실제 유니크 제약/PK에 맞게 사용. Repeatable 마이그레이션은 파일
내용(체크섬)이 바뀌면 재실행되므로, 재실행돼도 중복 삽입/에러가 나면 안 된다.

### 4. 기존 부트스트랩 제거

- `server/init.sql` 삭제.
- `server/src/main/resources/application-dev.yml`에서 `spring.sql.init.*` 블록 전체 삭제.

### 5. 프로필별 Flyway 설정

`server/src/main/resources/application.yaml` (공통 섹션에 추가):

```yaml
spring:
  flyway:
    baseline-on-migrate: false
```

`server/src/main/resources/application-dev.yml`:

```yaml
spring:
  flyway:
    locations: classpath:db/migration,classpath:db/seed/common,classpath:db/seed/dev
    clean-disabled: false
```

`server/src/main/resources/application-prod.yml`:

```yaml
spring:
  flyway:
    locations: classpath:db/migration,classpath:db/seed/common
    clean-disabled: true
```

`server/src/test/resources/application-test.yml`: 테스트는 H2 + `ddl-auto: create-drop`
그대로 유지하고 Flyway를 끈다(마이그레이션 SQL이 PostgreSQL 전용 문법을 쓰므로 H2에서
그대로 돌리면 깨짐):

```yaml
spring:
  flyway:
    enabled: false
```

### 6. 검증 (반드시 실제로 실행해서 확인 — 코드만 작성하고 끝내지 말 것)

1. 로컬 PostgreSQL을 완전히 비운 상태(또는 새 스크래치 DB)에서 `dev` 프로필로 앱을
   기동. Flyway가 `V0`~`V16` + `seed/common` + `seed/dev`를 순서대로 적용하고,
   `ddl-auto: validate`가 통과하는지 확인.
2. 기동 후 기존 테스트 계정(예: `01011112222`, 비밀번호 `pwpwpwpw1234`)으로 로그인 API가
   정상 동작하는지 확인.
3. 별도의 빈 PostgreSQL DB를 하나 더 준비해서 `prod` 프로필(`--spring.profiles.active=prod`,
   필요한 env var는 로컬 더미값으로 채워서)로 앱을 기동. `seed/dev`가 로드되지 않아
   `members`/`games`/`matches`/테스트 회원 데이터가 전혀 없는지 SQL로 직접 확인.
   **절대 실제 prod DB에 연결하지 말고 로컬 임시 DB로만 검증한다.**
4. `./gradlew test` 실행해서 기존 테스트가 전부 그대로 통과하는지 확인 (H2 경로는
   변경 없어야 하므로 실패하면 안 됨).

### 7. 마무리 보고

작업 완료 후 다음을 정리해서 알려줄 것:

- 추가/수정/삭제된 파일 전체 목록.
- 신규 관리자 계정의 실제 phone/평문 비밀번호 (사용자가 로그인해서 확인하고 나중에
  교체해야 하므로).
- 검증 단계에서 실제로 실행한 명령어와 결과.
