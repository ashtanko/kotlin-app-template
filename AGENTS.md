# Repository agent guide

This file is the canonical entry point for coding agents working in this repository.

## Start here

- Read [`.agents/README.md`](.agents/README.md) for the task-specific context map.
- Load only the relevant reference or skill; don't pull every agent document into context at once.
- See [`CLAUDE.md`](CLAUDE.md) for how the repo *operates* — build, commands, tooling pipeline,
  generated README, centralized versions, CI, git hooks, PR mechanics. This file and `.agents/` own
  how to *write the code*. The two don't duplicate each other.
- This is a **GitHub template repo**, not a product: the code under
  `src/main/kotlin/dev/shtanko/template/` (`Calculator`, `DataProcessor`,
  `DivideByZeroException`) is placeholder/example code that demonstrates the toolchain, not a
  feature to preserve as-is.

## Repository rules

- Use the Gradle wrapper (`./gradlew`); target JDK 17+ (CI builds on 17 and 21).
- Kotlin language level is pinned to `KOTLIN_2_2` in `build.gradle.kts` even though the Kotlin
  plugin/compiler is 2.4.0 — don't use language features newer than 2.2.
- Keep dependency and plugin versions in `gradle/libs.versions.toml`; don't hardcode versions
  elsewhere.
- Formatting, import order, and license headers are enforced by tooling (`spotless`, `detekt`,
  `ktlint`, `diktat`). Run `./gradlew spotlessApply` / `make check` rather than hand-formatting —
  see [`.agents/reference/commands.md`](.agents/reference/commands.md).
- Don't edit generated output (`build/`, `README.md`'s body, `config/detekt/detekt-baseline.xml`)
  unless the task explicitly requires it. `README.md` is generated from `config/main.md` — edit
  that instead and run `make md`.
- Preserve unrelated work in a dirty worktree. Inspect the diff before editing and never clean up
  changes you did not make.

## Working method

1. Inspect the target file, its neighboring examples, and the current diff before editing.
2. Make the smallest coherent change that follows the existing package and file conventions —
   mirror the canonical example files listed in
   [`.agents/reference/coding-conventions.md`](.agents/reference/coding-conventions.md).
3. Add or update tests with behavior changes, following
   [`.agents/reference/testing.md`](.agents/reference/testing.md).
4. Run the narrowest useful check first, then broaden in proportion to the change — see
   [`.agents/reference/commands.md`](.agents/reference/commands.md). Run `make check && make test`
   before finishing.
5. Report changed files, checks run, and anything not verified. Never claim a check passed if it
   was not run.

## Task-specific guidance

- Coding conventions (naming, null-safety, data modeling, SOLID): [`.agents/reference/coding-conventions.md`](.agents/reference/coding-conventions.md)
- Branching, `when` exhaustiveness, guard conditions: [`.agents/skills/kotlin-control-flow/SKILL.md`](.agents/skills/kotlin-control-flow/SKILL.md)
- Coroutine scope ownership, cancellation, `runBlocking`: [`.agents/skills/kotlin-coroutines-structured-concurrency/SKILL.md`](.agents/skills/kotlin-coroutines-structured-concurrency/SKILL.md)
- `StateFlow`/`SharedFlow`/`Channel` and event modeling: [`.agents/skills/kotlin-flow-state-event-modeling/SKILL.md`](.agents/skills/kotlin-flow-state-event-modeling/SKILL.md)
- Function ownership (member/top-level/extension): [`.agents/skills/kotlin-functions/SKILL.md`](.agents/skills/kotlin-functions/SKILL.md)
- `value class` vs `data class`: [`.agents/skills/kotlin-types-value-class/SKILL.md`](.agents/skills/kotlin-types-value-class/SKILL.md)
- Testing strategy: [`.agents/reference/testing.md`](.agents/reference/testing.md)
- Choosing which command to run: [`.agents/reference/commands.md`](.agents/reference/commands.md)
- Build, CI, tooling, versions, generated README, git hooks: [`CLAUDE.md`](CLAUDE.md)
