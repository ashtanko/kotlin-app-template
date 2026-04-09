# Kotlin App Template Development Guide for AI Agents

This guide provides comprehensive instructions for AI agents working on the Kotlin App Template codebase. It covers the architecture, development workflows, and critical guidelines for effective contributions, including SOLID principles and Kotlin-specific best practices.

## Project Overview

This project is a comprehensive **Kotlin 2.2 application template** designed for rapid project setup with a strong emphasis on static analysis, testing, and continuous integration. It uses Gradle (Kotlin DSL) and targets JVM 17+.

## Architecture Overview

### Core Components

1. **Source Code (`src/main/kotlin/`)**: Contains the core application logic.
2. **Tests (`src/test/kotlin/`)**: Comprehensive test suite using JUnit 5.
3. **Configuration (`config/`)**: Holds configurations for static analysis tools like Detekt.
4. **Formatting (`spotless/`)**: Contains templates like the copyright header for Spotless.
5. **Git Hooks (`scripts/git-hooks/`)**: Pre-commit hooks to enforce quality before pushing.

### Key Design Principles

- **Kotlin-First**: 100% Kotlin codebase.
- **Aggressive Static Analysis**: Enforced quality via `detekt`, `ktlint`, `diktat`, and `spotless`.
- **High Test Coverage**: Tracked via Jacoco and Kover, targeting >80% coverage.
- **Automation**: Extensive use of Gradle tasks and a `Makefile` for streamlined workflows.

## Development Workflow

### Code Style and Standards

1. **Formatting**: Always apply Spotless to fix formatting and add license headers.
   ```bash
   ./gradlew spotlessApply
   ```
2. **Linting & Analysis**: Run all checks to ensure compliance.
   ```bash
   make check
   # Or individually: ./gradlew detekt ktlintCheck diktatCheck
   ```

### Common Contribution Types

#### 1. Fixing Static Analysis Issues
When Detekt or Ktlint flags an issue, fix it directly.
```kotlin
// Before (Complex function flagged by Detekt)
fun calculate(a: Int, b: Int): Int {
    return a + b
}

// After (Expression body, preferred style)
fun calculate(a: Int, b: Int) = a + b
```

#### 2. Adding Tests
Tests are highly valued. Use JUnit 5 and AssertJ.
```kotlin
@Test
fun `should return sum when two positive integers are added`() {
    // Arrange
    val a = 5
    val b = 10
    
    // Act
    val result = sum(a, b)
    
    // Assert
    assertThat(result).isEqualTo(15)
}
```

#### 3. Updating Dependencies
Dependencies are managed centrally.
```toml
# In gradle/libs.versions.toml
[versions]
kotlin = "2.2.0"
```

### CI Requirements

Before submitting changes, ensure:
1. **Formatting**: `./gradlew spotlessApply` has been run.
2. **Checks Pass**: `make check` executes without errors.
3. **Tests Pass**: `make test` completes successfully.
4. **Documentation**: Code is documented, and `make md` has been run if `README.md` components changed.

## Key Files and Directories

- `build.gradle.kts`: Primary Gradle build configuration.
- `gradle/libs.versions.toml`: Centralized dependency and version management.
- `Makefile`: Task automation shortcuts.
- `config/detekt/detekt.yml`: Detekt rule configurations.
- `config/main.md`: Source for the main part of the README.
- `spotless/copyright.kt`: License header template.
- `scripts/git-hooks/`: Source for git hooks.
- `src/main/kotlin/dev/shtanko/template/`: Root package for source code.
- `src/test/kotlin/dev/shtanko/template/`: Root package for test code.

## Kotlin Best Practices & Guidelines

### General Guidelines
- Use 4 spaces for indentation.
- Follow the [Official Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html) and [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).
- Keep line length under 120 characters where possible.
- **Naming Rules**:
  - Packages are always lowercase and do not use underscores (`org.example.project`).
  - Classes and Objects use PascalCase (`DeclarationProcessor`).
  - Functions and Properties use camelCase (`processDeclarations`, `propertyCount`).
  - Constants (const val or val with deeply immutable data without custom getter) use UPPER_SNAKE_CASE (`MAX_COUNT`).
  - Backing properties use an underscore prefix (`private val _elementList`).
  - Test function names can use spaces enclosed in backticks (``fun `should return true`()``) but are not supported on Android, though permitted in this JVM backend template.

## Development Conventions

- **Kotlin-First:** 100% Kotlin codebase.
- **Coding Style:** Enforced by `ktlint` and `diktat`. Follow standard Kotlin idioms.
- **License Headers:** All `.kt` files must include the license header defined in `spotless/copyright.kt`. This is enforced/applied by `spotless`.
- **Git Hooks:** Pre-commit hooks are located in `scripts/git-hooks/`. They are automatically installed to `.git/hooks/` during the Gradle `clean` task or when running `make check`. They run static analysis before every commit.
- **Static Analysis Baseline:** A Detekt baseline is maintained at `config/detekt/detekt-baseline.xml` to manage existing issues.

### Code Style & Best Practices (2025 Standards)
- **Prefer `val` over `var`**: Immutability is preferred. Only use `var` if absolutely necessary.
- **Use Lazy Initialization**: Use `by lazy { ... }` to defer object creation until it's actually needed, saving memory.
- **Utilize Scope Functions (`let`, `run`, `with`, `apply`, `also`) appropriately**:
  - Use `let` for null checks and executing operations on non-null objects.
  - Use `apply` to configure objects.
  - Avoid deeply nested scope functions to maintain readability.
- **Leverage Coroutines for Concurrency**: Use Kotlin Coroutines and Flows for efficient asynchronous programming instead of callbacks or raw threads.
- **Use Sealed Classes for State Management**: Represent restricted class hierarchies (like UI states: `Loading`, `Success`, `Error`) with `sealed class` or `sealed interface` for safer `when` expressions.
- **Optimize with Inline Functions & Reified Types**: Use `inline` for higher-order functions to prevent lambda object allocations, and `reified` to retain type information at runtime.
- **Use Data Classes for Models**: Leverage `data class` for automatically generated `equals()`, `hashCode()`, `toString()`, and `copy()` methods.
- **Expression Functions**: Use `fun sum(a: Int, b: Int) = a + b` for simple one-liners. When a function returns a single expression, prefer expression body over block body.
- **Trailing Commas**: Use trailing commas for better git diffs in multi-line parameter lists.
- **Nullable Types**: Use safe calls `?.` and Elvis operator `?:` instead of the non-null assertion `!!`.
- **Named Arguments**: Use named arguments for functions with many parameters or boolean flags.

### Coroutines Best Practices
- **Inject Dispatchers**: Never hardcode `Dispatchers.IO`, `Dispatchers.Default`, or `Dispatchers.Main`. Inject them via constructor parameters to make testing deterministic.
- **Suspend Functions should be Main-Safe**: A `suspend` function should always be safe to call from the main thread. If it needs to do heavy work or I/O, it should shift the execution context internally using `withContext(Dispatchers.IO)`.
- **Avoid Cancelling `Job` directly**: Cancel a `CoroutineScope` instead. Cancelling a `Job` means you have to create a new `Job` to start new coroutines.
- **Exception Handling**: Use `CoroutineExceptionHandler` for unhandled exceptions in top-level coroutines (like those launched by `viewModelScope.launch`), but prefer `try/catch` around specific suspend calls where you want to gracefully recover.
- **Use `yield()` or `ensureActive()`**: In long-running CPU-intensive loops inside coroutines, regularly check for cancellation.
- **Use Structured Concurrency**: Use coroutine scopes and structured concurrency to ensure all coroutines finish before their parent scope ends.
- **Use coroutineScope or supervisorScope for short-lived tasks**.
- **Avoid GlobalScope**. Global scope is used to launch top-level coroutines operating on the whole application lifetime and are not canceled prematurely. Over-usage can lead to unpredictable behavior and memory leaks.
- **Inject Dispatchers**.
- **Suspend functions should be Main-Safe**. A `suspend` function should always be safe to call from the main thread. If it needs to do heavy work or I/O, it should shift the execution context.
- **Prefer the Main dispatcher for Root Coroutine**.

✅ **Good**: DO inject an external scope instead of using GlobalScope. GlobalScope can be used indirectly. Here as a default parameter makes sense.

```kotlin
// ✅ DO inject an external scope instead of using GlobalScope.
// GlobalScope can be used indirectly. Here as a default parameter makes sense.
class ArticlesRepository(
    private val articlesDataSource: ArticlesDataSource,
    private val externalScope: CoroutineScope = GlobalScope,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    // As we want to complete bookmarking the article even if the user moves
    // away from the screen, the work is done creating a new coroutine
    // from an external scope
    suspend fun bookmarkArticle(article: Article) {
        externalScope.launch(defaultDispatcher) {
            articlesDataSource.bookmarkArticle(article)
        }
            .join() // Wait for the coroutine to complete
    }
}
```

❌ **Bad**: use GlobalScope directly

```kotlin

// ❌ BAD - use GlobalScope directly
class ArticlesRepository(
    private val articlesDataSource: ArticlesDataSource,
) {
    // As we want to complete bookmarking the article even if the user moves away
    // from the screen, the work is done creating a new coroutine with GlobalScope
    suspend fun bookmarkArticle(article: Article) {
        GlobalScope.launch {
            articlesDataSource.bookmarkArticle(article)
        }
            .join() // Wait for the coroutine to complete
    }
}
```

✅ **Good**: Don’t hardcode Dispatchers when creating new coroutines or calling withContext.

```kotlin
// ✅ DO inject Dispatchers
class NewsRepository(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend fun loadNews() = withContext(defaultDispatcher) { /* ... */ }
}
```

❌ **Bad**:

```kotlin
// ❌ BAD hardcode Dispatchers
class NewsRepository {
    // DO NOT use Dispatchers.Default directly, inject it instead
    suspend fun loadNews() = withContext(Dispatchers.Default) { /* ... */ }
}
```

✅ **Good**: When performing background work and updating the UI within your root coroutine, avoid launching it with a non-Main dispatcher.

```kotlin
// ✅ Good
val scope = CoroutineScope(Dispatchers.Main)

fun login() = scope.launch {
    view.showLoading()
    withContext(Dispatcher.IO) { networkClient.login(...) }
    view.hideLoading()
}
```

❌ **Bad**:

```kotlin
// ❌ BAD
val scope = CoroutineScope(Dispatchers.Default)          // (1)

fun login() = scope.launch {
    withContext(Dispatcher.Main) { view.showLoading() }  // (2)  
    networkClient.login(...)
    withContext(Dispatcher.Main) { view.hideLoading() }  // (2)
}
```

✅ **Good**: Structured concurrency:

```kotlin
// ✅ ensures all child tasks are completed before reaching an end, all tasks are part of the parent's scope
class UserAccount {
    suspend fun getBalance ():Int {
        var count = 0
        var deferred: Deferred<Int>
        coroutineScope {
            launch(Dispatchers.IO) {
                delay(100)
                count = 2000
            }
            deferred = async (Dispatchers.IO){
                delay(300)
                return@async 100
            }
        }
        return count + deferred.await()
    }
}
```

❌ **Bad**: Unstructured Concurrency:

```kotlin

// ❌ BAD - output may be either 100$ or $2100, as there is no guarantee that Task 1 will be completed before reaching the method's last statement i.e. return count + deferred.await()
class UserAccount {
    suspend fun getBalance():Int {
        var count = 0
        // Task 1
        CoroutineScope(Dispatchers.IO).launch {
            count = 2000
        }
        // Task 2
        val deferred = CoroutineScope(Dispatchers.IO).async {
            // mimic api response
            delay(300)
            return@async 100
        }
        return count + deferred.await()
    }
}
```

### Android-Specific Kotlin Guidelines
*(Note: While this template targets JVM, these principles from the Android Kotlin guide are highly relevant for generic modern Kotlin)*
- **Imports**: Group imports properly. Avoid wildcard imports (e.g. `import foo.*`).
- **Annotations**: Place annotations on separate lines before the declaration. Multiple annotations can be placed on the same line if they don't have parameters.
- **Modifiers Order**: Always follow the standard Kotlin modifier order (`public` / `protected` / `private` / `internal` -> `expect` / `actual` -> `final` / `open` / `abstract` / `sealed` / `const` -> `external` / `override` / `lateinit` / `tailrec` -> `vararg` / `suspend` / `inner` / `enum` / `annotation` / `fun` -> `companion` / `inline` / `value` / `infix` / `operator` -> `data`).
- **Conditionals**: Avoid using deeply nested `if`/`else`. For multi-branch conditions, strongly prefer `when`.
- **Type arguments**: Only omit type arguments if they can be easily inferred by the compiler.
- **Control Flow as Expressions**: Leverage `if`, `when`, and `try/catch` as expressions assigning their result directly to a variable rather than using them purely as statements.
- **Use `viewModelScope` / `lifecycleScope` / `coroutineScope` properly**: Don't use `GlobalScope` as it can lead to memory leaks and unmanageable jobs. Tie coroutine lifecycles to their logical owner.

### ❌ BAD vs ✅ GOOD Approaches

| Feature | ❌ BAD | ✅ GOOD |
| :--- | :--- | :--- |
| **Null Check** | `if (user != null) { user.name }` | `user?.name ?: "Unknown"` |
| **Variable** | `var count = 0` (if never reassigned) | `val count = 0` |
| **Looping** | `for (i in 0..list.size - 1)` | `for (item in list)` or `list.forEach { ... }` |
| **String Concatenation** | `"Hello " + name + "!"` | `"Hello $name!"` |
| **Function body** | `fun add(a: Int, b: Int): Int { return a + b }` | `fun add(a: Int, b: Int) = a + b` |
| **State Handling** | `open class State` / `enum class State` (if holding data) | `sealed class` / `sealed interface State` |
| **Object Config** | `val paint = Paint(); paint.color = RED` | `val paint = Paint().apply { color = RED }` |
| **Constants** | `val maxCount = 10` (at top level) | `const val MAX_COUNT = 10` |
| **Filtering** | `list.filter { it != null }.map { it!! }` | `list.filterNotNull()` |
| **Branching** | `if (a == 1) { x } else if (a == 2) { y } else { z }` | `when (a) { 1 -> x; 2 -> y; else -> z }` |
| **Coroutine Dispatcher** | `withContext(Dispatchers.IO) { ... }` | `withContext(injectedDispatcher) { ... }` |
| **Scope Usage** | `GlobalScope.launch { ... }` | `viewModelScope.launch { ... }` |

### Performance Considerations
- **Inline Functions**: Use `inline` for functions taking lambda arguments to reduce overhead.
- **Lazy Initialization**: Use `by lazy { ... }` for heavy objects.
- **Sequences**: Use `Sequence` (`asSequence()`) instead of `Iterable` for large collections with multiple chained transformations to avoid intermediate collection creation.
- **Avoid Unnecessary Allocations**: Be mindful of creating objects in tight loops.

### Common Pitfalls & What to Avoid
- **Avoid `!!`**: The "Not-null assertion operator" leads to `NullPointerException`. Never use it.
- **Avoid Platform Types**: When interoperating with Java, explicitly specify nullability to avoid runtime crashes.
- **Don't Overuse `let`**: Use `let` for scoping or null-checks, but don't use it to replace every simple `if`.
- **Avoid Deep Nesting**: Use early returns or `when` expressions to keep code flat.
- **Omit Unit return types**: Never declare `: Unit` explicitly on functions.
- **Avoid Wildcard Imports**: E.g., `import java.util.*`. They pollute the namespace and can cause compilation errors later when new classes are introduced into the wildcard package.

## Software Development Best Practices (SOLID)

1. **Single Responsibility Principle (SRP)**: A class or function should have one, and only one, reason to change. Keep classes small and focused.
2. **Open/Closed Principle (OCP)**: Software entities should be open for extension, but closed for modification. Use interfaces and abstract classes to allow behavior extension without modifying existing code.
3. **Liskov Substitution Principle (LSP)**: Objects in a program should be replaceable with instances of their subtypes without altering the correctness of that program. Avoid overriding methods just to throw `NotImplementedError`.
4. **Interface Segregation Principle (ISP)**: Many client-specific interfaces are better than one general-purpose interface. Don't force classes to implement methods they don't use.
5. **Dependency Inversion Principle (DIP)**: Depend upon abstractions, not concretions. Use dependency injection (via constructor parameters) rather than instantiating dependencies directly inside a class.

## Testing Best Practices

- **Frameworks**: Use JUnit 5 as the primary test runner.
- **Naming Conventions**: Use descriptive names, often enclosed in backticks, e.g., ``fun `should return true when input is valid`()``.
- **Structure**: Follow the Arrange-Act-Assert (AAA) pattern explicitly or implicitly in every test.
- **Mocks**: Use `MockK` for idiomatic Kotlin mocking. Avoid mocking data classes or simple values; construct them directly.
- **Assertions**: Use `AssertJ` for fluent, readable assertions (e.g., `assertThat(result).isTrue()`).
- **Mutation Testing**: Run `pitest` periodically (`./gradlew pitest`) to ensure your tests actually catch bugs, not just cover lines.
- **Isolation**: Tests should not depend on each other or on external state (unless explicitly an integration test).

## Opening PRs

### Titles
Use [Conventional Commits](https://www.conventionalcommits.org/):
```
<type>(<scope>): <short description>
```
Types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`.

### Descriptions
- Keep it concise.
- Explain *why* the change was made if it's not obvious.
- Link related issues (e.g., `Closes #123`).

## Quick Reference

### Essential Commands

```bash
# Run all checks (format, lint, analysis)
make check

# Apply code formatting automatically
./gradlew spotlessApply

# Run test suite
make test

# Generate coverage report
make report

# Run mutation testing
./gradlew pitest

# Build project and update README
make all
```

### Makefile Shortcuts

- `make check`: Runs all static analysis tools (`spotless`, `detekt`, `ktlint`, `diktat`).
- `make test`: Runs tests.
- `make report`: Generates Jacoco coverage report.
- `make md`: Regenerates `README.md` by concatenating `config/main.md`, `build/reports/detekt/detekt.md`, and `config/license.md`.
- `make all`: Runs checks, builds the project, and updates `README.md`.

## Building and Running

The project uses Gradle. A `Makefile` is also provided for convenience.

### Key Gradle Commands

- `./gradlew build`: Compiles the project and runs tests.
- `./gradlew test`: Executes the test suite.
- `./gradlew run`: Runs the application (main class: `link.kotlin.scripts.Application`).
- `./gradlew detekt`: Runs static analysis with Detekt.
- `./gradlew ktlintCheck`: Checks code style with ktlint.
- `./gradlew diktatCheck`: Checks code style with diktat.
- `./gradlew spotlessApply`: Automatically formats code and applies license headers.
- `./gradlew jacocoTestReport`: Generates a Jacoco coverage report.
- `./gradlew koverHtmlReport`: Generates a Kover HTML coverage report.
- `./gradlew dokkaHtml`: Generates HTML documentation.
- `./gradlew pitest`: Runs mutation tests.
