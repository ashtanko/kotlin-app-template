---
description: Run the Kotlin unit test suite (optionally filtered to a class or method)
argument-hint: "[TestClass | TestClass.method | fully.qualified.Pattern]"
allowed-tools: Bash(./gradlew test:*), Bash(./gradlew cleanTest test:*), Bash(make test)
---

Run the project's JUnit 5 unit tests with Gradle and report the result.

## How to run

The argument is: `$ARGUMENTS`

- **No argument** (empty): run the full suite with `./gradlew test`.
- **Argument given**: run only the matching tests using Gradle's `--tests` filter.
  Always wrap the value in double quotes (backticked test names contain spaces):
  - Bare class name (e.g. `ExampleTest`) → `./gradlew test --tests "*ExampleTest"`
  - Class + method (e.g. `` ExampleTest.`Divide test` ``) →
    `./gradlew test --tests "*ExampleTest.Divide test"`
  - Fully-qualified pattern already containing the package (e.g.
    `dev.shtanko.template.core.ExampleTest`) → pass it through verbatim to `--tests`.
  - To scope to one module instead of letting `./gradlew test` fan out across both,
    prefix the task: `./gradlew :core:test --tests "..."` or `:app:test`.

If Gradle reports the `test` task as `UP-TO-DATE` and you need a genuine re-run,
re-run as `./gradlew cleanTest test` (optionally with the same `--tests` filter).

## Reporting

- **Success**: state how many tests ran/passed in one line. Don't dump the log.
- **Failure**: show only the failing test name(s) and the key assertion or exception
  from the output — not the whole log. Then open the relevant test/source file,
  explain the likely cause, and **ask before changing anything**. The HTML report is
  at `<module>/build/reports/tests/test/index.html` (e.g. `core/build/reports/tests/test/index.html`).
- **Compilation error**: surface the compiler error itself rather than reporting it
  as a test failure.
- **No matching tests** (`No tests found for given includes`): report that the filter
  matched nothing and suggest the closest test names.

Do not modify any source or test files unless the user explicitly asks you to fix a
failure.
