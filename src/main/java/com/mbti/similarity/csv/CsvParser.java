package com.mbti.similarity.csv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CsvParser {
    public List<CsvRow> parse(Path path) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '\ufeff') {
            content = content.substring(1);
        }

        List<CsvRow> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int lineNumber = 1;
        int rowStartLine = 1;

        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);

            if (current == '"') {
                if (inQuotes && index + 1 < content.length() && content.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (!inQuotes && current == ',') {
                fields.add(field.toString());
                field.setLength(0);
                continue;
            }

            if (!inQuotes && (current == '\n' || current == '\r')) {
                fields.add(field.toString());
                field.setLength(0);
                rows.add(new CsvRow(rowStartLine, new ArrayList<>(fields)));
                fields.clear();

                if (current == '\r' && index + 1 < content.length() && content.charAt(index + 1) == '\n') {
                    index++;
                }

                lineNumber++;
                rowStartLine = lineNumber;
                continue;
            }

            field.append(current);
            if (current == '\n') {
                lineNumber++;
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException("CSV contains an unclosed quoted field.");
        }

        if (field.length() > 0 || !fields.isEmpty()) {
            fields.add(field.toString());
            rows.add(new CsvRow(rowStartLine, new ArrayList<>(fields)));
        }

        return rows;
    }
}
