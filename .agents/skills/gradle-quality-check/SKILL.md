---
name: gradle-quality-check
description: Run formatting, static analysis, unit tests, and mutation tests on the Kotlin codebase.
---

# Gradle Quality Check Workflow

Use this skill when verifying code changes or diagnosing build and lint failures in the repository.

## Commands

1. **Auto-format & Static Analysis**:
   ```bash
   make check
   ```
   Runs `spotlessApply` (auto-formats & applies license headers) followed by `detekt`, `ktlintCheck`, and `diktatCheck`.

2. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```
   Runs all unit tests across `:app` and `:core`. Scope to a single test with:
   ```bash
   ./gradlew :core:test --tests "dev.shtanko.template.core.ExampleTest"
   ```

3. **Coverage Verification**:
   ```bash
   ./gradlew jacocoTestReport koverHtmlReport
   ```

4. **Mutation Testing**:
   ```bash
   ./gradlew pitest
   ```

5. **Regenerate Documentation**:
   ```bash
   make md
   ```

## Guidelines
- If `detekt` fails, check if the issue is a new violation or can be resolved without expanding baseline debt.
- Do not bypass `koverVerify` or coverage gates.
