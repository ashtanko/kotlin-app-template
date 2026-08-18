# Agent workspace

This directory contains shared, vendor-neutral context for coding agents.
[`../AGENTS.md`](../AGENTS.md) is the canonical contract; vendor files such as
[`../CLAUDE.md`](../CLAUDE.md) adapt or point to it rather than duplicating it.

## Layout

```text
.agents/
├── reference/                        # Project facts loaded only when relevant
│   ├── coding-conventions.md         # Naming, null-safety, data modeling, SOLID, quick reference
│   ├── testing.md                    # Test framework, style, coroutine/Flow testing, definition of done
│   └── commands.md                   # Which check to run for which kind of change
└── skills/                           # Reusable, portable Kotlin task workflows (SKILL.md format)
    ├── kotlin-control-flow/
    ├── kotlin-functions/
    ├── kotlin-types-value-class/
    ├── kotlin-coroutines-structured-concurrency/
    └── kotlin-flow-state-event-modeling/
```

## Context map

| Task | Load first | Add when relevant |
| --- | --- | --- |
| General coding conventions (naming, null-safety, scope functions, SOLID) | [`reference/coding-conventions.md`](reference/coding-conventions.md) | The focused skill below for a deep procedure |
| Branching / `when` expressions / exhaustiveness / smart casts | [`skills/kotlin-control-flow/SKILL.md`](skills/kotlin-control-flow/SKILL.md) | `reference/coding-conventions.md` |
| Coroutine scope ownership, cancellation, `runBlocking` | [`skills/kotlin-coroutines-structured-concurrency/SKILL.md`](skills/kotlin-coroutines-structured-concurrency/SKILL.md) | `reference/testing.md` for the test-side rules |
| `StateFlow` / `SharedFlow` / `Channel` / event modeling | [`skills/kotlin-flow-state-event-modeling/SKILL.md`](skills/kotlin-flow-state-event-modeling/SKILL.md) | The coroutines skill above |
| Choosing where a function belongs (member/top-level/extension) | [`skills/kotlin-functions/SKILL.md`](skills/kotlin-functions/SKILL.md) | — |
| `@JvmInline value class` vs `data class` | [`skills/kotlin-types-value-class/SKILL.md`](skills/kotlin-types-value-class/SKILL.md) | `kotlin-functions` for construction/parsing placement |
| Selecting or reviewing tests | [`reference/testing.md`](reference/testing.md) | `reference/commands.md` |
| Choosing which command to run | [`reference/commands.md`](reference/commands.md) | — |
| Build, CI, tooling versions, generated README, git hooks | [`../CLAUDE.md`](../CLAUDE.md) | — |

The skills use the portable `SKILL.md` format: front matter with `name`/`description`, then a
procedure. They are not registered under `.claude/skills`, so they don't auto-trigger — an agent
follows the linked file directly when the task matches.

## Where these came from

The skills under `skills/` were ported from a companion Android/Compose template's `.agents/`
workspace, filtered to the Kotlin-language material that applies to a plain JVM project (dropped:
Android component/lifecycle, Compose, Hilt/DI-framework, and Kotlin Multiplatform content — none of
it applies here, since this repo has no Android target, no UI framework, and no multiplatform
source sets). `reference/coding-conventions.md` and `reference/testing.md` are this repository's own
long-standing conventions, restructured to match that layout.

## Maintenance

- Keep [`../AGENTS.md`](../AGENTS.md) short and stable; put detailed or task-specific material here.
- Keep fast-changing dependency versions in [`../gradle/libs.versions.toml`](../gradle/libs.versions.toml)
  and link to it instead of copying values into agent docs.
- If `src/main/kotlin/dev/shtanko/template/` stops being example/placeholder code (see
  [`../CLAUDE.md`](../CLAUDE.md) — "What this repo is"), update the file references in
  `reference/coding-conventions.md`, `reference/testing.md`, and the skills under `skills/` to point
  at the new canonical examples.
- A change to `config/detekt/detekt.yml`, `.editorconfig`, or the Kotlin language-level pin in
  `build.gradle.kts` should be reflected in `reference/coding-conventions.md`'s "Enforced by
  tooling" and "Project context" sections in the same change.
