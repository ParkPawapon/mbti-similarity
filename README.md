# MBTI Similarity Analyzer

โปรเจกต์นี้ใช้สำหรับงาน CSS121 Data Science Application เพื่อหา
`Top-N คนที่คะแนน Ne, Ni, Te, Ti, Se, Si, Fe, Fi ใกล้กับรหัสเป้าหมายมากที่สุด`
โดยรองรับกรณีข้อมูลยังไม่ครบ (แถวที่คะแนนไม่ครบจะถูกข้ามอัตโนมัติ)

## จุดเด่นเวอร์ชันนี้ (Production-ready)

- รองรับ `--exclude-id` เพื่อกันแถวพิเศษ (เช่นอาจารย์/test account)
- รองรับโหมดคำนวณ 2 แบบ
  - `--mode raw` : ใช้คะแนนดิบ
  - `--mode zscore` : normalize (z-score) ก่อนคำนวณ
- รองรับ `--weights` สำหรับปรับน้ำหนักแต่ละมิติแบบปลอดภัย (validate + normalize อัตโนมัติ)
- รองรับ `--max-skipped-ratio` เป็น data quality gate (fail ทันทีเมื่อข้อมูลเสียเกิน threshold)
- Export รายงานทั้ง `.md` และ `.json` พร้อม metadata ของโหมด/น้ำหนักที่ใช้จริง
- มี structured logging (JSON) พร้อม `runId`
- มี Unit + Integration tests ครอบคลุม parsing, distance logic, exclusion, weighting, data quality gate
- มี CI workflow สำหรับ restore/build/test
- ล็อก .NET SDK ด้วย `global.json` เพื่อรันเวอร์ชันเดียวกันทุกเครื่อง
- มี `Dockerfile` สำหรับ reproducible container runtime
- มี one-command script `./scripts/run-prod.sh` ลด human error
- มี R script เสริมสำหรับ visualization/report เชิงสถิติ

## สูตรคำนวณที่ใช้

ใช้ **Weighted Euclidean Distance**:

`distance = sqrt( Σ( w_i * (x_i - y_i)^2 ) )`

โดย `w_i` คือ weight ของแต่ละมิติ (`Ne, Ni, Te, Ti, Se, Si, Fe, Fi`) และระบบจะ normalize ให้ `Σw_i = 1` เสมอ

## โครงสร้างโปรเจกต์

- `data/` : ไฟล์ข้อมูล CSV
- `src/MbtiEnterpriseSimilarity.App` : แอปหลัก (CLI)
- `tests/MbtiEnterpriseSimilarity.Tests` : ชุด Unit Tests
- `reports/` : ไฟล์ผลลัพธ์ที่ generate จากการรัน
- `scripts/run-prod.sh` : คำสั่งเดียวสำหรับ production run
- `analytics/visualize_report.R` : สคริปต์ R สำหรับ visualization/report เสริม
- `renv.lock` : ล็อก R environment สำหรับ reproducibility
- `global.json` : ล็อก SDK
- `Dockerfile` : container build/runtime

## Dataset ในโปรเจกต์

- `data/CSS121_MBTI_2026_68.csv`

## Reproducible Environment

- SDK lock: `global.json` (กำหนด `9.0.305`)
- Build policy: `Directory.Build.props` (analyzers + warning as error)
- CI pipeline: `.github/workflows/ci.yml`

## วิธีรัน

```bash
dotnet build
dotnet test

# โหมด normalize z-score + exclude แถวพิเศษ + น้ำหนัก default (equal)
dotnet run --project src/MbtiEnterpriseSimilarity.App -- \
  --input "./data/CSS121_MBTI_2026_68.csv" \
  --target-id 68090500418 \
  --top 5 \
  --max-skipped-ratio 0.2 \
  --exclude-id 99999999999 \
  --mode zscore \
  --output-dir "./reports"
```

## One-Command Production Run

```bash
./scripts/run-prod.sh
```

ตัวอย่าง custom config:

```bash
EXCLUDE_IDS="99999999999,68090500456" \
MAX_SKIPPED_RATIO=0.2 \
MODE=zscore \
WEIGHTS="Ne=1.2,Ni=1.2,Te=1,Ti=1,Se=0.8,Si=0.8,Fe=1,Fi=1" \
./scripts/run-prod.sh
```

ตัวแปรที่ script รองรับ:
- `INPUT_PATH`, `TARGET_ID`, `TOP_N`
- `MODE`, `WEIGHTS`
- `EXCLUDE_IDS`
- `MAX_SKIPPED_RATIO`
- `OUTPUT_DIR`
- `RUN_TESTS` (`1`/`0`)
- `WITH_R_VIS` (`1`/`0`)
- `KEEP_ONLY_LATEST` (`1`/`0`, default=`1`)
- `KEEP_REPORT_FORMAT` (`md`/`json`/`both`, default=`md`)

หมายเหตุ:
- ค่า default ของ `KEEP_ONLY_LATEST=1` และ `KEEP_REPORT_FORMAT=md` จะลบรายงานเก่าใน `reports/` และเก็บไว้เฉพาะรายงาน `.md` ล่าสุดไฟล์เดียว
- ถ้าต้องการเก็บทั้งคู่ (`.md` + `.json`) ให้ตั้ง `KEEP_REPORT_FORMAT=both`

ตัวอย่าง custom weights:

```bash
dotnet run --project src/MbtiEnterpriseSimilarity.App -- \
  --input "./data/CSS121_MBTI_2026_68.csv" \
  --target-id 68090500418 \
  --exclude-id 99999999999 \
  --mode zscore \
  --weights "Ne=1.2,Ni=1.2,Te=1,Ti=1,Se=0.8,Si=0.8,Fe=1,Fi=1" \
  --output-dir "./reports"
```

## Docker (Reproducible Runtime)

Build image:

```bash
docker build -t mbti-similarity:latest .
```

Run with defaults:

```bash
docker run --rm -v "$(pwd)/reports:/app/reports" mbti-similarity:latest
```

Run with custom args:

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

## R Visualization (Supplementary)

R script เป็นส่วนเสริมด้าน visualization/report โดยไม่กระทบ production pipeline หลัก C#.

Prerequisite:
- ต้องมี `Rscript` ในเครื่อง
- แนะนำ restore R environment จาก lockfile ก่อนรัน:

```bash
Rscript -e 'renv::restore(prompt = FALSE)'
```

Run:

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

ไฟล์ output จาก R:
- `top_matches_r.csv`
- `summary_r.txt`
- `top_matches_barplot.png`
- `dimension_diff_heatmap.png`

## พารามิเตอร์ CLI

- `--input` path ไฟล์ CSV (จำเป็น)
- `--target-id` รหัสนักศึกษาเป้าหมาย (จำเป็น)
- `--top` จำนวนอันดับที่ต้องการ (default = 5)
- `--output-dir` โฟลเดอร์เก็บรายงาน (default = `./output`)
- `--exclude-id` รหัสที่ต้องการตัดออก (ใส่ซ้ำได้ หรือคั่น comma ได้)
- `--mode` `raw` หรือ `zscore` (default = `raw`)
- `--weights` รูปแบบ `Ne=...,Ni=...,Te=...,Ti=...,Se=...,Si=...,Fe=...,Fi=...`
- `--max-skipped-ratio` ค่าได้ช่วง `0..1` (default = `1.0`)

## ผลลัพธ์ตัวอย่าง (Equal Weights)

รันด้วย:
- `--exclude-id 99999999999`
- `--mode zscore`
- `--weights` default equal (normalized เป็น `0.1250` ทุกมิติ)

ผล Top 5 ของ `68090500418`:

1. `68090500404` - Chanyanuch Thanusorn - Distance `0.5446`
2. `68090500427` - Atilak Modetad - Distance `0.5472`
3. `68090500429` - Asnawee Ezor - Distance `0.5597`
4. `68090500421` - Yossakon Rungrattanrarak - Distance `0.5602`
5. `68090500416` - Peerawit Umphaisri - Distance `0.5996`

รายงานอ้างอิง:
- `reports/similarity_68090500418_zscore_equal_top5_20260302_224118.md`

ตัวอย่างรายงานเมื่อใช้ custom weights:
- `reports/similarity_68090500418_zscore_custom_top5_20260302_220341.md`

## มาตรฐานคุณภาพ

- แยก Domain/Service/CLI ชัดเจน
- Input validation ครบ (mode, exclude-id, weights)
- Error handling ชัดเจนและอ่านง่าย
- Structured logging แบบ JSON
- Data quality threshold gate
- CI: `.github/workflows/ci.yml`
- Build/Test ผ่านเรียบร้อย
