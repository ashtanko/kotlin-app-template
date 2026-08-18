# GitHub Copilot Instructions for kotlin-app-template

This project is a multi-module Kotlin/JVM template (`app` + `core` + `buildSrc`) targeting JVM 17+ and Kotlin 2.2 language level.

## Key Development Rules

1. **Coding Style & Conventions**:
   - Follow all conventions defined in [`AGENTS.md`](file:///home/oleksii-shtanko/github/kotlin/kotlin-app-template/AGENTS.md).
   - Prefer `val` over `var`. Never use `!!`.
   - Single-expression functions should use expression body syntax (`fun add(a: Int, b: Int) = a + b`).
   - Keep line lengths under 120 characters (enforced by Detekt).

2. **Coroutines**:
   - Inject `CoroutineDispatcher` via constructor with default `= Dispatchers.IO`. Never hardcode `Dispatchers.IO` in method bodies.
   - Use structured concurrency (`coroutineScope`, `async`/`await`).

3. **Testing**:
   - JUnit 5 runner with `@Test` and backticked descriptive names.
   - For Flow testing, use Turbine (`flow.test { awaitItem(); awaitComplete() }`).
   - For coroutines, use `runTest` with `StandardTestDispatcher`.

4. **Multi-Module Structure**:
   - `buildSrc`: Shared build logic in `template.kotlin-library.gradle.kts`.
   - `app`: Application entry point (`Application.kt`).
   - `core`: Library/domain business logic.
