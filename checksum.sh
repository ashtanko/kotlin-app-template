#!/bin/bash
set -euo pipefail

RESULT_FILE="${1:-/tmp/checksum.txt}"

if [ -f "$RESULT_FILE" ]; then
  rm "$RESULT_FILE"
fi
touch "$RESULT_FILE"

checksum_file() {
  if command -v md5sum >/dev/null 2>&1; then
    md5sum "$1" | awk '{print $1}'
  else
    openssl md5 "$1" | awk '{print $2}'
  fi
}

FILES=()

# Read Gradle related files excluding build and hidden cache dirs
while IFS= read -r -d '' file; do
  FILES+=("$file")
done < <(find . \
  \( -path '*/build/*' -o -path '*/.gradle/*' -o -path '*/.git/*' -o -path '*/.idea/*' \) -prune \
  -o -type f \( -name "*.gradle.kts" -o -name "gradle-wrapper.properties" -o -name "*.versions.toml" -o -name "gradle.properties" \) -print0)

# Read buildSrc source files
while IFS= read -r -d '' file; do
  FILES+=("$file")
done < <(find buildSrc/src/main/kotlin -type f -name "*.kt" -print0 2>/dev/null || true)

for FILE in "${FILES[@]}"; do
  [ -f "$FILE" ] && echo "$(checksum_file "$FILE")  $FILE" >> "$RESULT_FILE"
done

sort "$RESULT_FILE" -o "$RESULT_FILE"