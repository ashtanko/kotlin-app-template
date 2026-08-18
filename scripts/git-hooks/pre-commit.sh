#!/bin/sh
set -e

echo "Running static analysis..."

# 1) Auto-fix formatting, trailing whitespace and license headers
./gradlew spotlessApply --profile --daemon

# 2) Stage any files modified by spotlessApply
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git add -u
fi

# 3) Run full verification suite
if ./gradlew detekt ktlintCheck diktatCheck spotlessCheck --profile --daemon; then
    echo "*********************************************"
    echo "      Static analysis no problems found      "
    echo "*********************************************"
    exit 0
else
    echo "*********************************************"
    echo "            Static Analysis Failed           "
    echo "Please fix the above issues before committing"
    echo "*********************************************"
    exit 1
fi

