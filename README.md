# MBTI Similarity Analyzer

This repository contains a production-style CLI project for a CSS121 Data Science Application assignment.

The goal is simple: given a target student ID, find the Top-N students with the most similar cognitive function profile across these eight dimensions:

- `Ne`
- `Ni`
- `Te`
- `Ti`
- `Se`
- `Si`
- `Fe`
- `Fi`

Rows with incomplete score data are skipped automatically.

## How similarity is calculated

The analyzer uses weighted Euclidean distance:

`distance = sqrt( sum( w_i * (x_i - y_i)^2 ) )`

- `w_i` is the weight of each dimension.
- Weights are always normalized so that `sum(w_i) = 1`.
- Lower distance means higher similarity.

The tool supports two scoring modes:

- `raw`: use original values directly.
- `zscore`: normalize each dimension with z-score before distance calculation.

## Key capabilities

- Exclude special IDs with `--exclude-id` (supports multiple IDs).
- Choose `raw` or `zscore` mode.
- Provide custom weights with strict validation.
- Enforce data quality with `--max-skipped-ratio`.
- Export both Markdown and JSON reports.
- Structured JSON logging with a per-run `runId`.
- Unit and integration tests.
- CI pipeline (restore/build/test).
- Reproducible `.NET` setup via `global.json`.
- Reproducible container runtime via `Dockerfile`.
- One-command production runner: `./scripts/run-prod.sh`.
- Optional R-based visualization/reporting.

## Repository structure

- `data/` - input CSV files
- `src/MbtiEnterpriseSimilarity.App/` - main CLI application
- `tests/MbtiEnterpriseSimilarity.Tests/` - test suite
- `reports/` - generated outputs
- `scripts/run-prod.sh` - one-command production runner
- `analytics/visualize_report.R` - optional R analytics/visualization
- `renv.lock` - locked R environment
- `global.json` - locked .NET SDK
- `.github/workflows/ci.yml` - CI workflow

## Dataset used in this project

- `data/CSS121_MBTI_2026_68.csv`

## Requirements

- .NET SDK from `global.json` (currently `9.0.305`)
- Optional: R (`Rscript`) if you want visualization outputs

## Quick start

```bash
dotnet build
dotnet test

dotnet run --project src/MbtiEnterpriseSimilarity.App -- \
  --input "./data/CSS121_MBTI_2026_68.csv" \
  --target-id 68090500418 \
  --top 5 \
  --max-skipped-ratio 0.2 \
  --exclude-id 99999999999 \
  --mode zscore \
  --output-dir "./reports"
```

## One-command production run

```bash
./scripts/run-prod.sh
```

Example with custom settings:

```bash
EXCLUDE_IDS="99999999999,68090500456" \
MAX_SKIPPED_RATIO=0.2 \
MODE=zscore \
WEIGHTS="Ne=1.2,Ni=1.2,Te=1,Ti=1,Se=0.8,Si=0.8,Fe=1,Fi=1" \
./scripts/run-prod.sh
```

### `run-prod.sh` environment variables

- `INPUT_PATH`
- `TARGET_ID`
- `TOP_N`
- `MODE`
- `WEIGHTS`
- `EXCLUDE_IDS`
- `MAX_SKIPPED_RATIO`
- `OUTPUT_DIR`
- `RUN_TESTS` (`1`/`0`)
- `WITH_R_VIS` (`1`/`0`)
- `KEEP_ONLY_LATEST` (`1`/`0`, default: `1`)
- `KEEP_REPORT_FORMAT` (`md`/`json`/`both`, default: `md`)

Default behavior keeps only the latest Markdown report in `reports/`.
If you want both report files, set `KEEP_REPORT_FORMAT=both`.

## CLI arguments

- `--input` (required): path to the CSV file
- `--target-id` (required): target student ID
- `--top` (optional, default: `5`): number of matches to return
- `--output-dir` (optional, default: `./output`): output directory
- `--exclude-id` (optional): IDs to exclude (repeat flag or comma-separated)
- `--mode` (optional, default: `raw`): `raw` or `zscore`
- `--weights` (optional): `Ne=...,Ni=...,Te=...,Ti=...,Se=...,Si=...,Fe=...,Fi=...`
- `--max-skipped-ratio` (optional, default: `1.0`): allowed skipped-row ratio in `[0, 1]`

## Docker (reproducible runtime)

Build image:

```bash
docker build -t mbti-similarity:latest .
```

Run with defaults:

```bash
docker run --rm -v "$(pwd)/reports:/app/reports" mbti-similarity:latest
```

Run with custom arguments:

```bash
docker run --rm -v "$(pwd)/reports:/app/reports" mbti-similarity:latest \
  --input /app/data/CSS121_MBTI_2026_68.csv \
  --target-id 68090500418 \
  --top 5 \
  --max-skipped-ratio 0.2 \
  --exclude-id 99999999999 \
  --mode zscore \
  --weights "Ne=1.2,Ni=1.2,Te=1,Ti=1,Se=0.8,Si=0.8,Fe=1,Fi=1" \
  --output-dir /app/reports
```

## Optional R visualization

Restore locked R packages first:

```bash
Rscript -e 'renv::restore(prompt = FALSE)'
```

Run R analytics:

```bash
Rscript ./analytics/visualize_report.R \
  --input "./data/CSS121_MBTI_2026_68.csv" \
  --target-id 68090500418 \
  --top 5 \
  --mode zscore \
  --exclude-id 99999999999 \
  --max-skipped-ratio 0.2 \
  --weights "Ne=1,Ni=1,Te=1,Ti=1,Se=1,Si=1,Fe=1,Fi=1" \
  --output-dir "./reports/r"
```

R outputs:

- `top_matches_r.csv`
- `summary_r.txt`
- `top_matches_barplot.png`
- `dimension_diff_heatmap.png`

## Example result (equal weights, z-score mode)

Target: `68090500418`

Top 5 matches:

1. `68090500404` - Chanyanuch Thanusorn - `0.5446`
2. `68090500427` - Atilak Modetad - `0.5472`
3. `68090500429` - Asnawee Ezor - `0.5597`
4. `68090500421` - Yossakon Rungrattanrarak - `0.5602`
5. `68090500416` - Peerawit Umphaisri - `0.5996`

## Reproducibility and quality controls

- SDK lock: `global.json`
- Analyzer rules + warnings as errors: `Directory.Build.props`
- CI checks: `.github/workflows/ci.yml`
- Structured runtime logging in JSON
- Data-quality gate with `--max-skipped-ratio`
