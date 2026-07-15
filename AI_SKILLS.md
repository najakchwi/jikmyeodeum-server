# AI Skills Index

This file indexes project-local skills stored under `./skills`.

AI tools do not all share a universal `SKILL.md` auto-loading standard. Therefore, every AI entry file in this repository points here so agents can discover and read the relevant skill manually.

## How to Use This Index

1. Identify the user's task.
2. Pick every skill whose trigger matches the task.
3. Read the referenced `SKILL.md` before planning or editing.
4. Follow the skill unless it conflicts with explicit user instructions or verified project code.
5. If a skill conflicts with the current project implementation, call out the conflict before changing code.

## Available Project Skills

### `architecture-patterns`

Path: `skills/architecture-patterns/SKILL.md`

Use when:

- Designing or refactoring backend modules.
- Adding a new bounded context.
- Adding or changing use-case services.
- Creating or reviewing input/output ports.
- Implementing persistence, Redis, object storage, event, scheduler, or web adapters.
- Checking Clean Architecture, Hexagonal Architecture, DDD, dependency direction, aggregate boundaries, value objects, or domain events.
- Debugging code where persistence models, transport models, or adapter logic leaks into domain entities/value objects or across port boundaries.

Related reference:

- `skills/architecture-patterns/references/advanced-patterns.md`

### `junit`

Path: `skills/junit/SKILL.md`

Use when:

- Writing or refactoring tests.
- Adding JUnit 5 test cases.
- Testing Java domain objects, value objects, use-case services, or Spring Boot wiring.
- Choosing between pure unit tests, fake-port tests, and Spring context tests.
- Using AssertJ assertions or exception assertions.
- Running Gradle test commands.

## Current Skill Coverage

```text
skills/
├── architecture-patterns/
│   ├── SKILL.md
│   └── references/
│       └── advanced-patterns.md
└── junit/
    └── SKILL.md
```

## Maintenance Rule

When adding a new folder under `skills/<name>/SKILL.md`, update this index and mention when the skill should be used.

When changing architecture or testing conventions, update the corresponding skill first, then update this index only if trigger conditions changed.
