# Kotlin Development Conventions for AI Agents

When working on this repository, strictly adhere to the Kotlin coding conventions and operational rules defined in the root documentation:

- **Kotlin Coding Conventions**: Refer to [`AGENTS.md`](file:///home/oleksii-shtanko/github/kotlin/kotlin-app-template/AGENTS.md) for conventions on coroutines, dispatcher injection, scope functions, immutability, error handling, and testing style.
- **Repository Operations & Tooling**: Refer to [`CLAUDE.md`](file:///home/oleksii-shtanko/github/kotlin/kotlin-app-template/CLAUDE.md) for build, static analysis, coverage, and CI conventions.

## Key Rules
1. **JVM-Only**: Target JVM 17+ (Kotlin 2.2 language level pinned via convention plugin). No Android dependencies.
2. **Dispatcher Injection**: Always inject `CoroutineDispatcher` into classes/services doing background work (defaulting to `Dispatchers.IO`).
3. **Immutability & Null-Safety**: Prefer `val` over `var`. Never use `!!`.
4. **Testing**: JUnit 5 + Turbine for Flows + `runTest` with `StandardTestDispatcher`.
5. **Quality Gates**: Always run `make check` and `make test` before completing tasks.
