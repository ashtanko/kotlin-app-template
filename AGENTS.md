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

### Formatting
- Use 4 spaces for indentation.
- Follow [Official Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html).
- Keep line length under 120 characters where possible.
- Use `spotlessApply` to fix formatting issues automatically.

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
- Follow the [Official Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html).
- Keep line length under 120 characters where possible.

## Development Conventions

- **Kotlin-First:** 100% Kotlin codebase.
- **Coding Style:** Enforced by `ktlint` and `diktat`. Follow standard Kotlin idioms.
- **License Headers:** All `.kt` files must include the license header defined in `spotless/copyright.kt`. This is enforced/applied by `spotless`.
- **Git Hooks:** Pre-commit hooks are located in `scripts/git-hooks/`. They are automatically installed to `.git/hooks/` during the Gradle `clean` task or when running `make check`. They run static analysis before every commit.
- **Static Analysis Baseline:** A Detekt baseline is maintained at `config/detekt/detekt-baseline.xml` to manage existing issues.

### Code Style
- **Prefer `val` over `var`**: Immutability is preferred. Only use `var` if absolutely necessary.
- **Expression Functions**: Use `fun sum(a: Int, b: Int) = a + b` for simple one-liners.
- **Trailing Commas**: Use trailing commas for better git diffs in multi-line parameter lists.
- **Nullable Types**: Use safe calls `?.` and Elvis operator `?:` instead of the non-null assertion `!!`.
- **Named Arguments**: Use named arguments for functions with many parameters or boolean flags.

### ❌ BAD vs ✅ GOOD Approaches

| Feature | ❌ BAD | ✅ GOOD |
| :--- | :--- | :--- |
| **Null Check** | `if (user != null) { user.name }` | `user?.name ?: "Unknown"` |
| **Variable** | `var count = 0` (if never reassigned) | `val count = 0` |
| **Looping** | `for (i in 0..list.size - 1)` | `for (item in list)` or `list.forEach { ... }` |
| **String Concatenation** | `"Hello " + name + "!"` | `"Hello $name!"` |
| **Function body** | `fun add(a: Int, b: Int): Int { return a + b }` | `fun add(a: Int, b: Int) = a + b` |

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

