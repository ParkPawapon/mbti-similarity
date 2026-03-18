package com.mbti.similarity.domain;

public final class SkippedRecord {
    private final int lineNumber;
    private final String id;
    private final String reason;

    public SkippedRecord(int lineNumber, String id, String reason) {
        this.lineNumber = lineNumber;
        this.id = id == null ? "" : id.trim();
        this.reason = reason == null ? "" : reason.trim();
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }
}
