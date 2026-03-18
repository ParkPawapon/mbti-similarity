#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MAIN_BUILD_DIR="$ROOT_DIR/build/classes/main"

INPUT_PATH="${INPUT_PATH:-$ROOT_DIR/data/CSS121_MBTI_2026_68_2.csv}"
TARGET_ID="${TARGET_ID:-68090500418}"
TOP_N="${TOP_N:-5}"
MODE="${MODE:-zscore}"
MAX_SKIPPED_RATIO="${MAX_SKIPPED_RATIO:-0.2}"
EXCLUDE_IDS="${EXCLUDE_IDS:-99999999999}"
OUTPUT_DIR="${OUTPUT_DIR:-$ROOT_DIR/reports}"
WEIGHTS="${WEIGHTS:-}"
RUN_TESTS="${RUN_TESTS:-1}"
KEEP_ONLY_LATEST="${KEEP_ONLY_LATEST:-1}"
KEEP_REPORT_FORMAT="${KEEP_REPORT_FORMAT:-md}" # md | json | both

if [[ ! -f "$INPUT_PATH" ]]; then
  echo "[ERROR] Input CSV not found at $INPUT_PATH"
  exit 1
fi

if [[ "$RUN_TESTS" == "1" ]]; then
  echo "[INFO] Running build + tests..."
  "$ROOT_DIR/scripts/test.sh"
else
  echo "[INFO] Running build..."
  "$ROOT_DIR/scripts/build.sh"
fi

mkdir -p "$OUTPUT_DIR"

CMD=(
  java -cp "$MAIN_BUILD_DIR" com.mbti.similarity.Main
  --input "$INPUT_PATH"
  --target-id "$TARGET_ID"
  --top "$TOP_N"
  --max-skipped-ratio "$MAX_SKIPPED_RATIO"
  --mode "$MODE"
  --output-dir "$OUTPUT_DIR"
  --exclude-id "$EXCLUDE_IDS"
)

if [[ -n "$WEIGHTS" ]]; then
  CMD+=(--weights "$WEIGHTS")
fi

if [[ "$#" -gt 0 ]]; then
  CMD+=("$@")
fi

echo "[INFO] Running production analysis..."
"${CMD[@]}"

cleanup_reports_keep_latest() {
  local report_dir="$1"
  local keep_format="$2"
  local latest_md
  local latest_json
  local latest_base
  local file

  latest_md="$(ls -1t "$report_dir"/similarity_*.md 2>/dev/null | head -n 1 || true)"
  latest_json="$(ls -1t "$report_dir"/similarity_*.json 2>/dev/null | head -n 1 || true)"

  case "$keep_format" in
    md|json|both) ;;
    *)
      echo "[ERROR] KEEP_REPORT_FORMAT must be one of: md, json, both"
      exit 1
      ;;
  esac

  if [[ -n "$latest_md" ]]; then
    latest_base="${latest_md%.md}"
  elif [[ -n "$latest_json" ]]; then
    latest_base="${latest_json%.json}"
  else
    return 0
  fi

  for file in "$report_dir"/similarity_*.md "$report_dir"/similarity_*.json; do
    [[ -e "$file" ]] || continue
    case "$keep_format" in
      md)
        [[ "$file" == "$latest_base.md" ]] || rm -f "$file"
        ;;
      json)
        [[ "$file" == "$latest_base.json" ]] || rm -f "$file"
        ;;
      both)
        if [[ "$file" != "$latest_base.md" && "$file" != "$latest_base.json" ]]; then
          rm -f "$file"
        fi
        ;;
    esac
  done

  echo "[INFO] Keeping latest reports only (format=$keep_format):"
  if [[ "$keep_format" == "md" || "$keep_format" == "both" ]]; then
    [[ -f "$latest_base.md" ]] && echo "       - $latest_base.md"
  fi
  if [[ "$keep_format" == "json" || "$keep_format" == "both" ]]; then
    [[ -f "$latest_base.json" ]] && echo "       - $latest_base.json"
  fi
}

if [[ "$KEEP_ONLY_LATEST" == "1" ]]; then
  cleanup_reports_keep_latest "$OUTPUT_DIR" "$KEEP_REPORT_FORMAT"
fi

echo "[INFO] Done. Reports are in: $OUTPUT_DIR"
