package com.mbti.similarity.service;

import com.mbti.similarity.domain.DimensionWeights;
import com.mbti.similarity.domain.LoadResult;
import com.mbti.similarity.domain.SimilarityMatch;
import com.mbti.similarity.domain.SimilarityMode;
import com.mbti.similarity.domain.SkippedRecord;
import com.mbti.similarity.domain.StudentProfile;
import com.mbti.similarity.util.JsonEscaper;
import com.mbti.similarity.util.NumberFormats;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ResultExporter {
    public void writeConsoleSummary(
        StudentProfile target,
        List<SimilarityMatch> matches,
        LoadResult loadResult,
        Path sourcePath,
        SimilarityMode mode,
        Collection<String> excludedIds,
        DimensionWeights weights,
        double skippedRatio,
        double maxSkippedRatio
    ) {
        List<String> normalizedExcludedIds = normalizeExcludedIds(excludedIds);

        System.out.println("=== MBTI Cognitive Similarity Report ===");
        System.out.println("Source CSV      : " + sourcePath);
        System.out.println("Similarity mode : " + mode);
        System.out.println("Weights (norm)  : " + weights.toCompactDisplay());
        System.out.println(
            "Data quality    : skippedRatio=" + NumberFormats.formatFixed(skippedRatio, 4)
                + ", threshold=" + NumberFormats.formatFixed(maxSkippedRatio, 4)
        );
        System.out.println(
            "Excluded IDs    : " + (normalizedExcludedIds.isEmpty() ? "(none)" : String.join(", ", normalizedExcludedIds))
        );
        System.out.println("Target          : " + target.getId() + " | " + target.getName() + " (" + target.getNick() + ")");
        System.out.println("Type/Enneagram  : " + target.getType() + " / " + target.getEnneagram());
        System.out.println(
            "Rows loaded     : " + loadResult.getProfiles().size() + "/" + loadResult.getTotalDataRows() + " valid"
        );
        System.out.println("Rows skipped    : " + loadResult.getSkippedRecords().size());

        if (!loadResult.getSkippedRecords().isEmpty()) {
            List<String> preview = new ArrayList<>();
            int limit = Math.min(5, loadResult.getSkippedRecords().size());
            for (int index = 0; index < limit; index++) {
                preview.add(loadResult.getSkippedRecords().get(index).getId());
            }
            System.out.println(
                "Skipped IDs     : " + String.join(", ", preview)
                    + (loadResult.getSkippedRecords().size() > 5 ? ", ..." : "")
            );
        }

        System.out.println();
        System.out.printf("%-4s %-11s %-26s %-5s %9s%n", "Rank", "ID", "Name", "Type", "Distance");
        System.out.println("-".repeat(62));

        for (int i = 0; i < matches.size(); i++) {
            SimilarityMatch match = matches.get(i);
            System.out.printf(
                "%-4d %-11s %-26s %-5s %9s%n",
                i + 1,
                match.getCandidate().getId(),
                truncate(match.getCandidate().getName(), 26),
                match.getCandidate().getType(),
                NumberFormats.formatFixed(match.getDistance(), 4)
            );
        }

        System.out.println();
    }

    public void writeMarkdownReport(
        Path outputPath,
        StudentProfile target,
        List<SimilarityMatch> matches,
        LoadResult loadResult,
        Path sourcePath,
        SimilarityMode mode,
        Collection<String> excludedIds,
        DimensionWeights weights,
        double skippedRatio,
        double maxSkippedRatio
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("# MBTI Cognitive Similarity Report\n\n");
        builder.append("- Generated at: ")
            .append(OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx")))
            .append('\n');
        builder.append("- Source CSV: `").append(sourcePath).append("`\n");
        builder.append("- Similarity Mode: `").append(mode).append("`\n");
        builder.append("- Weights (normalized): ");

        boolean first = true;
        for (Map.Entry<String, Double> entry : weights.toMap().entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append('`')
                .append(entry.getKey())
                .append('=')
                .append(NumberFormats.formatFixed(entry.getValue(), 4))
                .append('`');
            first = false;
        }
        builder.append('\n');
        builder.append("- Data Quality Threshold: `")
            .append(NumberFormats.formatFixed(maxSkippedRatio, 4))
            .append("`\n");
        builder.append("- Data Quality Actual Skipped Ratio: `")
            .append(NumberFormats.formatFixed(skippedRatio, 4))
            .append("`\n");

        List<String> normalizedExcludedIds = normalizeExcludedIds(excludedIds);
        builder.append("- Excluded IDs: ");
        if (normalizedExcludedIds.isEmpty()) {
            builder.append("(none)\n");
        } else {
            for (int index = 0; index < normalizedExcludedIds.size(); index++) {
                if (index > 0) {
                    builder.append(", ");
                }
                builder.append('`').append(normalizedExcludedIds.get(index)).append('`');
            }
            builder.append('\n');
        }

        builder.append("- Target ID: `").append(target.getId()).append("`\n");
        builder.append("- Target Name: ").append(target.getName()).append(" (").append(target.getNick()).append(")\n");
        builder.append("- Target Type: ").append(target.getType()).append('\n');
        builder.append("- Total Rows: ").append(loadResult.getTotalDataRows()).append('\n');
        builder.append("- Valid Rows: ").append(loadResult.getProfiles().size()).append('\n');
        builder.append("- Skipped Rows: ").append(loadResult.getSkippedRecords().size()).append('\n');
        builder.append('\n');

        builder.append("## Top Similar Profiles\n\n");
        builder.append("| Rank | ID | Name | Type | Distance | NeΔ | NiΔ | TeΔ | TiΔ | SeΔ | SiΔ | FeΔ | FiΔ |\n");
        builder.append("| ---: | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |\n");

        for (int index = 0; index < matches.size(); index++) {
            SimilarityMatch match = matches.get(index);
            builder.append("| ")
                .append(index + 1)
                .append(" | ")
                .append(match.getCandidate().getId())
                .append(" | ")
                .append(escapeMarkdownPipes(match.getCandidate().getName()))
                .append(" | ")
                .append(match.getCandidate().getType())
                .append(" | ")
                .append(NumberFormats.formatFixed(match.getDistance(), 4));

            for (String dimension : List.of("Ne", "Ni", "Te", "Ti", "Se", "Si", "Fe", "Fi")) {
                builder.append(" | ")
                    .append(NumberFormats.formatFixed(match.getAbsoluteDifferences().get(dimension), 2));
            }

            builder.append(" |\n");
        }

        if (!loadResult.getSkippedRecords().isEmpty()) {
            builder.append('\n');
            builder.append("## Skipped Rows (Missing/Invalid Scores)\n\n");
            builder.append("| Line | ID | Reason |\n");
            builder.append("| ---: | --- | --- |\n");
            for (SkippedRecord skippedRecord : loadResult.getSkippedRecords()) {
                builder.append("| ")
                    .append(skippedRecord.getLineNumber())
                    .append(" | ")
                    .append(escapeMarkdownPipes(skippedRecord.getId()))
                    .append(" | ")
                    .append(escapeMarkdownPipes(skippedRecord.getReason()))
                    .append(" |\n");
            }
        }

        writeUtf8(outputPath, builder.toString());
    }

    public void writeJsonReport(
        Path outputPath,
        StudentProfile target,
        List<SimilarityMatch> matches,
        LoadResult loadResult,
        Path sourcePath,
        SimilarityMode mode,
        Collection<String> excludedIds,
        DimensionWeights weights,
        double skippedRatio,
        double maxSkippedRatio
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendJsonField(builder, 1, "generatedAt", OffsetDateTime.now().toString(), true);
        appendJsonField(builder, 1, "sourceCsv", sourcePath.toString(), true);
        builder.append(indent(1)).append("\"analysis\": {\n");
        appendJsonField(builder, 2, "similarityMode", mode.name(), true);
        appendJsonObject(builder, 2, "normalizedWeights", weights.toMap(), true);
        appendJsonNumberField(builder, 2, "maxSkippedRatio", maxSkippedRatio, true, false);
        appendJsonNumberField(builder, 2, "actualSkippedRatio", skippedRatio, true, false);
        appendJsonStringArray(builder, 2, "excludedIds", normalizeExcludedIds(excludedIds), false);
        builder.append(indent(1)).append("},\n");

        builder.append(indent(1)).append("\"summary\": {\n");
        appendJsonNumberField(builder, 2, "totalRows", loadResult.getTotalDataRows(), true, true);
        appendJsonNumberField(builder, 2, "validRows", loadResult.getProfiles().size(), true, true);
        appendJsonNumberField(builder, 2, "skippedRows", loadResult.getSkippedRecords().size(), false, true);
        builder.append(indent(1)).append("},\n");

        builder.append(indent(1)).append("\"target\": {\n");
        appendJsonField(builder, 2, "id", target.getId(), true);
        appendJsonField(builder, 2, "name", target.getName(), true);
        appendJsonField(builder, 2, "nick", target.getNick(), true);
        appendJsonField(builder, 2, "type", target.getType(), true);
        appendJsonField(builder, 2, "enneagram", target.getEnneagram(), true);
        appendJsonField(builder, 2, "sex", target.getSex(), true);
        appendJsonObject(builder, 2, "scores", target.getScores().toMap(), false);
        builder.append(indent(1)).append("},\n");

        builder.append(indent(1)).append("\"topMatches\": [\n");
        for (int index = 0; index < matches.size(); index++) {
            SimilarityMatch match = matches.get(index);
            builder.append(indent(2)).append("{\n");
            appendJsonNumberField(builder, 3, "rank", index + 1, true, true);
            appendJsonField(builder, 3, "id", match.getCandidate().getId(), true);
            appendJsonField(builder, 3, "name", match.getCandidate().getName(), true);
            appendJsonField(builder, 3, "nick", match.getCandidate().getNick(), true);
            appendJsonField(builder, 3, "type", match.getCandidate().getType(), true);
            appendJsonField(builder, 3, "enneagram", match.getCandidate().getEnneagram(), true);
            appendJsonNumberField(builder, 3, "distance", match.getDistance(), true, false, 6);
            appendJsonObject(builder, 3, "absoluteDifferences", match.getAbsoluteDifferences(), false);
            builder.append(indent(2)).append(index + 1 < matches.size() ? "},\n" : "}\n");
        }
        builder.append(indent(1)).append("],\n");

        builder.append(indent(1)).append("\"skipped\": [\n");
        for (int index = 0; index < loadResult.getSkippedRecords().size(); index++) {
            SkippedRecord skippedRecord = loadResult.getSkippedRecords().get(index);
            builder.append(indent(2)).append("{\n");
            appendJsonNumberField(builder, 3, "lineNumber", skippedRecord.getLineNumber(), true, true);
            appendJsonField(builder, 3, "id", skippedRecord.getId(), true);
            appendJsonField(builder, 3, "reason", skippedRecord.getReason(), false);
            builder.append(indent(2)).append(index + 1 < loadResult.getSkippedRecords().size() ? "},\n" : "}\n");
        }
        builder.append(indent(1)).append("]\n");
        builder.append("}\n");

        writeUtf8(outputPath, builder.toString());
    }

    private static void writeUtf8(Path outputPath, String content) {
        try {
            Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write report: " + outputPath, ex);
        }
    }

    private static List<String> normalizeExcludedIds(Collection<String> excludedIds) {
        if (excludedIds == null) {
            return List.of();
        }

        return excludedIds.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private static void appendJsonField(StringBuilder builder, int indentLevel, String key, String value, boolean trailingComma) {
        builder.append(indent(indentLevel))
            .append('"')
            .append(JsonEscaper.escape(key))
            .append("\": \"")
            .append(JsonEscaper.escape(value))
            .append('"')
            .append(trailingComma ? ",\n" : "\n");
    }

    private static void appendJsonNumberField(
        StringBuilder builder,
        int indentLevel,
        String key,
        double value,
        boolean trailingComma,
        boolean integral
    ) {
        appendJsonNumberField(builder, indentLevel, key, value, trailingComma, integral, 6);
    }

    private static void appendJsonNumberField(
        StringBuilder builder,
        int indentLevel,
        String key,
        double value,
        boolean trailingComma,
        boolean integral,
        int decimals
    ) {
        builder.append(indent(indentLevel))
            .append('"')
            .append(JsonEscaper.escape(key))
            .append("\": ")
            .append(integral ? Long.toString((long) value) : NumberFormats.jsonRounded(value, decimals))
            .append(trailingComma ? ",\n" : "\n");
    }

    private static void appendJsonObject(
        StringBuilder builder,
        int indentLevel,
        String key,
        Map<String, Double> values,
        boolean trailingComma
    ) {
        builder.append(indent(indentLevel))
            .append('"')
            .append(JsonEscaper.escape(key))
            .append("\": {\n");

        int index = 0;
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            boolean comma = index + 1 < values.size();
            builder.append(indent(indentLevel + 1))
                .append('"')
                .append(JsonEscaper.escape(entry.getKey()))
                .append("\": ")
                .append(NumberFormats.jsonRounded(entry.getValue(), 6))
                .append(comma ? ",\n" : "\n");
            index++;
        }

        builder.append(indent(indentLevel))
            .append('}')
            .append(trailingComma ? ",\n" : "\n");
    }

    private static void appendJsonStringArray(
        StringBuilder builder,
        int indentLevel,
        String key,
        List<String> values,
        boolean trailingComma
    ) {
        builder.append(indent(indentLevel))
            .append('"')
            .append(JsonEscaper.escape(key))
            .append("\": [");

        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append('"')
                .append(JsonEscaper.escape(values.get(index)))
                .append('"');
        }

        builder.append(']')
            .append(trailingComma ? ",\n" : "\n");
    }

    private static String indent(int level) {
        return "  ".repeat(level);
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private static String escapeMarkdownPipes(String value) {
        return value.replace("|", "\\|");
    }
}
