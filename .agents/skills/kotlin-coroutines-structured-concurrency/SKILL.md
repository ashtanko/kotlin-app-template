---
name: kotlin-coroutines-structured-concurrency
description: Use when writing or reviewing Kotlin code that stores CoroutineScope, launches from init/non-suspending APIs, calls runBlocking, or catches broad exceptions around suspend calls.
---

# Kotlin coroutines: structured concurrency

## Core principle

A well-structured coroutine is a self-contained unit of asynchronous work — single entry, single exit, scoped to a lifecycle known at the call site.

**Scopes should usually be tied to the caller's lifecycle, not stored as a property on the callee.** A stored `CoroutineScope` is a strong review signal: the class must prove it owns cancellation, error reporting, restart behavior, and lifecycle. Most repositories, processors, and data sources cannot prove that, so they should expose `suspend` APIs instead. This project's canonical example is
[`DataProcessor.kt`](../../../core/src/main/kotlin/dev/shtanko/template/core/DataProcessor.kt), which injects a `CoroutineDispatcher` and exposes `suspend`/`Flow` APIs rather than owning a scope.

The fix is almost always the same: **make the API `suspend` and let the caller own the scope.**

## When to use this skill

You're writing or reviewing Kotlin code and you see any of these:

- A class with `private val scope: CoroutineScope` (constructor param stored as a property)
- An `init { scope.launch { ... } }` block
- A non-suspending public function whose body is `scope.launch { ... }`
- `runBlocking { ... }` in suspend-capable application code, or in tests where `runTest` should apply
- `runCatching { suspendCall() }` or a `catch` on `Exception` / `Throwable` around a `suspend` call without rethrowing `CancellationException`
- A `catch (e: CancellationException)` (or equivalent) around suspension that does not rethrow

## The silent-cancellation bug

The reason an unowned `CoroutineScope` property is so dangerous: **once a scope is cancelled, every future `launch` on it silently completes as cancelled — no exception, no log, nothing.** The work just doesn't happen. This is one of the hardest coroutine bugs to diagnose, and it appears when a class holds a long-lived reference to a lifecycle it does not own.

If APIs are `suspend`, this can't happen: the caller's scope is either alive (work runs) or the call site cancels (the caller knows).

## Anti-patterns and fixes

### 1. CoroutineScope stored as a property

```kotlin
// ❌ BAD
class UserRepository(
    private val scope: CoroutineScope,
    private val api: UserApi,
) {
    fun refresh() {
        scope.launch { _state.value = api.fetchUser() }
    }
}

// ✅ GOOD
class UserRepository(
    private val api: UserApi,
) {
    suspend fun refresh(): User = api.fetchUser()
}
```

The repository no longer needs to know about coroutines' lifecycle at all. The caller decides on what scope, with what error handling, with what cancellation semantics.

### 2. init-block launches

```kotlin
// ❌ BAD: construction-time side effect, unbounded work
class UserSession(private val scope: CoroutineScope, private val api: Api) {
    init { scope.launch { _user.value = api.load() } }
}
```

The constructor returns immediately. The caller can't `await` the load, can't see errors, can't cancel. The class is "alive" but its state is undefined.

```kotlin
// ✅ GOOD: explicit bootstrap, caller owns the suspension
class UserSession(private val api: Api) {
    private var _user: User? = null
    val user: User get() = checkNotNull(_user) { "Call init() first" }

    suspend fun init() { _user = api.load() }
}
```

### 3. Fire-and-forget from non-entrypoint classes

A non-suspending public function on a **non-entrypoint class** (repository, processor, manager, data source) that launches into a class-owned scope. The caller gets no result, no error, no cancellation, and no guarantee the work ever ran.

```kotlin
// ❌ BAD — repository with stored scope and fire-and-forget public API
class AnalyticsClient(private val scope: CoroutineScope, private val api: Api) {
    fun track(event: Event) {
        scope.launch { api.send(event) }      // caller has no idea what happens
    }
    fun signOut() {
        scope.launch { api.signOut() }        // silent failure if scope cancelled
    }
}
```

```kotlin
// ✅ GOOD
class AnalyticsClient(private val api: Api) {
    suspend fun track(event: Event) = api.send(event)
    suspend fun signOut() = api.signOut()
}
```

#### Carve-out: the entrypoint boundary

Some call sites genuinely cannot `suspend` — a `main()` function, a synchronous callback from a non-coroutine framework, a scheduled-job trigger. That boundary is where a non-suspending event legitimately gets translated into scoped asynchronous work; it is the *only* layer where that translation belongs.

```kotlin
// ✅ GOOD — the application entrypoint owns a scope and launches structured work
fun main(): Unit = runBlocking {
    coroutineScope {
        launch { processor.processDataStream(sourceFlow).collect(::handle) }
    }
}
```

This is **not** the fire-and-forget anti-pattern. All three conditions must hold:

1. **A genuine non-suspending boundary** — an application entrypoint, a synchronous framework callback, or a scheduled trigger. Not a repository, manager, use case, or data source.
2. **An owned, bounded scope** — `coroutineScope`/`supervisorScope` for structured children, or an explicitly injected `CoroutineScope` with documented cancellation. Not an ad-hoc `CoroutineScope(...)` created and discarded per call.
3. **The caller really is at that boundary** — not another business-logic class calling through it.

The repository / processor / data-source layers underneath still expose `suspend` APIs. The entrypoint is where the non-suspending → suspending translation belongs.

### 4. Stored scopes that aren't injected

The same anti-pattern, without an injected scope:

```kotlin
// ❌ BAD — same problem, scope is constructed in-class instead of injected
class FooManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
}
```

Lifecycle is now owned by nothing and lives forever. Replace with `suspend` APIs.

The same is true if the instantiation is nested inside a function body — `fun foo() { CoroutineScope(...).launch { … } }` is just a stored scope with extra steps. Each call leaks a new uncancellable scope; bundling it into a `by lazy` property doesn't fix the underlying issue (the scope shouldn't exist at all).

### 5. Singletons or initializers that launch on construction

A class launches a coroutine from its constructor, `init` block, or an `initialize()`-style lifecycle hook. The launched work then has:

- **A non-deterministic start time** — whenever something constructs or wires the class. Startup ordering is invisible.
- **No observable lifecycle.** Nothing else in the codebase can see whether it's running or has crashed.
- **No `stop()` / restart path.** If upstream enters a bad state, the loop is uncancellable.
- **No calling code to grep for.** Readers can't find "who starts this and when".

§1 says scopes should be tied to the caller's lifecycle. This variant violates it indirectly: the launch is hidden inside construction rather than at a visible call site.

```kotlin
// ❌ BAD — class boots background work as a side effect of being constructed
class TokenRefresher(
    private val scope: CoroutineScope,
    private val auth: AuthService,
) {
    init {
        scope.launch {
            while (isActive) {
                delay(5.minutes)
                auth.refreshIfNeeded()
            }
        }
    }
}
```

#### First ask: does this background-loop class need to exist at all?

Most background-loop classes exist only because no one inverted the observation. Three answers, in order of preference:

**Pattern 1 — invert into the consumer.** The class observes state forever to react when it changes. But *someone* mutates the state — a sign-out flow, a config-reload handler. That mutation site is already in a coroutine context and is the natural place to do the work directly.

```kotlin
// ✅ GOOD — no background loop, no scope, no class. The mutation site does the work.
class Authenticator(
    private val authStore: AuthStore,
    private val tokenInvalidator: TokenInvalidator,
) {
    suspend fun signOut() {
        authStore.clearTokens()
        tokenInvalidator.invalidate()   // direct call at the mutation site
    }
}
```

The background-loop class is **deleted**. The work happens where the state changes.

When this applies: the consumer of the state has a clear lifecycle (a use case, an Authenticator, a request handler) and can perform the reaction inline.

**Pattern 2 — scheduled work.** Genuinely periodic or deferred work belongs behind a scheduler (a cron trigger, a job queue, an external scheduler invoking the process). The enqueue/entrypoint is one-shot and `suspend`; call it once from the process's startup orchestration.

**Pattern 3 — explicit named launch site.** Sometimes the consumer is a synchronous API with no observable lifecycle. The observation has to live somewhere coroutine-aware, but it must live at an *explicit named call site* — not in the class's own `init`.

```kotlin
// ✅ GOOD — work is named; an explicit call site owns the launch
class ConfigurableSampler(...) {
    @Volatile private var rate: Double = DEFAULT_RATE

    suspend fun observeRate(config: ConfigSource) {
        config.observe(SAMPLING_RATE_KEY).collect { rate = it.coerceIn(0.0, 1.0) }
    }
}

// wired explicitly at the composition root / main():
applicationScope.launch { sampler.observeRate(configSource) }
```

When this applies: the consumer is a synchronous API that calls *into* you with no observable lifecycle. The launch can't be invertible, but it must still be visible at a named call site.

#### Test for which pattern fits

"Is the consumer's lifecycle observable to me?"

- **Yes, and they're already in a coroutine context** → Pattern 1. Push the subscription into them; delete the background-loop class.
- **The work is periodic / deferred** → Pattern 2. Suspend enqueue called once from startup.
- **No, they're a synchronous API with no observable lifecycle** → Pattern 3. Explicit launch site, not `init`.

If a fourth answer seems to fit — e.g., "I want a `Bootable` interface that launches everything for me" — that's the same anti-pattern with an extra layer of abstraction. The whole point is that launches be *visible*; auto-discovery by interface defeats it.

### 6. Swallowing `CancellationException`

A `catch` clause around a `suspend` call that matches `CancellationException` — directly, or through `Exception` / `Throwable` — and doesn't rethrow usually turns cancellation into silent success. The parent coroutine thinks the child finished; the child keeps running (or its side effects do); the cancellation contract is broken.

Same failure shape as §1's stored-scope bug, viewed from the other end: §1 hides the work *from* the caller's lifecycle; this hides cancellation *from* the work.

```kotlin
// ❌ BAD — catches CancellationException, never rethrows
suspend fun fetch() {
    try {
        api.load()
    } catch (e: Exception) {           // matches CancellationException too
        logger.warn("load failed", e)
    }
}

// ❌ ALSO BAD — runCatching has the same problem
suspend fun fetch() {
    runCatching { api.load() }
        .onFailure { logger.warn("load failed", it) }
}
```

The acceptable shapes:

```kotlin
// ✅ Separate catch first
try { api.load() }
catch (e: CancellationException) { throw e }
catch (e: Exception) { logger.warn("load failed", e) }

// ✅ Conditional rethrow inside the broad catch
try { api.load() }
catch (e: Exception) {
    if (e is CancellationException) throw e
    logger.warn("load failed", e)
}

// ✅ ensureActive() — good when the catch handles ordinary failures and you only need
// to rethrow if the current coroutine is cancelled
try { api.load() }
catch (e: Exception) {
    currentCoroutineContext().ensureActive()
    logger.warn("load failed", e)
}

// ✅ runCatching with explicit guard
runCatching { api.load() }
    .onFailure {
        if (it is CancellationException) throw it
        logger.warn("load failed", it)
    }

// ✅ runCatching terminated with getOrThrow (cancellation flows back out)
runCatching { api.load() }.getOrThrow()
```

The trigger is "a suspend call inside the `try`", not "the enclosing function is declared `suspend`". This applies inside any suspending body — `suspend fun`, a `launch { … }` lambda, a Flow `collect { … }`, etc. — including `DataProcessor.processDataStream`'s use of the `catch` operator, which must only handle non-cancellation failures.

The common carve-out is an intentionally local timeout: catching `TimeoutCancellationException` from your own `withTimeout` and converting it to a domain result can be correct. Keep that catch narrow and close to the timeout. Do not use it as permission to swallow arbitrary cancellation.

Catching a non-cancellation subtype (`IOException`, your own exception types) is fine — they don't extend `CancellationException`.

### 7. `runBlocking`

`runBlocking` parks the current thread until the lambda finishes. Inside suspend-capable application paths it is wrong: a thread that meant to be async is now blocked, structured concurrency is broken, and any cancellation upstream has no effect. It is the "callee makes a structural decision for the caller" anti-pattern at its most direct.

```kotlin
// ❌ BAD — bridging to suspend by blocking the calling thread
fun saveUser(user: User) {
    runBlocking { repository.save(user) }
}
```

Fixes, by context:

**Suspend-capable application code** — make the function `suspend`:

```kotlin
// ✅ GOOD
suspend fun saveUser(user: User) = repository.save(user)
```

If the immediate caller can't suspend either, use an owned lifecycle scope at that boundary instead — see §3's entrypoint carve-out. The fix is at the boundary, not inside `saveUser`.

Legitimate blocking boundaries exist: `main` in a CLI tool or JVM application (this project's `Application.kt` entrypoint is exactly this boundary), Java interop APIs that must return synchronously, framework callbacks with no suspending alternative, and migration shims. Keep `runBlocking` at that outer boundary, keep the body small, and call suspending code immediately.

**Tests** — use `runTest` with an injected `StandardTestDispatcher`, per this project's [testing convention](../../reference/testing.md):

```kotlin
// ❌ BAD — real time, slow tests, no virtual delay
@Test fun `loads user`() = runBlocking {
    assertEquals("Alice", repository.load().name)
}

// ✅ GOOD
@Test fun `loads user`() = runTest {
    assertEquals("Alice", repository.load().name)
}
```

`runTest` gives you virtual time (`delay()` returns immediately), `TestDispatcher` integration, and proper coroutine cleanup. Real-time `runBlocking` in tests makes them slow and flaky.

## Quick reference

| Symptom | Anti-pattern | Fix |
|---|---|---|
| Class has `private val scope: CoroutineScope` | Stored scope on the callee | Remove. Make public APIs `suspend`. |
| `init { scope.launch { ... } }` | Construction-time launch | Move to `suspend fun init()` / `login()` |
| `fun foo() { scope.launch { ... } }` on a repository/manager/processor | Fire-and-forget from non-entrypoint class | `suspend fun foo()`, let the entrypoint pick the scope |
| `fun main() = runBlocking { coroutineScope { launch { ... } } }` | Entrypoint boundary — fine | Keep as-is (see §3 carve-out) |
| `private val scope = CoroutineScope(...)` | Internally-constructed stored scope | Same — remove, make APIs `suspend` |
| `class X(scope) { init { scope.launch { … } } }` | Opaque construction-time launch (§5) | Expose `suspend fun run()`, launch from startup orchestration |
| `try { suspendCall() } catch (e: Exception\|Throwable\|CancellationException) { … }` with no rethrow | Swallowed cancellation (§6) | Prefer `catch (e: CancellationException) { throw e }`; use `ensureActive()` only when that matches the intent |
| `runCatching { suspendCall() }.onFailure { … }` with no cancellation guard | Same shape as above (§6) | Add `if (it is CancellationException) throw it`, or terminate with `.getOrThrow()` |
| `runBlocking { … }` inside suspend-capable app code | Thread-blocking bridge (§7) | Make caller `suspend`; or use an owned scope at the boundary |
| `runBlocking { … }` in a test | Same — real-time bridging (§7) | Use `runTest { … }` |

## Refactoring guidance

Removing an existing offender:

1. **Start at the leaf.** Pick the class farthest from the entrypoint — usually a repository or data source. Its public surface should be the easiest to convert.
2. **Convert public functions to `suspend`** one at a time. The compiler will surface every caller.
3. **At each caller, choose the scope deliberately:** `coroutineScope { }`, `supervisorScope { }`, or an explicit job at the entrypoint. This is the choice that was missing before.
4. **Delete the `CoroutineScope` constructor parameter** once nothing uses it.

Don't try to fix every class in one change. Removing an anti-pattern is incremental work.

## When NOT to apply

- **The entrypoint boundary absorbing a non-suspending event.** A `main()` or scheduled trigger with `launch { ... }` is correct — that's the boundary the process needs. See §3 carve-out.
- **Lifecycle owners with explicit cancellation and error policy.** A long-running service/orchestrator class may own a scope when it exposes clear `close`/`cancel`/restart behavior and maps directly to a documented process lifecycle. Inject that scope explicitly rather than creating one ad-hoc. **This is not permission to launch from `init` / `initialize()`** — see §5.
- **Already-suspending APIs** don't need any of this work.
- **Tests** sometimes use `TestScope` as a deliberate ambient scope — that's a different pattern with explicit virtual-time control.

## Red flags during review

These thoughts mean the anti-pattern is back:

| Thought | Reality |
|---|---|
| "I'll just add a `CoroutineExceptionHandler` to the scope" | The problem isn't error handling. The problem is the scope shouldn't exist. |
| "I need to launch from `init` so the data's ready when consumers arrive" | Consumers reading state that isn't ready is the bug. Use phasing. |
| "The caller doesn't want to deal with `suspend`" | Then the caller chooses fire-and-forget at their scope. Don't decide for them. |
| "It's just a small fire-and-forget call" | Silent cancellation makes every fire-and-forget a potential silent failure. |
| "We caught and logged the exception, so we're fine" | Did the catch rethrow `CancellationException`? If no, the coroutine is silently un-cancelled. (§6) |
| "It's just one `runBlocking`, in a non-critical path" | Every `runBlocking` asserts the caller has no async option. If they do, it's the wrong primitive. (§7) |
| "Tests are simpler with `runBlocking`" | They run in real time, can't fast-forward `delay`, and lose `TestDispatcher` semantics. Use `runTest`. (§7) |

## Related

- [`kotlin-flow-state-event-modeling`](../kotlin-flow-state-event-modeling/SKILL.md) — `StateFlow`, `SharedFlow`, `Channel`, `stateIn`, one-shot events, and related modeling.
- [`../../reference/coding-conventions.md`](../../reference/coding-conventions.md) — this project's dispatcher-injection and structured-concurrency conventions.
