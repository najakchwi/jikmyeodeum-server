---
name: architecture-patterns
description: Route backend architecture work for this Java Spring Boot server. Use for new bounded contexts, use cases, ports/adapters, infrastructure adapters, DDD boundaries, dependency leaks, or architecture review. For tiny adapter-only edits, read this lightweight router first and open only the referenced section you need.
---

# Architecture Patterns

This is the lightweight router for the server architecture skill. Do not load the
full reference by default.

## Baseline Rules

- `domain/<context>` owns business model, value objects, domain exceptions, use-case ports, output ports, and use-case services.
- `infrastructure/adapter/in` owns driver adapters: web controllers, schedulers, event listeners, message consumers.
- `infrastructure/adapter/out` owns driven adapters: JPA persistence, object storage, OAuth, SMS, tokens, external SDKs.
- Domain entities and value objects must not import JPA, Spring Web, Spring Security, HTTP clients, SDKs, or infrastructure packages.
- Controllers call input ports; persistence and external adapters implement output ports.
- Use-case services coordinate domain objects and ports. They must not use HTTP DTOs, JPA entities, SDK clients, or repository implementations directly.
- Output adapters map infrastructure models back to domain objects before crossing port boundaries.
- Prefer fake ports for use-case tests.

## Load Only What Matches

Use this file alone for small edits that clearly stay inside an existing adapter
and do not alter ports, use cases, package boundaries, or domain behavior.

Open `references/full-patterns.md` only when you need examples or are doing one
of these:

- Adding a new bounded context or package structure.
- Adding/changing an input port, output port, use-case service, aggregate, value object, or domain event.
- Introducing a new persistence, Redis, object storage, event, scheduler, SMS, OAuth, or web adapter.
- Refactoring business logic out of controllers, JPA entities, security filters, or configuration.
- Debugging dependency cycles or leaks between `domain` and `infrastructure`.
- Reviewing whether a change follows Clean/Hexagonal/DDD boundaries.

Open `references/advanced-patterns.md` only for cross-context coordination,
domain events, transactional boundaries, or complex application-service design.

For tests, also read `../junit/SKILL.md`.

## Quick Package Shape

```text
src/main/java/com/sportsmate/server/
├── common/
├── domain/<context>/
│   ├── <Aggregate>.java
│   ├── enums/
│   ├── event/
│   ├── exception/
│   ├── port/
│   │   ├── dto/
│   │   ├── in/
│   │   └── out/
│   ├── service/
│   └── vo/
└── infrastructure/adapter/
    ├── in/web/<context>/
    └── out/persistence/<context>/
```

## Dependency Check

Allowed:

```text
infrastructure.adapter.in.web -> domain.<context>.port.in
infrastructure.adapter.out.* -> domain.<context>.port.out
use-case service -> domain model + port.out
```

Disallowed:

```text
domain -> infrastructure
domain entity/value object -> JPA/Spring/SDK/HTTP
use-case service -> JPA entity/repository implementation/external SDK
controller -> JPA repository or persistence adapter directly
output adapter -> input adapter
```

If a file under `domain` imports a package under `infrastructure`, the boundary is broken.

## Review Checklist

- Business rules live in domain model or use-case service, not controllers/adapters.
- Port names express project-owned capabilities, not vendor APIs.
- Adapter DTOs/entities do not leak through ports.
- Transaction and framework annotations stay outside entities/value objects.
- Tests can cover use-case behavior with fake output ports unless Spring wiring is the behavior under test.
