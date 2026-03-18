#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/classes/main"

rm -rf "$ROOT_DIR/build/classes/main"
mkdir -p "$BUILD_DIR"

MAIN_SOURCES=()
while IFS= read -r source_file; do
  MAIN_SOURCES+=("$source_file")
done < <(find "$ROOT_DIR/src/main/java" -name '*.java' | sort)

if [[ "${#MAIN_SOURCES[@]}" -eq 0 ]]; then
  echo "[ERROR] No Java source files found under src/main/java"
  exit 1
fi

javac -d "$BUILD_DIR" "${MAIN_SOURCES[@]}"
echo "[INFO] Compiled ${#MAIN_SOURCES[@]} main source files into $BUILD_DIR"
