#!/usr/bin/env Rscript

# Supplementary R script for statistical visualization/reporting.
# This does not replace the C# production pipeline.

args <- commandArgs(trailingOnly = TRUE)

parse_args <- function(raw_args) {
  result <- list(
    input = "./data/CSS121_MBTI_2026_68.csv",
    target_id = "68090500418",
    top = 5,
    mode = "zscore",
    exclude_id = "99999999999",
    weights = "Ne=1,Ni=1,Te=1,Ti=1,Se=1,Si=1,Fe=1,Fi=1",
    max_skipped_ratio = 1.0,
    output_dir = "./reports/r"
  )

  i <- 1
  while (i <= length(raw_args)) {
    key <- raw_args[[i]]
    if (i == length(raw_args)) {
      stop(paste("Missing value for", key))
    }
    value <- raw_args[[i + 1]]

    if (key == "--input") result$input <- value
    else if (key == "--target-id") result$target_id <- value
    else if (key == "--top") result$top <- as.integer(value)
    else if (key == "--mode") result$mode <- tolower(value)
    else if (key == "--exclude-id") result$exclude_id <- value
    else if (key == "--weights") result$weights <- value
    else if (key == "--max-skipped-ratio") result$max_skipped_ratio <- as.numeric(value)
    else if (key == "--output-dir") result$output_dir <- value
    else stop(paste("Unknown arg:", key))

    i <- i + 2
  }

  result
}

parse_weights <- function(weight_string, dimensions) {
  weights <- setNames(rep(1, length(dimensions)), dimensions)
  parts <- strsplit(weight_string, ",")[[1]]

  for (part in parts) {
    token <- trimws(part)
    if (nchar(token) == 0) next

    pair <- strsplit(token, "=")[[1]]
    if (length(pair) != 2) stop(paste("Invalid weight token:", token))

    key <- trimws(pair[[1]])
    value <- as.numeric(trimws(pair[[2]]))

    if (!(key %in% dimensions)) stop(paste("Unknown weight dimension:", key))
    if (is.na(value) || !is.finite(value) || value < 0) stop(paste("Invalid weight value for", key))

    weights[[key]] <- value
  }

  total <- sum(weights)
  if (total <= 0) stop("At least one weight must be > 0")
  weights / total
}

weighted_distance <- function(a, b, w) {
  sqrt(sum(w * (a - b)^2))
}

opts <- parse_args(args)

dimensions <- c("Ne", "Ni", "Te", "Ti", "Se", "Si", "Fe", "Fi")
required_columns <- c("ID", "Name", "Type", dimensions)

if (!file.exists(opts$input)) {
  stop(paste("Input CSV not found:", opts$input))
}

dir.create(opts$output_dir, recursive = TRUE, showWarnings = FALSE)

raw <- read.csv(opts$input, stringsAsFactors = FALSE, check.names = FALSE)

missing <- setdiff(required_columns, names(raw))
if (length(missing) > 0) {
  stop(paste("Missing required columns:", paste(missing, collapse = ", ")))
}

for (d in dimensions) {
  raw[[d]] <- suppressWarnings(as.numeric(raw[[d]]))
}

valid_mask <- complete.cases(raw[, dimensions])
valid <- raw[valid_mask, ]
skipped <- raw[!valid_mask, ]

skipped_ratio <- if (nrow(raw) == 0) 0 else nrow(skipped) / nrow(raw)
if (skipped_ratio > opts$max_skipped_ratio) {
  stop(sprintf("Data quality gate failed: skipped_ratio=%.4f > threshold=%.4f", skipped_ratio, opts$max_skipped_ratio))
}

exclude_ids <- trimws(unlist(strsplit(opts$exclude_id, ",")))
exclude_ids <- exclude_ids[exclude_ids != ""]
analysis <- valid[!(valid$ID %in% exclude_ids), ]

if (!(opts$target_id %in% analysis$ID)) {
  stop(paste("Target ID not found after exclusions:", opts$target_id))
}

if (nrow(analysis) < 2) {
  stop("Need at least 2 valid rows after exclusions")
}

weights <- parse_weights(opts$weights, dimensions)

mat <- as.matrix(analysis[, dimensions])
mode(mat) <- "numeric"

if (opts$mode == "zscore") {
  means <- colMeans(mat)
  sds <- apply(mat, 2, sd)
  sds[sds == 0] <- 1
  mat <- scale(mat, center = means, scale = sds)
} else if (opts$mode != "raw") {
  stop("--mode must be raw or zscore")
}

target_idx <- which(analysis$ID == opts$target_id)[1]

dists <- apply(mat, 1, function(row) weighted_distance(mat[target_idx, ], row, weights))

result <- data.frame(
  ID = analysis$ID,
  Name = analysis$Name,
  Type = analysis$Type,
  Distance = dists,
  stringsAsFactors = FALSE
)

result <- result[result$ID != opts$target_id, ]
result <- result[order(result$Distance, result$ID), ]
top_n <- head(result, opts$top)

write.csv(top_n, file.path(opts$output_dir, "top_matches_r.csv"), row.names = FALSE)

summary_lines <- c(
  sprintf("input=%s", normalizePath(opts$input, winslash = "/", mustWork = FALSE)),
  sprintf("target_id=%s", opts$target_id),
  sprintf("mode=%s", opts$mode),
  sprintf("weights=%s", paste(sprintf("%s=%.4f", names(weights), weights), collapse = ",")),
  sprintf("total_rows=%d", nrow(raw)),
  sprintf("valid_rows=%d", nrow(valid)),
  sprintf("skipped_rows=%d", nrow(skipped)),
  sprintf("skipped_ratio=%.4f", skipped_ratio)
)
writeLines(summary_lines, file.path(opts$output_dir, "summary_r.txt"))

png(file.path(opts$output_dir, "top_matches_barplot.png"), width = 1200, height = 700)
barplot(
  top_n$Distance,
  names.arg = top_n$ID,
  las = 2,
  col = "steelblue",
  main = sprintf("Top %d Similar Profiles (R %s)", nrow(top_n), toupper(opts$mode)),
  ylab = "Distance"
)
text(seq_along(top_n$Distance), top_n$Distance, labels = sprintf("%.4f", top_n$Distance), pos = 3, cex = 0.8)
dev.off()

raw_analysis <- analysis[, dimensions]
raw_target <- as.numeric(raw_analysis[target_idx, ])
raw_others <- raw_analysis[analysis$ID != opts$target_id, , drop = FALSE]
raw_others <- raw_others[match(top_n$ID, analysis$ID[analysis$ID != opts$target_id]), , drop = FALSE]

abs_diff <- abs(sweep(as.matrix(raw_others), 2, raw_target, FUN = "-"))
rownames(abs_diff) <- top_n$ID
colnames(abs_diff) <- dimensions

png(file.path(opts$output_dir, "dimension_diff_heatmap.png"), width = 1200, height = 700)
heatmap(
  abs_diff,
  Rowv = NA,
  Colv = NA,
  scale = "none",
  col = colorRampPalette(c("#f7fbff", "#6baed6", "#08306b"))(100),
  margins = c(8, 8),
  main = "Absolute Difference by Dimension"
)
dev.off()

cat("R visualization/report generated at:", normalizePath(opts$output_dir, winslash = "/", mustWork = FALSE), "\n")
