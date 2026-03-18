package com.mbti.similarity.app;

import com.mbti.similarity.domain.DimensionWeights;
import com.mbti.similarity.domain.SimilarityMode;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class AppOptions {
    private final Path inputPath;
    private final String targetId;
    private final int topN;
    private final Path outputDirectory;
    private final double maxSkippedRatio;
    private final SimilarityMode similarityMode;
    private final DimensionWeights weights;
    private final List<String> excludedIds;
    private final boolean showHelp;

    private AppOptions(
        Path inputPath,
        String targetId,
        int topN,
        Path outputDirectory,
        double maxSkippedRatio,
        SimilarityMode similarityMode,
        DimensionWeights weights,
        List<String> excludedIds,
        boolean showHelp
    ) {
        this.inputPath = inputPath;
        this.targetId = targetId;
        this.topN = topN;
        this.outputDirectory = outputDirectory;
        this.maxSkippedRatio = maxSkippedRatio;
        this.similarityMode = similarityMode;
        this.weights = weights;
        this.excludedIds = List.copyOf(excludedIds);
        this.showHelp = showHelp;
    }

    public static AppOptions parse(String[] args) {
        if (args == null || args.length == 0) {
            return help();
        }

        String inputPath = null;
        String targetId = null;
        int topN = 5;
        String outputDirectory = null;
        double maxSkippedRatio = 1d;
        SimilarityMode mode = SimilarityMode.RAW;
        DimensionWeights weights = DimensionWeights.EQUAL;
        LinkedHashSet<String> excludedIds = new LinkedHashSet<>();

        for (int index = 0; index < args.length; index++) {
            String current = args[index];
            switch (current) {
                case "--help":
                case "-h":
                    return help();
                case "--input":
                    inputPath = readRequiredValue(args, ++index, "--input");
                    break;
                case "--target-id":
                    targetId = readRequiredValue(args, ++index, "--target-id");
                    break;
                case "--top":
                    topN = parseTopN(readRequiredValue(args, ++index, "--top"));
                    break;
                case "--output-dir":
                    outputDirectory = readRequiredValue(args, ++index, "--output-dir");
                    break;
                case "--max-skipped-ratio":
                    maxSkippedRatio = parseMaxSkippedRatio(readRequiredValue(args, ++index, "--max-skipped-ratio"));
                    break;
                case "--exclude-id":
                    addExcludedIds(excludedIds, readRequiredValue(args, ++index, "--exclude-id"));
                    break;
                case "--mode":
                    mode = SimilarityMode.parse(readRequiredValue(args, ++index, "--mode"));
                    break;
                case "--weights":
                    weights = DimensionWeights.parse(readRequiredValue(args, ++index, "--weights"));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + current);
            }
        }

        if (inputPath == null || inputPath.isBlank()) {
            throw new IllegalArgumentException("--input is required.");
        }

        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("--target-id is required.");
        }

        Path normalizedOutputDirectory = outputDirectory == null || outputDirectory.isBlank()
            ? Path.of(System.getProperty("user.dir"), "output")
            : Path.of(outputDirectory).toAbsolutePath().normalize();

        List<String> normalizedExcludedIds = excludedIds.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .sorted(Comparator.naturalOrder())
            .toList();

        return new AppOptions(
            Path.of(inputPath).toAbsolutePath().normalize(),
            targetId.trim(),
            topN,
            normalizedOutputDirectory,
            maxSkippedRatio,
            mode,
            weights,
            normalizedExcludedIds,
            false
        );
    }

    public static void printUsage(PrintStream output) {
        output.println("MBTI Similarity Analyzer");
        output.println();
        output.println("Usage:");
        output.println("  java -cp build/classes/main com.mbti.similarity.Main \\");
        output.println(
            "    --input <csv-path> --target-id <student-id> [--top 5] [--output-dir <folder>] "
                + "[--max-skipped-ratio 0.2] [--exclude-id <id>] [--mode raw|zscore] "
                + "[--weights Ne=1,Ni=1,Te=1,Ti=1,Se=1,Si=1,Fe=1,Fi=1]"
        );
        output.println();
        output.println("Examples:");
        output.println("  java -cp build/classes/main com.mbti.similarity.Main \\");
        output.println(
            "    --input \"./data/CSS121_MBTI_2026_68_2.csv\" --target-id 68090500418 --top 5 "
                + "--max-skipped-ratio 0.2 --exclude-id 99999999999 --mode zscore"
        );
        output.println("  java -cp build/classes/main com.mbti.similarity.Main \\");
        output.println(
            "    --input \"./data/CSS121_MBTI_2026_68_2.csv\" --target-id 68090500418 --mode zscore "
                + "--weights \"Ne=1.2,Ni=1.2,Te=1,Ti=1,Se=0.8,Si=0.8,Fe=1,Fi=1\""
        );
    }

    public Path getInputPath() {
        return inputPath;
    }

    public String getTargetId() {
        return targetId;
    }

    public int getTopN() {
        return topN;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public double getMaxSkippedRatio() {
        return maxSkippedRatio;
    }

    public SimilarityMode getSimilarityMode() {
        return similarityMode;
    }

    public DimensionWeights getWeights() {
        return weights;
    }

    public List<String> getExcludedIds() {
        return excludedIds;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    private static AppOptions help() {
        return new AppOptions(
            Path.of(""),
            "",
            5,
            Path.of(""),
            1d,
            SimilarityMode.RAW,
            DimensionWeights.EQUAL,
            List.of(),
            true
        );
    }

    private static String readRequiredValue(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException(flag + " requires a value.");
        }
        return args[index];
    }

    private static int parseTopN(String rawValue) {
        try {
            int topN = Integer.parseInt(rawValue);
            if (topN <= 0) {
                throw new IllegalArgumentException("--top must be a positive integer.");
            }
            return topN;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("--top must be a positive integer.", ex);
        }
    }

    private static double parseMaxSkippedRatio(String rawValue) {
        try {
            double ratio = Double.parseDouble(rawValue);
            if (!Double.isFinite(ratio) || ratio < 0d || ratio > 1d) {
                throw new IllegalArgumentException("--max-skipped-ratio must be in range [0, 1].");
            }
            return ratio;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("--max-skipped-ratio must be a number between 0 and 1.", ex);
        }
    }

    private static void addExcludedIds(Collection<String> destination, String rawValue) {
        for (String token : rawValue.split(",")) {
            String normalized = token.trim();
            if (!normalized.isEmpty()) {
                destination.add(normalized);
            }
        }
    }
}
