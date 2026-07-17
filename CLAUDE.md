# Server — AI 작업 지침 (Claude·Codex 공통)

This repository uses shared AI instructions instead of duplicating rules per tool.
(`AGENTS.md` is a symlink to this file.)

Before doing any work in this project:

1. Read `AI_GUIDE.md`.
2. Read `AI_SKILLS.md`.
3. For the current task, read every matching project-local skill under `./skills`.

Required project-local skill routing:

- Architecture, DDD, Clean Architecture, Hexagonal Architecture, ports/adapters, use cases, adapters, dependency boundaries: read `skills/architecture-patterns/SKILL.md`.
- Tests, JUnit 5, Java test code, AssertJ, fake ports, Spring Boot test context: read `skills/junit/SKILL.md`.

If multiple skills match, read all of them before planning or editing.

Do not assume `./skills/**/SKILL.md` files are auto-loaded by the runtime. Treat `AI_SKILLS.md` as the source of truth for project-local skill discovery.
