<h1 align="center">Kotlin App Template</h1></br>

<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://github.com/ashtanko/kotlin-app-template/actions/workflows/ci.yml"><img alt="Build Status" src="https://github.com/ashtanko/kotlin-app-template/actions/workflows/ci.yml/badge.svg"/></a>
  <a href="https://sonarcloud.io/dashboard?id=ashtanko_kotlin-app-template"><img alt="Quality Gate Status" src="https://sonarcloud.io/api/project_badges/measure?project=ashtanko_kotlin-app-template&metric=alert_status"/></a>
  <a href="https://sonarcloud.io/dashboard?id=ashtanko_kotlin-app-template"><img alt="Lines of Code" src="https://sonarcloud.io/api/project_badges/measure?project=ashtanko_kotlin-app-template&metric=ncloc"/></a>
  <a href="https://sonarcloud.io/dashboard?id=ashtanko_kotlin-app-template"><img alt="Coverage" src="https://sonarcloud.io/api/project_badges/measure?project=ashtanko_kotlin-app-template&metric=coverage"/></a>
  <a href="https://codecov.io/gh/ashtanko/kotlin-app-template"><img alt="Codecov" src="https://codecov.io/gh/ashtanko/kotlin-app-template/graph/badge.svg?token=FQFL6U7YL0"/></a>
  <a href="https://www.codefactor.io/repository/github/ashtanko/kotlin-app-template"><img alt="CodeFactor" src="https://www.codefactor.io/repository/github/ashtanko/kotlin-app-template/badge"/></a>
  <a href="https://app.codacy.com/gh/ashtanko/kotlin-app-template/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade"><img alt="Codacy Badge" src="https://app.codacy.com/project/badge/Grade/4935d531e41241faa0ce25eeddb67533"/></a>
  <a href="https://app.codacy.com/gh/ashtanko/kotlin-app-template/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage"><img alt="Codacy Badge" src="https://app.codacy.com/project/badge/Coverage/4935d531e41241faa0ce25eeddb67533"/></a>
</p><br>

## Overview

A GitHub template for bootstrapping **Kotlin** projects with **static analysis**, **testing**, and **continuous integration** preconfigured and ready to go. Use this template to create a new Kotlin/JVM project and be up and running in seconds.

## Features 🦄

- **100% Kotlin-only template**.
- Kotlin 2.4 with K2 Compiler.
- JVM 17+ target.
- 100% Gradle Kotlin DSL setup (Gradle 9.5).
- CI setup with GitHub Actions (builds on JDK 17 and 21).
- Aggressive Kotlin static analysis via `detekt`, `ktlint`, `diktat`, and `spotless`.
- Test suite with JUnit 5, AssertJ, MockK, Mockito, and Turbine.
- Code coverage via Jacoco and Kover (≥ 80% enforced).
- Mutation testing via Pitest.
- API documentation via Dokka.
- Pre-commit Git hooks for automated quality checks.
- Project rename script for quick customization.
- GitHub Issues templates (bug report + feature request).

## Requirements

| Tool   | Version |
|--------|---------|
| JDK    | 17+     |
| Gradle | 9.5 (included via wrapper) |

No additional installation is required — the Gradle wrapper (`./gradlew`) is included in the repository.

## Getting Started

### 1. Create a new repository from this template

Click **"Use this template"** on GitHub, or clone the repository directly:

```bash
git clone https://github.com/ashtanko/kotlin-app-template.git
cd kotlin-app-template
```

### 2. Rename the project (optional)

A rename script is provided to update the project name, package, and GitHub owner in one step:

```bash
./scripts/rename-project.sh -n "my-project" -p "com.example.myproject"
```

Run with `--help` for all available options, or `--dry-run` to preview changes.

### 3. Build the project

```bash
./gradlew build
```

### 4. Run the application

```bash
./gradlew run
```

> **Note:** The default main class is `link.kotlin.scripts.Application` (configured in `build.gradle.kts`). Update this to your own entry point after scaffolding.

## Scripts & Commands

### Makefile Shortcuts

| Command            | Description                                                             |
|--------------------|-------------------------------------------------------------------------|
| `make check`       | Run all static analysis (`spotless`, `detekt`, `ktlint`, `diktat`)      |
| `make test`        | Run the test suite                                                      |
| `make report`      | Generate Jacoco coverage report                                         |
| `make kover`       | Generate Kover HTML coverage report                                     |
| `make detekt`      | Run Detekt analysis only                                                |
| `make diktat`      | Run Diktat check only                                                   |
| `make md`          | Regenerate `README.md` from `config/main.md` + detekt report + license  |
| `make all`         | Run checks, build, and regenerate README                                |
| `make lines`       | Count lines of Kotlin code                                              |
| `make bump-gradle` | Upgrade the Gradle wrapper version                                      |

### Gradle Tasks

| Command                        | Description                                    |
|--------------------------------|------------------------------------------------|
| `./gradlew build`              | Compile and run tests                          |
| `./gradlew test`               | Run the test suite                             |
| `./gradlew run`                | Run the application                            |
| `./gradlew detekt`             | Run Detekt static analysis                     |
| `./gradlew ktlintCheck`        | Check code style with ktlint                   |
| `./gradlew diktatCheck`        | Check code style with Diktat                   |
| `./gradlew spotlessApply`      | Auto-format code and apply license headers     |
| `./gradlew jacocoTestReport`   | Generate Jacoco coverage report                |
| `./gradlew koverHtmlReport`    | Generate Kover HTML coverage report            |
| `./gradlew koverXmlReport`     | Generate Kover XML coverage report             |
| `./gradlew dokkaHtml`          | Generate HTML API documentation                |
| `./gradlew pitest`             | Run mutation tests                             |

## Environment Variables

| Variable         | Description                          | Default                        |
|------------------|--------------------------------------|--------------------------------|
| `PITEST_THREADS` | Number of threads for mutation tests | Half of available CPU cores    |

<!-- TODO: Add additional environment variables here if needed (e.g., API keys, secrets) -->

## Testing

Tests are located in `src/test/kotlin/` and use:

- **JUnit 5** — test runner and parameterized tests
- **AssertJ** — fluent assertions
- **MockK** — Kotlin-idiomatic mocking
- **Mockito** — additional mocking support
- **Turbine** — Kotlin Flow testing

Run all tests:

```bash
./gradlew test
# or
make test
```

Generate coverage reports:

```bash
./gradlew jacocoTestReport   # Jacoco (HTML + XML + CSV)
./gradlew koverHtmlReport    # Kover
```

Run mutation testing:

```bash
./gradlew pitest
```

Coverage is enforced at ≥ 80% via Kover and ≥ 50% via Jacoco verification.

## Project Structure

```
kotlin-app-template/
├── build.gradle.kts                # Main Gradle build configuration
├── settings.gradle.kts             # Gradle settings (project name, toolchain resolver)
├── gradle.properties               # Gradle and Kotlin build properties
├── gradle/
│   └── libs.versions.toml          # Centralized dependency and version catalog
├── Makefile                        # Task automation shortcuts
├── config/
│   ├── main.md                     # Source for the main README section
│   ├── license.md                  # License section appended to README
│   └── detekt/
│       ├── detekt.yml              # Detekt rule configuration
│       └── detekt-baseline.xml     # Detekt baseline for existing issues
├── spotless/
│   └── copyright.kt               # License header template for Spotless
├── scripts/
│   ├── git-hooks/
│   │   └── pre-commit.sh           # Pre-commit hook (static analysis)
│   └── rename-project.sh           # Project rename utility
├── src/
│   ├── main/kotlin/dev/shtanko/template/
│   │   ├── Calculator.kt           # Example calculator class
│   │   ├── DataProcessor.kt        # Example data processor with coroutines/Flow
│   │   └── DivideByZeroException.kt
│   └── test/kotlin/dev/shtanko/template/
│       ├── ExampleTest.kt           # Example calculator tests
│       └── DataProcessorTest.kt     # Data processor tests
├── .github/
│   └── workflows/
│       └── ci.yml                   # GitHub Actions CI pipeline
├── codecov.yml                      # Codecov configuration
├── renovate.json                    # Renovate bot configuration for dependency updates
├── diktat-analysis.yml              # Diktat analysis configuration
├── checksum.sh                      # Checksum verification script
└── AGENTS.md                        # AI agent guidelines
```

## CI/CD

The project includes a GitHub Actions workflow (`.github/workflows/ci.yml`) that runs on every push to `main` and on pull requests:

1. **Build & Test** — compiles and runs tests on JDK 17 and JDK 21.
2. **Static Analysis** — runs `detekt`, `ktlint`, and `diktat`.
3. **Coverage Reporting** — generates Jacoco and Kover reports, uploads to Codecov and Codacy.
4. **Code Quality** — runs Codacy Analysis CLI.

## Contributing 🤝

Feel free to open an issue or submit a pull request for any bugs/improvements.

Use [Conventional Commits](https://www.conventionalcommits.org/) for PR titles:

```
<type>(<scope>): <short description>
```

Types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`.

