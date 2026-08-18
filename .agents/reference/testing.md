# Testing reference

Mirror [`DataProcessorTest.kt`](../../core/src/test/kotlin/dev/shtanko/template/core/DataProcessorTest.kt)
and [`ExampleTest.kt`](../../core/src/test/kotlin/dev/shtanko/template/core/ExampleTest.kt).

## Principle

Test the smallest contract that proves the behavior. The project is split into `app` and `core`
Gradle modules; each module's tests live in its own `src/test/kotlin` and run on plain JUnit 5.

## Framework and style

- **Runner**: JUnit 5 (Jupiter).
- **Assertions**: the examples use plain JUnit assertions
  (`org.junit.jupiter.api.Assertions.assertEquals` / `assertTrue` / `assertAll` / `assertThrows`) —
  match that. **AssertJ** and **MockK** (and Mockito) are on the test classpath
  (`build.gradle.kts`) and fine to use when they add clarity, but they aren't required, and the
  current examples use neither AssertJ nor mocks. Don't mock data classes or simple values —
  construct them directly.
- **Structure**: Arrange-Act-Assert in every test.
- **Naming**: descriptive backticked names (`` fun `divide by zero throws`() ``).
- **Isolation**: tests must not depend on each other or external state.
- **Parameterized / dynamic**: `@ParameterizedTest` + `@CsvSource`, or `@TestFactory` returning
  `DynamicTest`s (see `ExampleTest`).

## Coroutines and Flow

- Use `runTest` with an injected `StandardTestDispatcher`, and build the subject-under-test with
  that dispatcher (see `DataProcessorTest.setUp`). Never bridge with real-time `runBlocking` in a
  test — see
  [`kotlin-coroutines-structured-concurrency`](../skills/kotlin-coroutines-structured-concurrency/SKILL.md#7-runblocking).
- Assert `Flow` behavior with **Turbine** — `flow.test { awaitItem(); awaitComplete() }`.
- Assert the complete emission/error sequence when order matters; don't just assert on the last
  value.
- Verify cancellation and error behavior for long-running or retrying suspend functions.
- Do not use real network calls, the wall clock, or random values in a deterministic unit test.
- For `StateFlow`/`SharedFlow`/`Channel`-backed test subjects, load
  [`kotlin-flow-state-event-modeling`](../skills/kotlin-flow-state-event-modeling/SKILL.md) to
  pick the right primitive before writing the test around it.

## Mutation testing

Run `./gradlew pitest` periodically to confirm tests actually catch regressions, not just cover
lines. This isn't part of `make check`/`make test` — run it deliberately when you want confidence
beyond line/branch coverage.

## Coverage

Coverage is dual-gated (see [`CLAUDE.md`](../../CLAUDE.md)): Kover enforces ≥ 80% and Jacoco
enforces ≥ 50%. Meeting the threshold is not the goal — a test that only pads coverage without
asserting real behavior should be rewritten or removed, not kept because it's green.

## Definition of done

- The test fails for the old behavior and passes for the intended behavior when a regression is
  being fixed.
- External inputs, time, and dispatchers are deterministic.
- `./gradlew test` (or the narrower `--tests` filter while iterating, see
  [`commands.md`](commands.md)) passes, followed by `make check` before finishing.
- The final report lists exact commands run and anything not verified.
