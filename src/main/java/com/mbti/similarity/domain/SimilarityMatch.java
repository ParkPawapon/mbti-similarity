package com.mbti.similarity.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SimilarityMatch {
    private final StudentProfile candidate;
    private final double distance;
    private final Map<String, Double> absoluteDifferences;

    public SimilarityMatch(StudentProfile candidate, double distance, Map<String, Double> absoluteDifferences) {
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        this.distance = distance;
        this.absoluteDifferences = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(absoluteDifferences, "absoluteDifferences")));
    }

    public StudentProfile getCandidate() {
        return candidate;
    }

    public double getDistance() {
        return distance;
    }

    public Map<String, Double> getAbsoluteDifferences() {
        return absoluteDifferences;
    }
}
