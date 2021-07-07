#!/bin/bash
RESULT_FILE=$1

if [ -f "$RESULT_FILE" ]; then
  rm "$RESULT_FILE"
fi
touch "$RESULT_FILE"

checksum_file() {
  # shellcheck disable=SC2005
  # shellcheck disable=SC2046
  echo $(openssl md5 "$1" | awk '{print $2}')
}

FILES=()

# Read Gradle related files
while read -r -d ''; do
	FILES+=("$REPLY")
done < <(find . -type f \( -name "build.gradle.kts" -o -name "gradle-wrapper.properties" \) -print0)

# Read dependencies related files
while read -r -d ''; do
	FILES+=("$REPLY")
done < <(find buildSrc/src/main/kotlin -type f \( -name "*.kt" \) -print0)

# Loop through files and append MD5 to result file
# shellcheck disable=SC2068
for FILE in ${FILES[@]}; do
	# shellcheck disable=SC2046
	# shellcheck disable=SC2005
	echo $(checksum_file "$FILE") >> "$RESULT_FILE"
done
# Now sort the file so that it is
sort "$RESULT_FILE" -o "$RESULT_FILE"