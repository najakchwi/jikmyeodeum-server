# AI Guide

This file is the shared project instruction entry point for AI coding tools.

All AI agents working in this repository should read this file first, then read `AI_SKILLS.md` to select any project-local skills under `./skills` that match the task.

## Project Summary

This repository is a Java + Spring Boot backend service for "Let's Sports" (레츠포츠).

Core stack:

- Java 21
- Spring Boot
- Gradle Groovy DSL
- Spring Web (webmvc)
- Spring Data JPA
- Spring Validation
- Spring Security (stateless JWT)
- Spring AOP/AspectJ
- PostgreSQL (dev/prod), H2 (test)
- Lombok
- springdoc-openapi (Swagger)
- JUnit 5 / AssertJ for tests

Authentication:

- Social login: Google, Kakao (ID token verification via OIDC JWKS)
- Phone number login (SMS verification)

## Architecture Baseline

This project follows a Clean Architecture / Hexagonal Architecture / DDD-oriented style.

Current package intent:

```text
src/main/java/com/sportsmate/server/
├── domain/                 # business model, use-case ports, output ports, use-case services
├── infrastructure/adapter/  # web/event input adapters and persistence/storage/oauth output adapters
└── common/                  # shared value objects, exceptions, annotations, and cross-cutting ports
```

Core rules:

- Domain entities and value objects must not depend on JPA, Spring Web, Spring Security, SDKs, HTTP clients, or persistence entities.
- Controllers are input adapters; they should call input ports and map responses.
- Persistence, object storage, SMS, and third-party OAuth integrations are output adapters.
- Use-case services coordinate domain objects and ports.
- Use-case services must not contain HTTP transport models, JPA entities, external SDK clients, or persistence models.
- Output adapters must map persistence/infrastructure models back to domain objects before crossing port boundaries.
- Prefer fake ports for use-case tests.

## Project-Specific Architecture Note

Domain entities/value objects remain framework-free, and infrastructure details belong behind ports and adapters.

## Required Skill Selection

Before implementing, inspect `AI_SKILLS.md` and load/read the relevant project-local skill files from `./skills`.

Minimum mapping:

- Architecture, module design, ports/adapters, DDD, dependency boundaries: `skills/architecture-patterns/SKILL.md`
- Unit tests, JUnit 5, AssertJ, fake ports, Spring Boot test context: `skills/junit/SKILL.md`

If more than one skill applies, read all relevant skill files.

## Coding Rules

- Preserve the existing Java style and package conventions.
- Prefer small, explicit interfaces at architectural boundaries.
- Do not introduce unsafe casts or type-suppression patterns.
- Do not delete failing tests to make the build pass.
- Do not move business logic into controllers, JPA entities, security filters, or configuration classes.
- Use comments only when the code's intent is not obvious from naming and structure.

## Verification Rules

After code changes, run the smallest relevant verification first, then broaden if needed.

Recommended commands:

```bash
./gradlew test
```

For targeted tests:

```bash
./gradlew test --tests "fully.qualified.TestClassName"
```

For documentation-only changes, at minimum inspect the diff and check for formatting/whitespace issues.

## Local Push and Deployment Rule

When the user asks to push local changes, commit the requested scope and run only `git push origin dev`.
The user creates the `dev -> main` pull request, writes the PR title/body, and merges it directly in GitHub.
After that merge pushes to `main`, `.github/workflows/ci.yml` runs tests and builds the jar, then `.github/workflows/cd.yml` tags the release and deploys to Lightsail after CI succeeds.
