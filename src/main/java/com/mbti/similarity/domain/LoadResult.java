package com.mbti.similarity.domain;

import java.util.List;
import java.util.Objects;

public final class LoadResult {
    private final List<StudentProfile> profiles;
    private final List<SkippedRecord> skippedRecords;
    private final int totalDataRows;

    public LoadResult(List<StudentProfile> profiles, List<SkippedRecord> skippedRecords, int totalDataRows) {
        this.profiles = List.copyOf(Objects.requireNonNull(profiles, "profiles"));
        this.skippedRecords = List.copyOf(Objects.requireNonNull(skippedRecords, "skippedRecords"));
        this.totalDataRows = totalDataRows;
    }

    public List<StudentProfile> getProfiles() {
        return profiles;
    }

    public List<SkippedRecord> getSkippedRecords() {
        return skippedRecords;
    }

    public int getTotalDataRows() {
        return totalDataRows;
    }
}
