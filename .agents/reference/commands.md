# Commands and validation

The full command list, and what each tool does, lives in [`CLAUDE.md`](../../CLAUDE.md) (`Common
commands` section) so it isn't duplicated here. This file is only a selection guide: which check to
run for which kind of change.

## Selecting a check

| Change | Run |
| --- | --- |
| Iterating on one test class/method | `./gradlew test --tests "dev.shtanko.template.ExampleTest"` (or `.method`) |
| Any `.kt` source change | `./gradlew spotlessApply` (auto-fixes formatting/license headers), then `make check` |
| Behavior change | `make test` (= `./gradlew test`) |
| Before committing / finishing a task | `make check && make test` — this is what CI and the pre-commit hook both run |
| Test quality (not just coverage) | `./gradlew pitest`, run deliberately, not part of `make check`/`make test` |
| Docs-only change (`config/main.md`, this `.agents/` tree) | Nothing to build; if `config/main.md` changed, run `make md` to regenerate `README.md` (needs a prior `./gradlew detekt` run — see [`CLAUDE.md`](../../CLAUDE.md)) |

## Notes

- `make check` = `spotlessApply spotlessCheck detekt ktlintCheck diktatCheck`. It auto-formats
  first, then verifies — running it twice in a row should be a no-op on the second run.
- `test --tests` accepts a bare class name, `Class.method`, or a fully-qualified pattern; see
  [`CLAUDE.md`](../../CLAUDE.md) for exact examples. If Gradle reports the `test` task as
  `UP-TO-DATE` and a genuine re-run is needed, use `./gradlew cleanTest test`.
- Never claim a check passed without having run it in this session.
- Git hooks self-install via Gradle (`installGitHooks`, wired to `clean`) and run the same
  `detekt ktlintCheck spotlessCheck spotlessApply` suite at commit time — `make check` locally
  before committing avoids surprises there.
