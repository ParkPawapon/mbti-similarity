package com.mbti.similarity.csv;

import java.util.List;
import java.util.Objects;

public final class CsvRow {
    private final int lineNumber;
    private final List<String> fields;

    public CsvRow(int lineNumber, List<String> fields) {
        this.lineNumber = lineNumber;
        this.fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public List<String> getFields() {
        return fields;
    }

    public boolean isBlank() {
        for (String field : fields) {
            if (field != null && !field.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
