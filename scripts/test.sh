#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN_BUILD_DIR="$ROOT_DIR/build/classes/main"
TEST_BUILD_DIR="$ROOT_DIR/build/classes/test"

"$ROOT_DIR/scripts/build.sh"

rm -rf "$TEST_BUILD_DIR"
mkdir -p "$TEST_BUILD_DIR"

TEST_SOURCES=()
while IFS= read -r source_file; do
  TEST_SOURCES+=("$source_file")
done < <(find "$ROOT_DIR/src/test/java" -name '*.java' | sort)

if [[ "${#TEST_SOURCES[@]}" -eq 0 ]]; then
  echo "[ERROR] No Java test files found under src/test/java"
  exit 1
fi

javac -cp "$MAIN_BUILD_DIR" -d "$TEST_BUILD_DIR" "${TEST_SOURCES[@]}"
java -cp "$MAIN_BUILD_DIR:$TEST_BUILD_DIR" com.mbti.similarity.tests.TestMain
