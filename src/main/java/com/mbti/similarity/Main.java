package com.mbti.similarity;

import com.mbti.similarity.app.AppOptions;
import com.mbti.similarity.domain.LoadResult;
import com.mbti.similarity.domain.SimilarityMatch;
import com.mbti.similarity.domain.StudentProfile;
import com.mbti.similarity.service.ResultExporter;
import com.mbti.similarity.service.SimilarityAnalyzer;
import com.mbti.similarity.service.StudentProfileCsvRepository;
import com.mbti.similarity.util.AppLogger;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    public static int run(String[] args) {
        return run(args, System.out, System.err);
    }

    static int run(String[] args, PrintStream standardOutput, PrintStream errorOutput) {
        String runId = UUID.randomUUID().toString().replace("-", "");
        AppLogger logger = new AppLogger(errorOutput, runId);

        try {
            AppOptions options = AppOptions.parse(args);
            if (options.isShowHelp()) {
                AppOptions.printUsage(standardOutput);
                return 0;
            }

            logger.info("Analysis started", Map.of(
                "targetId", options.getTargetId(),
                "mode", options.getSimilarityMode().name(),
                "outputDir", options.getOutputDirectory().toString(),
                "maxSkippedRatio", options.getMaxSkippedRatio()
            ));

            StudentProfileCsvRepository repository = new StudentProfileCsvRepository();
            LoadResult loadResult = repository.load(options.getInputPath());
            double skippedRatio = calculateSkippedRatio(loadResult);

            logger.info("CSV loaded", Map.of(
                "totalRows", loadResult.getTotalDataRows(),
                "validRows", loadResult.getProfiles().size(),
                "skippedRows", loadResult.getSkippedRecords().size(),
                "skippedRatio", skippedRatio
            ));

            if (skippedRatio > options.getMaxSkippedRatio()) {
                logger.error("Data quality gate failed", Map.of(
                    "skippedRatio", skippedRatio,
                    "threshold", options.getMaxSkippedRatio()
                ));

                errorOutput.println(
                    "[DATA QUALITY ERROR] skipped ratio "
                        + String.format(java.util.Locale.ROOT, "%.4f", skippedRatio)
                        + " exceeds --max-skipped-ratio "
                        + String.format(java.util.Locale.ROOT, "%.4f", options.getMaxSkippedRatio())
                        + "."
                );
                return 1;
            }

            SimilarityAnalyzer analyzer = new SimilarityAnalyzer();
            StudentProfile target = analyzer.findTarget(
                loadResult.getProfiles(),
                options.getTargetId(),
                options.getExcludedIds()
            );
            List<SimilarityMatch> nearest = analyzer.findNearest(
                loadResult.getProfiles(),
                options.getTargetId(),
                options.getTopN(),
                options.getSimilarityMode(),
                options.getExcludedIds(),
                options.getWeights()
            );

            ResultExporter exporter = new ResultExporter();
            exporter.writeConsoleSummary(
                target,
                nearest,
                loadResult,
                options.getInputPath(),
                options.getSimilarityMode(),
                options.getExcludedIds(),
                options.getWeights(),
                skippedRatio,
                options.getMaxSkippedRatio()
            );

            Files.createDirectories(options.getOutputDirectory());

            String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String modeText = options.getSimilarityMode().name().toLowerCase(java.util.Locale.ROOT);
            String weightsText = options.getWeights().isEqualProfile() ? "equal" : "custom";
            String baseName = "similarity_" + options.getTargetId()
                + "_" + modeText
                + "_" + weightsText
                + "_top" + nearest.size()
                + "_" + timestamp;

            Path markdownPath = options.getOutputDirectory().resolve(baseName + ".md");
            Path jsonPath = options.getOutputDirectory().resolve(baseName + ".json");

            exporter.writeMarkdownReport(
                markdownPath,
                target,
                nearest,
                loadResult,
                options.getInputPath(),
                options.getSimilarityMode(),
                options.getExcludedIds(),
                options.getWeights(),
                skippedRatio,
                options.getMaxSkippedRatio()
            );
            exporter.writeJsonReport(
                jsonPath,
                target,
                nearest,
                loadResult,
                options.getInputPath(),
                options.getSimilarityMode(),
                options.getExcludedIds(),
                options.getWeights(),
                skippedRatio,
                options.getMaxSkippedRatio()
            );

            standardOutput.println("Markdown report saved: " + markdownPath);
            standardOutput.println("JSON report saved    : " + jsonPath);

            logger.info("Analysis completed successfully", Map.of(
                "markdownReport", markdownPath.toString(),
                "jsonReport", jsonPath.toString()
            ));

            return 0;
        } catch (IllegalArgumentException ex) {
            logger.warn("Input validation failed", Map.of("error", ex.getMessage()));
            errorOutput.println("[INPUT ERROR] " + ex.getMessage());
            errorOutput.println();
            AppOptions.printUsage(errorOutput);
            return 1;
        } catch (Exception ex) {
            logger.error("Unhandled error during analysis", buildErrorFields(ex));
            errorOutput.println("[ERROR] " + ex.getMessage());
            return 1;
        }
    }

    private static double calculateSkippedRatio(LoadResult loadResult) {
        if (loadResult.getTotalDataRows() <= 0) {
            return 0d;
        }

        return (double) loadResult.getSkippedRecords().size() / loadResult.getTotalDataRows();
    }

    private static Map<String, Object> buildErrorFields(Exception exception) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("error", exception.getMessage());
        fields.put("exceptionType", exception.getClass().getName());
        return fields;
    }
}
