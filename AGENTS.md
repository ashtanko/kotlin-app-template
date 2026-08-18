# Kotlin coding conventions for AI agents

This is the single source of truth for **how to write Kotlin** in this repository.
For **how the repo operates** (build, commands, tooling pipeline, generated README,
versions, CI, git hooks, PR mechanics), see [`CLAUDE.md`](./CLAUDE.md). The two files
don't duplicate each other.

## How to use this guide

1. **Tooling enforces the mechanics; this guide covers the judgment.** Formatting,
   indentation, import ordering, expression bodies, naming, modifier order, and
   trailing commas are checked automatically by `ktlint`, `detekt`, `diktat`, and
   `spotless`. Don't hand-optimize them from memory — run `./gradlew spotlessApply`
   and `make check` and let the tools decide. What follows focuses on the decisions
   the linters *can't* make for you.
2. **When unsure, mirror the canonical example files.** The most reliable pattern in
   this repo is to write code like the existing examples:
   - `core/src/main/kotlin/dev/shtanko/template/core/DataProcessor.kt` — coroutines,
     dispatcher injection, `Flow`, error handling.
   - `core/src/main/kotlin/dev/shtanko/template/core/Calculator.kt` — expression bodies, KDoc.
   - `core/src/test/kotlin/dev/shtanko/template/core/DataProcessorTest.kt` &
     `ExampleTest.kt` — the testing style (JUnit 5, coroutine tests, Turbine).

## Project context that affects code

- **JVM-only Kotlin template**, Gradle (Kotlin DSL), targeting JVM 17+ (CI builds on
  17 and 21). There is **no Android** here — ignore Android-only APIs
  (`viewModelScope`, `lifecycleScope`, `Context`, `View`, etc.).
- **Compiler is Kotlin 2.4.0, but the language level is pinned to 2.2.** The
  `template.kotlin-library` convention plugin (buildSrc) sets `apiVersion`/`languageVersion`
  to `KOTLIN_2_2` for every module. Do **not** use language features newer than 2.2 even
  though a 2.4 compiler is running.
- **Coroutines are first-class.** The `kotlinx-coroutines-jdk8` and (in tests)
  `kotlinx-coroutines-test` + Turbine dependencies are wired in — see
  `DataProcessor.kt` for the intended style.

## Enforced by tooling (don't sweat these by hand)

Running `make check` / `./gradlew spotlessApply` covers all of the following; they're
listed here only so you recognize them, not so you memorize them:

- **Formatting & indentation**: 4-space indent, LF line endings, final newline,
  no trailing whitespace (`.editorconfig`, ktlint, spotless).
- **Line length**: keep lines **≤ 120 chars** — detekt's `MaxLineLength`
  (`config/detekt/detekt.yml`) fails the build past 120. (Note: `.editorconfig`'s
  ktlint `max_line_length` is 180, so detekt is the binding constraint; the IDE shows
  visual guides at 80/120/180.)
- **No wildcard imports** (e.g. `import java.util.*`) — they pollute the namespace and
  break when the wildcard package gains a colliding class.
- **Expression bodies** for single-expression functions
  (`fun add(a: Int, b: Int) = a + b`).
- **Trailing commas** in multi-line parameter/argument lists (cleaner diffs).
- **License header**: every `.kt` file must start with the Apache header from
  `spotless/copyright.kt` (`spotlessApply` adds it).
- **Detekt baseline**: pre-existing issues are parked in
  `config/detekt/detekt-baseline.xml`; don't add to it for new code.

## Naming

- Packages: lowercase, no underscores (`dev.shtanko.template`).
- Classes / objects: `PascalCase` (`DataProcessor`).
- Functions / properties: `camelCase` (`processDataStream`, `ioDispatcher`).
- Constants (`const val`, or deeply-immutable `val` with no custom getter):
  `UPPER_SNAKE_CASE` (`MAX_COUNT`).
- Backing properties: leading underscore (`private val _items`).
- Test functions: backticked, descriptive sentences are encouraged here
  (`` fun `divide by zero throws`() ``). This is a JVM backend, so the Android
  restriction on spaces-in-backticks does not apply.

## Conventions that need judgment

### Immutability & null-safety
- **Prefer `val` over `var`.** Only reach for `var` when reassignment is genuinely
  required.
- **Never use `!!`.** Use safe calls `?.`, the Elvis operator `?:`, or restructure so
  nullability is handled explicitly. `!!` is the most common source of avoidable
  `NullPointerException`.
- **Specify nullability at Java boundaries.** Don't let platform types (`String!`)
  leak untyped into Kotlin code.

### Control flow as expressions
- Use `if`, `when`, and `try`/`catch` as **expressions** that produce a value, rather
  than statements that mutate a `var` (see `Calculator.divide`).
- Prefer `when` over deeply nested `if`/`else` chains. Keep code flat with early
  returns.

### Scope functions
- `let` — null checks / operating on a non-null value: `user?.let { render(it) }`.
- `apply` — configure an object and return it.
- `also` — side effects (logging) that return the receiver.
- `run` / `with` — grouping calls on a receiver and returning a result.
- Don't nest scope functions deeply, and don't use `let` to replace a plain `if`.

### Modeling data & state
- **`data class` for models** — free `equals`/`hashCode`/`toString`/`copy`.
- **`sealed class` / `sealed interface` for closed hierarchies** (e.g. a
  `Result`/`State` with `Loading`/`Success`/`Error`) so `when` can be exhaustive
  without an `else`.
- `enum class` only when the variants carry no distinct data.

### Collections & performance
- Use idiomatic operators (`filterNotNull()`, `map`, `forEach`) over manual
  index loops.
- Prefer `asSequence()` for large collections with several chained transformations to
  avoid intermediate allocations.
- `inline` higher-order functions to avoid lambda allocation overhead; use `reified`
  when you need the type at runtime.
- `by lazy { … }` for expensive objects that may not be needed.
- Avoid allocating in tight loops.

### Modifiers & annotations
- Follow the standard Kotlin modifier order:
  `public`/`protected`/`private`/`internal` → `expect`/`actual` →
  `final`/`open`/`abstract`/`sealed`/`const` →
  `external`/`override`/`lateinit`/`tailrec` → `vararg`/`suspend`/`inner` →
  `enum`/`annotation`/`fun` → `companion` → `inline`/`value`/`infix`/`operator` →
  `data`.
- Put annotations on their own line before the declaration (multiple parameterless
  annotations may share a line).
- Never write `: Unit` explicitly; omit the return type for `Unit` functions.

## Coroutines

The canonical example is **`DataProcessor.kt`** — read it first. Key rules:

- **Inject dispatchers; never hardcode them.** Pass a `CoroutineDispatcher` via the
  constructor so tests can substitute a test dispatcher.

  ```kotlin
  // ✅ inject the dispatcher
  class NewsRepository(
      private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
  ) {
      suspend fun loadNews() = withContext(ioDispatcher) { /* ... */ }
  }

  // ❌ hardcoded — untestable
  class NewsRepository {
      suspend fun loadNews() = withContext(Dispatchers.IO) { /* ... */ }
  }
  ```

- **Keep `suspend` functions main-safe.** A `suspend` function should be safe to call
  from any thread; if it does heavy work or I/O, shift context internally with
  `withContext(ioDispatcher)` (as `DataProcessor.fetchIds` does).

- **Use structured concurrency.** Launch child coroutines inside `coroutineScope`/
  `supervisorScope` so they all complete before the parent returns. Return values via
  `async`/`await` instead of mutating shared `var`s.

  ```kotlin
  // ✅ structured: both children finish before getBalance returns
  suspend fun getBalance(): Int = coroutineScope {
      val settled = async(ioDispatcher) { fetchSettled() }
      val pending = async(ioDispatcher) { fetchPending() }
      settled.await() + pending.await()
  }

  // ❌ unstructured: fire-and-forget scopes race the return statement
  suspend fun getBalance(): Int {
      var settled = 0
      CoroutineScope(Dispatchers.IO).launch { settled = fetchSettled() }
      val pending = CoroutineScope(Dispatchers.IO).async { fetchPending() }
      return settled + pending.await() // settled may still be 0
  }
  ```

- **Avoid `GlobalScope`.** Tie coroutines to a scope owned by their logical lifecycle.
  When work must outlive the caller, inject a `CoroutineScope` rather than reaching for
  `GlobalScope`.
- **Cancel scopes, not `Job`s.** Cancelling a `CoroutineScope` cleans up its children;
  cancelling a bare `Job` forces you to recreate it.
- **Check for cancellation** in long CPU-bound loops with `ensureActive()` or
  `yield()`.
- **Exception handling**: prefer `try`/`catch` around the specific suspend calls you
  can recover from; use a `CoroutineExceptionHandler` only for top-level uncaught
  failures. For `Flow`, handle errors with the `catch` operator (see
  `DataProcessor.processDataStream`).

## Quick reference: ❌ avoid → ✅ prefer

| Topic | ❌ Avoid | ✅ Prefer |
| :--- | :--- | :--- |
| Null check | `if (user != null) user.name` | `user?.name ?: "Unknown"` |
| Mutability | `var count = 0` (never reassigned) | `val count = 0` |
| Looping | `for (i in 0..list.size - 1)` | `list.forEach { … }` / `for (x in list)` |
| Strings | `"Hello " + name + "!"` | `"Hello $name!"` |
| Function body | `fun add(a: Int, b: Int): Int { return a + b }` | `fun add(a: Int, b: Int) = a + b` |
| State modeling | `open class State` holding data | `sealed interface State` |
| Object config | `val p = Point(); p.x = 1` | `Point().apply { x = 1 }` |
| Constants | `val maxCount = 10` (top level) | `const val MAX_COUNT = 10` |
| Filtering nulls | `list.filter { it != null }.map { it!! }` | `list.filterNotNull()` |
| Branching | `if (a==1) x else if (a==2) y else z` | `when (a) { 1 -> x; 2 -> y; else -> z }` |
| Dispatcher | `withContext(Dispatchers.IO) { … }` | `withContext(injectedDispatcher) { … }` |
| Coroutine scope | `GlobalScope.launch { … }` | `injectedScope.launch { … }` |

## SOLID

- **SRP** — one reason to change; keep classes/functions small and focused.
- **OCP** — extend behavior via interfaces/abstractions instead of editing existing
  classes.
- **LSP** — subtypes must be substitutable; don't override just to throw
  `NotImplementedError`.
- **ISP** — prefer several small, client-specific interfaces over one fat interface.
- **DIP** — depend on abstractions; inject dependencies through the constructor rather
  than instantiating them inside (mirrors the dispatcher-injection rule above).

## Testing

Mirror `DataProcessorTest.kt` and `ExampleTest.kt`.

- **Runner**: JUnit 5 (Jupiter).
- **Assertions**: the examples use plain JUnit assertions
  (`org.junit.jupiter.api.Assertions.assertEquals` / `assertTrue` / `assertAll` /
  `assertThrows`) — match that. **AssertJ** and **MockK** (and Mockito) are on the
  test classpath (`build.gradle.kts`) and fine to use when they add clarity, but they
  aren't required, and the current examples use neither AssertJ nor mocks. Don't mock
  data classes or simple values — construct them directly.
- **Coroutines**: use `runTest` with an injected `StandardTestDispatcher`, and build
  the subject-under-test with that dispatcher (see `DataProcessorTest.setUp`).
- **Flows**: assert with **Turbine** — `flow.test { awaitItem(); awaitComplete() }`.
- **Parameterized / dynamic**: `@ParameterizedTest` + `@CsvSource`, or `@TestFactory`
  returning `DynamicTest`s (see `ExampleTest`).
- **Structure**: Arrange-Act-Assert in every test.
- **Naming**: descriptive backticked names.
- **Isolation**: tests must not depend on each other or external state.
- **Mutation testing**: run `./gradlew pitest` periodically to confirm tests actually
  catch regressions, not just cover lines.

## Pull requests

Titles follow [Conventional Commits](https://www.conventionalcommits.org/):
`<type>(<scope>): <description>` (`feat`/`fix`/`chore`/`docs`/`test`/`refactor`).
Keep descriptions concise, explain *why* when it isn't obvious, and link issues
(`Closes #123`). See [`CLAUDE.md`](./CLAUDE.md) for the full PR/CI workflow.

## Key files

- `AGENTS.md` (this file) — Kotlin coding conventions.
- `CLAUDE.md` — build, commands, tooling, CI, versions, generated README.
- `build.gradle.kts` / `gradle/libs.versions.toml` — root build config & centralized
  versions; `buildSrc/src/main/kotlin/template.kotlin-library.gradle.kts` — the
  convention plugin shared by every module (`app`, `core`).
- `config/detekt/detekt.yml` — detekt rules (incl. the 120 line limit).
- `.editorconfig` — ktlint/formatter settings.
- `spotless/copyright.kt` — license header template.
- `core/src/main/kotlin/dev/shtanko/template/core/` &
  `core/src/test/kotlin/dev/shtanko/template/core/` — source and tests (the canonical examples).
