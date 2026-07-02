# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

This is a **GitHub template** for bootstrapping Kotlin/JVM projects, not a product. The code under `src/main/kotlin/dev/shtanko/template/` (`Calculator`, `DataProcessor`, `DivideByZeroException`) is placeholder/example code that demonstrates the toolchain — expect it to be replaced. The value of the repo is the preconfigured build, static-analysis, testing, and CI setup. When customizing for a real project, `scripts/rename-project.sh -n <name> -p <package>` rewrites the project name, Kotlin package, and `application.mainClass` throughout the repo (`--dry-run` to preview).

**`AGENTS.md` is the single source of truth for Kotlin coding conventions** (coroutines, scope functions, SOLID, naming, testing style). Consult it rather than re-deriving style rules, and don't duplicate that content here — this file owns *how the repo operates* (build, tooling, CI), `AGENTS.md` owns *how to write the code*.

## Common commands

```bash
./gradlew build                 # compile + run tests
./gradlew run                   # run the app (main: dev.shtanko.template.ApplicationKt)
make test                       # run the test suite (= ./gradlew test)
make check                      # spotlessApply + spotlessCheck + detekt + ktlintCheck + diktatCheck
./gradlew spotlessApply         # auto-format and apply license headers (run before committing)
./gradlew pitest                # mutation testing
```

Run a single test class or method (standard Gradle test filtering):

```bash
./gradlew test --tests "dev.shtanko.template.ExampleTest"
./gradlew test --tests "dev.shtanko.template.ExampleTest.should return true when input is valid"
```

## Architecture and conventions that span multiple files

**The README is generated — never edit `README.md` by hand.** `make md` rebuilds it by concatenating `config/main.md` + `build/reports/detekt/detekt.md` + `config/license.md`. Edit `config/main.md` for prose changes, then run `make md` (or `make all`). Because the detekt markdown report is part of the README, you must have run detekt for that section to exist.

**Versions are centralized.** All dependency and plugin versions live in `gradle/libs.versions.toml` (the `libs.*` version catalog). Change versions there, not inline in `build.gradle.kts`.

**Four overlapping static-analysis tools** are enforced: `detekt`, `ktlint`, `diktat`, and `spotless`. `make check` runs all of them; CI runs them independently. Spotless additionally enforces the Apache license header on every `.kt` file, using the template in `spotless/copyright.kt` — new files must start with that header (or run `./gradlew spotlessApply` to add it). Detekt uses `config/detekt/detekt.yml` with a baseline at `config/detekt/detekt-baseline.xml` for pre-existing issues.

**Kotlin language level is pinned below the compiler version.** `build.gradle.kts` sets `apiVersion`/`languageVersion` to `KOTLIN_2_2` even though the Kotlin plugin/compiler is `2.4.0`. Don't use language features newer than 2.2 in source even though a 2.4 compiler is running.

**Coverage is dual-gated.** Kover enforces ≥ 80% (`kover.reports.verify.rule.minBound(80)`) and Jacoco enforces ≥ 50% (`jacocoTestCoverageVerification`). Reports: `./gradlew jacocoTestReport` (HTML/XML/CSV) and `./gradlew koverHtmlReport`.

**Git hooks self-install via Gradle.** The `clean` task depends on `installGitHooks`, which copies `scripts/git-hooks/*.sh` into `.git/hooks/` (Linux/macOS only). The pre-commit hook runs `detekt ktlintCheck spotlessCheck spotlessApply` and blocks the commit on failure.

**A Claude Code agent hook lints AI edits.** Separately from the git pre-commit hook above (which fires at commit time), `.claude/settings.json` registers a `Stop` hook — `scripts/claude/lint-hook.sh` — that fires when the agent finishes a turn. If any `.kt` file changed, it runs `spotlessApply` (auto-fix) then `detekt ktlintCheck diktatCheck spotlessCheck`, feeding any remaining violations back so the agent fixes them before finishing. Run the same suite on demand with the `/lint` slash command.

## CI

`.github/workflows/ci.yml` runs on pushes to `main` and PRs: build + test on JDK 17 and 21, the static-analysis tools, and coverage upload (Codecov/Codacy). Match it locally with `make check && make test` before pushing.

## PRs

Use [Conventional Commits](https://www.conventionalcommits.org/) for titles: `<type>(<scope>): <description>`, types `feat`/`fix`/`chore`/`docs`/`test`/`refactor`.
