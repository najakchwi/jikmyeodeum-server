---
name: junit
description: Write and maintain Java JUnit 5 tests for this Spring Boot server. Use when adding or refactoring unit tests, domain tests, use-case tests with fake ports, Spring Boot context tests, AssertJ assertions, Gradle test execution, or test naming conventions for this project.
---

# JUnit Testing

Use this skill to write fast, readable, and architecture-aware tests for this Java + Spring Boot project.

This project uses:

- Gradle Groovy DSL
- Java 21
- Spring Boot test starters
- JUnit 5 / JUnit Jupiter
- AssertJ
- Lombok (in test code where it reduces boilerplate)
- `tasks.named('test') { useJUnitPlatform() }`

## Test Layers

Prefer the smallest test that proves the behavior.

```text
Domain test             -> no Spring, no database, no fake ports unless needed
Use-case/service test   -> no Spring context, fake output ports, fake event publisher
Adapter/integration test -> Spring context or real infrastructure only when wiring matters
Context smoke test      -> @SpringBootTest + @ActiveProfiles("test")
```

## Project Test Style

Follow the existing test style:

```java
package com.sportsmate.server.domain.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.vo.Email;
import com.sportsmate.server.domain.member.enums.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Member 도메인 단위 테스트")
class MemberTest {

    @Test
    @DisplayName("회원 생성 시 초기 상태는 PENDING이다")
    void create_initialStatusIsPending() {
        var member = Member.create(new Email("test@example.com"), "testuser");

        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
    }
}
```

Use:

- Korean `@DisplayName` for business-readable test descriptions when existing nearby tests do so.
- English method names in `action_condition_expectedResult` style.
- Arrange / Act / Assert spacing with blank lines between phases.
- AssertJ for fluent assertions: `assertThat`, `assertThatThrownBy`.

## Domain Unit Tests

Domain tests should instantiate domain objects directly and avoid Spring.

```java
@Test
@DisplayName("잘못된 이메일 형식으로 Email 생성 시 예외가 발생한다")
void email_withInvalidFormat_throwsException() {
    assertThatThrownBy(() -> new Email("not-an-email"))
            .isInstanceOf(IllegalArgumentException.class);
}
```

Rules:

- Do not use `@SpringBootTest` for pure domain behavior.
- Test state transitions and invariants on aggregate roots.
- Test value object validation at construction time.
- Avoid testing private implementation details.

## Use-Case Tests with Fake Ports

Use fake implementations for output ports. This keeps tests fast and proves the port boundary works.

```java
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    private final FakeMemberOutPort memberOutPort = new FakeMemberOutPort();
    private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
    private final MemberService memberService = new MemberService(memberOutPort, eventPublisher);

    @Test
    @DisplayName("정상적인 회원가입이 성공한다")
    void register_success() {
        var request = new CreateMemberRequest("test@example.com", "testuser");

        var result = memberService.register(request);

        assertThat(result.getEmail().value()).isEqualTo("test@example.com");
        assertThat(result.getNickname()).isEqualTo("testuser");
        assertThat(eventPublisher.getEvents()).anyMatch(event -> event instanceof MemberRegisteredEvent);
    }

    private static class FakeMemberOutPort implements MemberOutPort {
        private final Map<MemberId, Member> members = new LinkedHashMap<>();

        @Override
        public Member save(Member member) {
            members.put(member.getId(), member);
            return member;
        }

        @Override
        public Optional<Member> findById(MemberId memberId) {
            return Optional.ofNullable(members.get(memberId));
        }

        @Override
        public Optional<Member> findByEmail(Email email) {
            return members.values().stream()
                    .filter(member -> member.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(Email email) {
            return findByEmail(email).isPresent();
        }
    }
}
```

Rules:

- Prefer fake ports over Mockito for domain/use-case tests when behavior is simple.
- Keep fakes private inside the test class unless reused across many tests.
- Use Mockito only when interaction verification is the actual behavior under test.
- Do not require PostgreSQL, Docker, or network for use-case tests.

## Spring Boot Context Tests

Use context tests only for wiring smoke checks.

```java
@SpringBootTest
@ActiveProfiles("test")
class ServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

Rules:

- Keep context-load tests minimal.
- Use `application-test.yml` and `@ActiveProfiles("test")` for Spring tests.
- Do not put domain behavior assertions in context-load smoke tests.

## Exception Assertions

Use AssertJ exception assertions:

```java
assertThatThrownBy(member::activate)
        .isInstanceOf(IllegalStateException.class);
```

When the message is part of the contract, assert it explicitly:

```java
assertThatThrownBy(() -> new Email("invalid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid email");
```

## Running Tests

Run the smallest useful scope first, then the full suite when behavior is complete.

```bash
./gradlew test
```

For a single test class, use Gradle's test filter:

```bash
./gradlew test --tests "com.sportsmate.server.domain.member.MemberTest"
```

## Checklist

Before finishing test work, verify:

- The test proves behavior, not implementation details.
- Pure domain tests do not start Spring.
- Use-case tests use fake output ports where possible.
- Test names clearly describe action, condition, and expected result.
- Exceptions and domain events are asserted when they are part of behavior.
- The relevant Gradle test command passes.
