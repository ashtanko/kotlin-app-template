---
description: Run the full static-analysis suite (spotless, detekt, ktlint, diktat)
allowed-tools: Bash(make check), Bash(./gradlew spotlessApply:*), Bash(./gradlew detekt ktlintCheck diktatCheck spotlessCheck:*)
---

Run the project's static-analysis suite with Gradle and report the result.

## How to run

- Run `make check` (= `./gradlew spotlessApply spotlessCheck detekt ktlintCheck
  diktatCheck`). This auto-formats first, then verifies detekt/ktlint/diktat.
- If you only want to auto-fix formatting and license headers without the full
  verification, run `./gradlew spotlessApply` instead.

Note: this is the same suite the Stop hook runs automatically after code changes
(`scripts/claude/lint-hook.sh`) and that CI enforces — use `/lint` for an on-demand
check.

## Reporting

- **Success**: state that all checks passed in one line. Don't dump the log.
- **Failure**: show only the failing rule(s) and the file(s) they point at — not the
  whole log. detekt uses a baseline (`config/detekt/detekt-baseline.xml`), so only new
  issues are reported. Then open the relevant file and explain the likely fix.
- **Compilation error**: surface the compiler error itself rather than reporting it as
  a lint failure.

Do not modify any source files unless the user asks you to fix the reported issues.
