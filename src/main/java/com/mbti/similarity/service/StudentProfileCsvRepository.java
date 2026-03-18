package com.mbti.similarity.service;

import com.mbti.similarity.csv.CsvParser;
import com.mbti.similarity.csv.CsvRow;
import com.mbti.similarity.domain.CognitiveScores;
import com.mbti.similarity.domain.LoadResult;
import com.mbti.similarity.domain.SkippedRecord;
import com.mbti.similarity.domain.StudentProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class StudentProfileCsvRepository {
    private static final List<String> REQUIRED_COLUMNS = List.of(
        "ID", "Name", "Sex", "Ne", "Ni", "Te", "Ti", "Se", "Si", "Fe", "Fi", "Type", "Enneagram", "Nick"
    );

    private final CsvParser csvParser;

    public StudentProfileCsvRepository() {
        this(new CsvParser());
    }

    public StudentProfileCsvRepository(CsvParser csvParser) {
        this.csvParser = Objects.requireNonNull(csvParser, "csvParser");
    }

    public LoadResult load(Path csvPath) {
        if (csvPath == null) {
            throw new IllegalArgumentException("CSV path is required.");
        }

        Path normalizedPath = csvPath.toAbsolutePath().normalize();
        if (!Files.exists(normalizedPath)) {
            throw new IllegalArgumentException("CSV file not found: " + normalizedPath);
        }

        List<CsvRow> rows;
        try {
            rows = csvParser.parse(normalizedPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read CSV file: " + normalizedPath, ex);
        }

        if (rows.isEmpty()) {
            throw new IllegalStateException("CSV file is empty.");
        }

        CsvRow headerRow = rows.get(0);
        Map<String, Integer> headerIndex = buildHeaderIndex(headerRow.getFields());
        validateRequiredColumns(headerIndex);

        List<StudentProfile> profiles = new ArrayList<>();
        List<SkippedRecord> skippedRecords = new ArrayList<>();
        int totalDataRows = 0;

        for (int i = 1; i < rows.size(); i++) {
            CsvRow row = rows.get(i);
            if (row.isBlank()) {
                continue;
            }

            totalDataRows++;

            try {
                List<String> fields = row.getFields();
                String id = getValue(fields, headerIndex, "ID");
                if (id.isBlank()) {
                    throw new IllegalArgumentException("ID is empty.");
                }

                CognitiveScores scores = new CognitiveScores(
                    parseScore(getValue(fields, headerIndex, "Ne"), "Ne"),
                    parseScore(getValue(fields, headerIndex, "Ni"), "Ni"),
                    parseScore(getValue(fields, headerIndex, "Te"), "Te"),
                    parseScore(getValue(fields, headerIndex, "Ti"), "Ti"),
                    parseScore(getValue(fields, headerIndex, "Se"), "Se"),
                    parseScore(getValue(fields, headerIndex, "Si"), "Si"),
                    parseScore(getValue(fields, headerIndex, "Fe"), "Fe"),
                    parseScore(getValue(fields, headerIndex, "Fi"), "Fi")
                );

                profiles.add(new StudentProfile(
                    id,
                    getValue(fields, headerIndex, "Name"),
                    getValue(fields, headerIndex, "Sex"),
                    scores,
                    getValue(fields, headerIndex, "Type"),
                    getValue(fields, headerIndex, "Enneagram"),
                    getValue(fields, headerIndex, "Nick")
                ));
            } catch (RuntimeException ex) {
                String id = safeGetValue(row.getFields(), headerIndex, "ID");
                skippedRecords.add(new SkippedRecord(row.getLineNumber(), id, ex.getMessage()));
            }
        }

        return new LoadResult(profiles, skippedRecords, totalDataRows);
    }

    private static Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        List<Integer> blankIndexes = new ArrayList<>();

        for (int i = 0; i < headers.size(); i++) {
            String normalizedHeader = normalizeHeader(headers.get(i));
            if (normalizedHeader.isEmpty()) {
                blankIndexes.add(i);
                continue;
            }

            String key = normalizedHeader.toLowerCase(Locale.ROOT);
            if (index.containsKey(key)) {
                throw new IllegalStateException("CSV header contains duplicate column: " + normalizedHeader);
            }
            index.put(key, i);
        }

        if (!index.containsKey("id") && blankIndexes.size() == 1 && blankIndexes.get(0) == 0) {
            index.put("id", 0);
        }

        return index;
    }

    private static void validateRequiredColumns(Map<String, Integer> headerIndex) {
        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_COLUMNS) {
            if (!headerIndex.containsKey(required.toLowerCase(Locale.ROOT))) {
                missing.add(required);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("CSV missing required columns: " + String.join(", ", missing));
        }
    }

    private static String safeGetValue(List<String> fields, Map<String, Integer> headerIndex, String columnName) {
        try {
            return getValue(fields, headerIndex, columnName);
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private static String getValue(List<String> fields, Map<String, Integer> headerIndex, String columnName) {
        Integer index = headerIndex.get(columnName.toLowerCase(Locale.ROOT));
        if (index == null || index >= fields.size()) {
            return "";
        }
        return fields.get(index).trim();
    }

    private static double parseScore(String rawValue, String columnName) {
        String normalized = rawValue == null ? "" : rawValue.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(columnName + " is empty.");
        }

        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(columnName + " is not numeric: '" + normalized + "'.", ex);
        }
    }

    private static String normalizeHeader(String rawHeader) {
        if (rawHeader == null) {
            return "";
        }

        String normalized = rawHeader.trim();
        if (!normalized.isEmpty() && normalized.charAt(0) == '\ufeff') {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }
}
