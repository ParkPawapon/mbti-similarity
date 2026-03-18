package com.mbti.similarity.util;

import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AppLogger {
    private final PrintStream output;
    private final String runId;

    public AppLogger(PrintStream output, String runId) {
        this.output = Objects.requireNonNull(output, "output");
        this.runId = Objects.requireNonNull(runId, "runId");
    }

    public void info(String message, Map<String, ?> fields) {
        log("INFO", message, fields);
    }

    public void warn(String message, Map<String, ?> fields) {
        log("WARN", message, fields);
    }

    public void error(String message, Map<String, ?> fields) {
        log("ERROR", message, fields);
    }

    private void log(String level, String message, Map<String, ?> fields) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        payload.put("level", level);
        payload.put("runId", runId);
        payload.put("message", message);
        if (fields != null) {
            payload.putAll(fields);
        }

        StringBuilder builder = new StringBuilder();
        builder.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                builder.append(", ");
            }

            builder.append('"')
                .append(JsonEscaper.escape(entry.getKey()))
                .append("\": ");

            Object value = entry.getValue();
            if (value == null) {
                builder.append("null");
            } else if (value instanceof Number number) {
                if (number instanceof Double || number instanceof Float) {
                    builder.append(NumberFormats.jsonNumber(number.doubleValue()));
                } else {
                    builder.append(number);
                }
            } else if (value instanceof Boolean bool) {
                builder.append(bool);
            } else {
                builder.append('"')
                    .append(JsonEscaper.escape(value.toString()))
                    .append('"');
            }

            first = false;
        }
        builder.append('}');

        output.println(builder);
    }
}
