---
name: architecture-patterns
description: Implement proven backend architecture patterns including Clean Architecture, Hexagonal Architecture, and Domain-Driven Design for this Java Spring Boot server. Use this skill when designing new bounded contexts, adding use cases, creating ports and adapters, refactoring infrastructure leaks, or debugging dependency cycles between domain, application/use-case, and infrastructure layers.
---

# Architecture Patterns

Master Clean Architecture, Hexagonal Architecture, and Domain-Driven Design in the style of this project: a Java + Spring Boot backend where business concepts live in `domain`, external systems live behind `port` interfaces, and concrete Spring/JPA/web/OAuth implementations live in `infrastructure/adapter`.

**Given:** a business capability or module to design.
**Produces:** package structure, dependency rules, port definitions, adapter boundaries, domain model examples, and test boundaries that keep the core business logic maintainable and testable.

The purpose of this skill is unchanged: keep business logic independent from delivery mechanisms, persistence models, and third-party infrastructure. The examples are written in Java 21 and mapped to the current project conventions.

## When to Use This Skill

Use this skill when:

- Designing a new backend module or bounded context.
- Adding a new use case to an existing domain such as `member`.
- Introducing a new external dependency: database, object storage, message broker, payment gateway, social login provider, SMS provider, or another service.
- Refactoring business logic out of controllers, JPA entities, security filters, or framework configuration.
- Debugging dependency cycles where `domain` starts depending on `infrastructure`.
- Creating unit tests that should run without a real database, Docker, or network dependency.
- Implementing DDD tactical patterns: entities, value objects, aggregate roots, domain events, repositories/ports, and application services.

## Current Project Shape

This project currently follows a domain-first, port-and-adapter style:

```text
src/main/java/com/sportsmate/server/
├── common/
│   ├── annotation/
│   │   └── PersistenceAdapter.java
│   ├── domain/
│   │   └── Event.java
│   ├── enums/
│   │   └── Role.java
│   ├── exception/
│   │   ├── ErrorCode.java
│   │   ├── CommonErrorCode.java
│   │   ├── AuthErrorCode.java
│   │   └── BusinessException.java
│   ├── persistence/
│   │   └── BaseTimeEntity.java
│   ├── port/out/
│   │   ├── event/
│   │   ├── oauth/
│   │   ├── sms/
│   │   ├── storage/
│   │   └── token/
│   └── vo/
│       ├── Email.java
│       └── PhoneNumber.java
├── domain/
│   └── member/
│       ├── Member.java
│       ├── enums/
│       ├── event/
│       ├── exception/
│       ├── port/
│       │   ├── in/
│       │   ├── out/
│       │   └── dto/
│       ├── service/
│       └── vo/
└── infrastructure/
    ├── adapter/
    │   ├── in/
    │   │   ├── event/
    │   │   └── web/
    │   └── out/
    │       ├── oauth/
    │       ├── persistence/
    │       ├── sms/
    │       ├── storage/
    │       └── token/
    ├── aop/
    ├── config/
    └── security/
```

Read these folders as architecture boundaries, not just naming conventions:

- `domain/<context>`: aggregate roots, value objects, domain events, domain exceptions, use-case ports, output ports, and current use-case services.
- `domain/<context>/port/in`: driving ports. These are use-case contracts called by controllers, schedulers, event consumers, or other input adapters.
- `domain/<context>/port/out`: driven ports. These are capabilities the core needs from the outside world, such as persistence or external APIs.
- `infrastructure/adapter/in`: input adapters. These translate HTTP requests, framework events, scheduled jobs, or messages into port calls.
- `infrastructure/adapter/out`: output adapters. These implement driven ports using JPA, object storage, OAuth providers, SMS providers, or SDKs.
- `common/port/out`: shared driven ports for cross-cutting infrastructure capabilities such as JWT issuance, social login token verification, SMS, or object storage.

## Core Concepts

### 1. Clean Architecture

Clean Architecture keeps framework and infrastructure details away from domain entities and value objects. Adapters depend inward through ports.

```text
infrastructure adapter -> port interface -> use-case service -> domain model
```

In this project, the rule means:

- Controllers may depend on input ports such as `RegisterMemberInPort`.
- Persistence adapters may depend on output ports such as `MemberOutPort`.
- Domain entities and value objects must not import Spring Web, JPA, Spring Security, HTTP clients, SDKs, or persistence entities.
- JPA entities and mappers stay in `infrastructure/adapter/out/persistence`.
- HTTP request handling stays in `infrastructure/adapter/in/web`.

### 2. Hexagonal Architecture: Ports and Adapters

For every external interaction, identify four pieces:

```text
Driver adapter  -> Controller, event listener, scheduler
Driver port     -> RegisterMemberInPort, GetMemberInPort, ManageMemberInPort
Driven port     -> MemberOutPort, ObjectStorage, SmsSender
Driven adapter  -> MemberPersistenceAdapter, S3StorageAdapter, SmsSenderAdapter
```

Ports belong to the inner side of the boundary. Adapters belong to `infrastructure`.

### 3. Domain-Driven Design

Use DDD tactical patterns when business rules matter:

- **Entity / Aggregate Root**: has identity and lifecycle. Example: `Member`.
- **Value Object**: immutable, validated at construction, equality by value. Example: `Email`, `PhoneNumber`, `MemberId`.
- **Domain Event**: fact in the past tense. Example: `MemberRegisteredEvent`, `MemberWithdrawnEvent`.
- **Port / Repository Abstraction**: project-owned interface for persistence or external capabilities. Example: `MemberOutPort`.
- **Application Service / Use Case**: coordinates ports and domain objects. Example: `MemberService`.
- **Adapter**: translates from framework or infrastructure details into the project-owned ports. Example: `MemberController`, `MemberPersistenceAdapter`, `KakaoAuthAdapter`.

## Dependency Rules

Follow these rules when adding or changing code:

```text
Allowed:
infrastructure.adapter.in.web -> domain.<context>.port.in
infrastructure.adapter.out.persistence -> domain.<context>.port.out
infrastructure.adapter.out.persistence -> domain entity/value object
use-case service -> domain entity/value object
use-case service -> port.out
use-case service -> port.in implementation

disallowed:
domain entity -> JPA entity
domain entity -> Spring Web / Spring Security / SDK / HTTP client
use-case service -> JPA entity / external SDK client
controller -> JPA repository directly
controller -> persistence adapter directly
JPA entity -> controller DTO
output adapter -> input adapter
```

Use imports as an architecture test. If a file under `domain` imports a package under `infrastructure`, the boundary is broken.

## Recommended Package Structure for a New Context

For a new bounded context, prefer this shape:

```text
src/main/java/com/sportsmate/server/domain/order/
├── Order.java
├── enums/
│   └── OrderStatus.java
├── event/
│   └── OrderPlacedEvent.java
├── exception/
│   ├── OrderErrorCode.java
│   └── OrderNotFoundException.java
├── port/
│   ├── dto/
│   │   ├── CreateOrderRequest.java
│   │   └── OrderResponse.java
│   ├── in/
│   │   ├── PlaceOrderInPort.java
│   │   └── GetOrderInPort.java
│   └── out/
│       └── OrderOutPort.java
├── service/
│   └── OrderService.java
└── vo/
    ├── OrderId.java
    └── Money.java

src/main/java/com/sportsmate/server/infrastructure/adapter/in/web/order/
└── OrderController.java

src/main/java/com/sportsmate/server/infrastructure/adapter/out/persistence/order/
├── OrderJpaRepository.java
├── OrderPersistenceAdapter.java
├── entity/
│   └── OrderEntity.java
└── mapper/
    └── OrderMapper.java
```

Keep the shape consistent with the existing `member` context unless there is a clear reason to evolve the architecture.

## Java Examples

### Entity / Aggregate Root

Keep the aggregate free of JPA and HTTP annotations. Model behavior as methods, not as public setters.

```java
package com.sportsmate.server.domain.member;

import com.sportsmate.server.common.vo.Email;
import com.sportsmate.server.domain.member.enums.MemberStatus;
import com.sportsmate.server.domain.member.vo.MemberId;
import java.time.LocalDateTime;

public class Member {

    private final MemberId id;
    private Email email;
    private String nickname;
    private MemberStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Member(
            MemberId id,
            Email email,
            String nickname,
            MemberStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Member create(Email email, String nickname) {
        if (nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname cannot be blank");
        }
        if (nickname.length() < 2 || nickname.length() > 50) {
            throw new IllegalArgumentException("Nickname must be between 2 and 50 characters");
        }

        LocalDateTime now = LocalDateTime.now();
        return new Member(MemberId.generate(), email, nickname, MemberStatus.PENDING, now, now);
    }

    public static Member reconstitute(
            MemberId id,
            Email email,
            String nickname,
            MemberStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Member(id, email, nickname, status, createdAt, updatedAt);
    }

    public void activate() {
        if (status == MemberStatus.ACTIVE) {
            throw new IllegalStateException("Member is already active");
        }
        if (status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("Withdrawn member cannot be activated");
        }
        this.status = MemberStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeEmail(Email newEmail) {
        this.email = newEmail;
        this.updatedAt = LocalDateTime.now();
    }

    public MemberId getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public MemberStatus getStatus() {
        return status;
    }
}
```

### Value Object

Use Java records for small validated concepts.

```java
package com.sportsmate.server.common.vo;

import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (!EMAIL_PATTERN.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
```

### Input Port

Input ports describe use cases in domain language. Controllers call these interfaces instead of concrete services.

```java
package com.sportsmate.server.domain.member.port.in;

import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.port.dto.CreateMemberRequest;

public interface RegisterMemberInPort {
    Member register(CreateMemberRequest request);
}
```

### Output Port

Output ports describe capabilities the core needs from the outside world.

```java
package com.sportsmate.server.domain.member.port.out;

import com.sportsmate.server.common.vo.Email;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.vo.MemberId;
import java.util.Optional;

public interface MemberOutPort {
    Member save(Member member);

    Optional<Member> findById(MemberId memberId);

    Optional<Member> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
```

### Use Case Service

A use-case service orchestrates domain objects and ports. It should not know HTTP request/response objects, JPA entities, or external SDKs.

```java
package com.sportsmate.server.domain.member.service;

import com.sportsmate.server.common.vo.Email;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.exception.DuplicateEmailException;
import com.sportsmate.server.domain.member.port.dto.CreateMemberRequest;
import com.sportsmate.server.domain.member.port.in.RegisterMemberInPort;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService implements RegisterMemberInPort {

    private final MemberOutPort memberOutPort;

    @Override
    public Member register(CreateMemberRequest request) {
        Email email = new Email(request.email());

        if (memberOutPort.existsByEmail(email)) {
            throw new DuplicateEmailException(request.email());
        }

        Member member = Member.create(email, request.nickname());
        return memberOutPort.save(member);
    }
}
```

### Input Adapter: REST Controller

A controller is an input adapter. It parses transport data, calls an input port, and maps the response.

```java
package com.sportsmate.server.infrastructure.adapter.in.web.member;

import com.sportsmate.server.domain.member.port.dto.CreateMemberRequest;
import com.sportsmate.server.domain.member.port.dto.MemberResponse;
import com.sportsmate.server.domain.member.port.in.RegisterMemberInPort;
import com.sportsmate.server.infrastructure.adapter.in.web.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final RegisterMemberInPort registerMemberInPort;

    @PostMapping
    public ApiResponse<MemberResponse> register(@RequestBody @Valid CreateMemberRequest request) {
        var member = registerMemberInPort.register(request);
        return ApiResponse.created(MemberResponse.from(member));
    }
}
```

Controller rules:

- Do not call JPA repositories directly.
- Do not contain business rules such as duplicate checks, state transitions, or aggregate invariants.
- Do not return persistence entities.
- Keep validation of transport shape at the boundary; keep business invariants in domain objects.

### Output Adapter: JPA Persistence

A persistence adapter implements a domain-owned output port and maps between domain objects and JPA entities.

```java
package com.sportsmate.server.infrastructure.adapter.out.persistence.member;

import com.sportsmate.server.common.annotation.PersistenceAdapter;
import com.sportsmate.server.common.vo.Email;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.member.vo.MemberId;
import com.sportsmate.server.infrastructure.adapter.out.persistence.member.mapper.MemberMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements MemberOutPort {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberMapper memberMapper;

    @Override
    @Transactional
    public Member save(Member member) {
        var entity = memberJpaRepository.findById(member.getId().value())
                .map(existing -> memberMapper.updateEntity(existing, member))
                .orElseGet(() -> memberMapper.toEntity(member));

        return memberMapper.toDomain(memberJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findById(MemberId memberId) {
        return memberJpaRepository.findById(memberId.value())
                .map(memberMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Member> findByEmail(Email email) {
        return memberJpaRepository.findByEmail(email.value())
                .map(memberMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        return memberJpaRepository.existsByEmail(email.value());
    }
}
```

Persistence rules:

- JPA annotations belong on persistence entities, not domain entities.
- Mapping belongs in infrastructure mappers or persistence entities, not controllers.
- Transactions belong at the adapter boundary or use-case boundary by deliberate decision; do not scatter transaction semantics through entities.

### Shared Output Port: Cross-Cutting Infrastructure

For cross-cutting infrastructure such as object storage, JWT issuance, social login verification, or SMS, define a shared port under `common/port/out` and implement it in `infrastructure/adapter/out`.

```java
package com.sportsmate.server.common.port.out.storage;

public interface ObjectStorage {
    StoredObject upload(ObjectUploadCommand command);

    void delete(String objectKey);

    String getUrl(String objectKey);
}
```

```java
package com.sportsmate.server.infrastructure.adapter.out.storage;

import com.sportsmate.server.common.port.out.storage.ObjectStorage;
import com.sportsmate.server.common.port.out.storage.ObjectUploadCommand;
import com.sportsmate.server.common.port.out.storage.StoredObject;
import org.springframework.stereotype.Component;

@Component
public class S3ObjectStorageAdapter implements ObjectStorage {

    @Override
    public StoredObject upload(ObjectUploadCommand command) {
        // upload to S3/R2 and return the stored object descriptor
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String objectKey) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String getUrl(String objectKey) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
```

## Testing with Fake Adapters

A correctly separated use case can be tested with fake ports and no real infrastructure.

```java
package com.sportsmate.server.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sportsmate.server.common.vo.Email;
import com.sportsmate.server.domain.member.Member;
import com.sportsmate.server.domain.member.port.dto.CreateMemberRequest;
import com.sportsmate.server.domain.member.port.out.MemberOutPort;
import com.sportsmate.server.domain.member.vo.MemberId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    private final FakeMemberOutPort memberOutPort = new FakeMemberOutPort();
    private final MemberService memberService = new MemberService(memberOutPort);

    @Test
    @DisplayName("정상적인 회원가입이 성공한다")
    void register_success() {
        var result = memberService.register(new CreateMemberRequest("test@example.com", "testuser"));

        assertThat(result.getEmail().value()).isEqualTo("test@example.com");
        assertThat(result.getNickname()).isEqualTo("testuser");
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

Testing rules:

- Domain tests should not start Spring.
- Use-case tests should use fake ports when the behavior under test is business orchestration.
- Adapter tests may start Spring or use Testcontainers when validating real JPA wiring.
- Do not delete or weaken tests to make architecture violations pass.

## Troubleshooting

### Use-case tests require a running database

Business logic has leaked into infrastructure. Move database access behind an output port and inject a fake implementation in unit tests.

### Controller contains duplicate checks, state transitions, or workflow logic

Move the logic into a use-case service. A controller should parse the request, call an input port, and map the response.

### Domain entity imports JPA annotations

Create a separate JPA entity in `infrastructure/adapter/out/persistence/<context>/entity` and map to/from the domain aggregate.

### Use-case service starts handling transport or persistence models

HTTP DTOs, JPA entities, and SDK clients should stay in adapters. Move those details behind ports or mappers.

### Output adapter returns persistence entities

Map persistence entities back to domain objects before crossing the port boundary.

### Bounded contexts import each other's aggregate roots

Introduce a lightweight ID or snapshot value object, or add an anti-corruption port. Do not share aggregate internals across contexts.

## Review Checklist

Before accepting a new architecture change, check:

- Does the package location match the responsibility?
- Does `domain` avoid importing `infrastructure`?
- Are controllers depending on input ports rather than concrete services or repositories?
- Are external systems represented by output ports?
- Are JPA/Security/SDK details confined to adapters?
- Are business invariants enforced by entities/value objects, not controllers?
- Can the use case be tested with fake ports?
- Is the new abstraction justified by a real boundary, not just ceremony?

## Advanced Patterns

For detailed DDD bounded context mapping, full multi-service project trees, Anti-Corruption Layer implementations, and Onion Architecture comparisons, see:

- [`references/advanced-patterns.md`](references/advanced-patterns.md)

## Related Skills

- `clean-ddd-hexagonal` — Apply these same rules as the project-wide architecture baseline.
- `architecture-patterns` — Use this project-local skill for Java/Spring examples and package conventions.
