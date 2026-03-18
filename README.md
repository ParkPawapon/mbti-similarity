# MBTI Similarity Analyzer

This repository contains a Java command-line application for a CSS121 assignment. It finds the students whose MBTI cognitive function scores are most similar to a target student across these eight dimensions:

- `Ne`
- `Ni`
- `Te`
- `Ti`
- `Se`
- `Si`
- `Fe`
- `Fi`

The project is implemented with plain Java 17 and `javac` only. There is no Maven, Gradle, or .NET dependency left in the codebase.

## Instructor Quick Start

If Java 17 is already installed, these commands are enough:

```bash
git clone https://github.com/ParkPawapon/mbti-similarity.git
cd mbti-similarity
./scripts/test.sh
./scripts/run-prod.sh
```

The default production run uses `data/CSS121_MBTI_2026_68_2.csv` and writes the latest Markdown report to `reports/`.

## Dataset

Primary dataset:

- `data/CSS121_MBTI_2026_68_2.csv`

The current dataset has a blank first header cell. The CSV loader handles that case safely by treating the first column as `ID`.

## Features

- `raw` and `zscore` similarity modes
- weighted Euclidean distance
- custom dimension weights with validation
- repeated or comma-separated `--exclude-id`
- data-quality gate with `--max-skipped-ratio`
- console summary output
- Markdown and JSON report export
- plain Java test harness for unit and integration coverage

## Project Structure

- `src/main/java/` main source code
- `src/test/java/` plain Java test suite
- `data/` CSV datasets
- `scripts/build.sh` compile with `javac`
- `scripts/test.sh` compile and run all tests
- `scripts/run-prod.sh` default production run
- `reports/` generated outputs

## Requirements

- Java 17 or newer
- `javac`
- a POSIX shell for the scripts in `scripts/`

## Build

```bash
./scripts/build.sh
```

Manual compile:

```bash
mkdir -p build/classes/main
javac -d build/classes/main $(find src/main/java -name '*.java' | sort)
```

## Test

```bash
./scripts/test.sh
```

## Run

Default run:

```bash
./scripts/run-prod.sh
```

Direct run:

```bash
java -cp build/classes/main com.mbti.similarity.Main \
  --input "./data/CSS121_MBTI_2026_68_2.csv" \
  --target-id 68090500418 \
  --top 5 \
  --max-skipped-ratio 0.2 \
  --exclude-id 99999999999 \
  --mode zscore \
  --output-dir "./reports"
```

Example with custom weights:

```bash
java -cp build/classes/main com.mbti.similarity.Main \
  --input "./data/CSS121_MBTI_2026_68_2.csv" \
  --target-id 68090500418 \
  --mode zscore \
  --weights "Ne=1.2,Ni=1.2,Te=1,Ti=1,Se=0.8,Si=0.8,Fe=1,Fi=1" \
  --output-dir "./reports"
```

## CLI Arguments

- `--input` required path to the CSV file
- `--target-id` required student ID
- `--top` optional, default `5`
- `--output-dir` optional, default `./output`
- `--exclude-id` optional, repeatable or comma-separated
- `--mode` optional, `raw` or `zscore`, default `raw`
- `--weights` optional, format `Ne=...,Ni=...,Te=...,Ti=...,Se=...,Si=...,Fe=...,Fi=...`
- `--max-skipped-ratio` optional, range `[0, 1]`, default `1.0`

## Default Production Configuration

`./scripts/run-prod.sh` uses these defaults:

- input: `data/CSS121_MBTI_2026_68_2.csv`
- target: `68090500418`
- top: `5`
- mode: `zscore`
- excluded IDs: `99999999999`
- max skipped ratio: `0.2`
- output directory: `reports/`

Supported environment variables:

- `INPUT_PATH`
- `TARGET_ID`
- `TOP_N`
- `MODE`
- `WEIGHTS`
- `EXCLUDE_IDS`
- `MAX_SKIPPED_RATIO`
- `OUTPUT_DIR`
- `RUN_TESTS`
- `KEEP_ONLY_LATEST`
- `KEEP_REPORT_FORMAT`

## Current Reference Result

Configuration:

- dataset: `data/CSS121_MBTI_2026_68_2.csv`
- target: `68090500418`
- mode: `zscore`
- excluded IDs: `99999999999`
- weights: equal
- valid rows: `57`
- skipped rows: `0`

Top 5 matches:

1. `68090500427` - Atilak Modetad - `0.5701`
2. `68090500404` - Chanyanuch Thanusorn - `0.5708`
3. `68090500429` - Asnawee Ezor - `0.5896`
4. `68090500421` - Yossakon Rungrattanrarak - `0.5965`
5. `68090500416` - Peerawit Umphaisri - `0.6358`
