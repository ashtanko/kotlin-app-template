#!/usr/bin/env bash
# Claude Code Stop hook: auto-format and lint the Kotlin the agent changed this turn.
# Registered in .claude/settings.json. See CLAUDE.md ("Git hooks self-install").
set -euo pipefail

input="$(cat)"

# Prevent infinite loops: if we are already re-running because a prior invocation
# blocked the stop, allow the stop through this time.
if printf '%s' "$input" | grep -q '"stop_hook_active"[[:space:]]*:[[:space:]]*true'; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

# Fast path: do nothing unless a .kt file actually changed in the working tree,
# so non-Kotlin turns stay instant (no Gradle spin-up).
if ! git status --porcelain 2>/dev/null | grep -qE '\.kt$'; then
  exit 0
fi

# 1) Auto-fix formatting, trailing whitespace and license headers (never blocks).
./gradlew --quiet spotlessApply >/dev/null 2>&1 || true

# 2) Enforce the static-analysis suite. To trim runtime, drop diktatCheck below.
if out="$(./gradlew --quiet detekt ktlintCheck diktatCheck spotlessCheck 2>&1)"; then
  exit 0
else
  echo "Static analysis found issues in the Kotlin you changed — please fix:" >&2
  printf '%s\n' "$out" | tail -n 60 >&2
  exit 2 # exit 2 feeds stderr back to the agent so it fixes the issues and retries.
fi
