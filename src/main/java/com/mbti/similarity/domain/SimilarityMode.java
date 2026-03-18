package com.mbti.similarity.domain;

public enum SimilarityMode {
    RAW,
    ZSCORE;

    public static SimilarityMode parse(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("--mode must be either 'raw' or 'zscore'.");
        }

        String normalized = rawValue.trim();
        if (normalized.equalsIgnoreCase("raw")) {
            return RAW;
        }

        if (normalized.equalsIgnoreCase("zscore") || normalized.equalsIgnoreCase("z-score")) {
            return ZSCORE;
        }

        throw new IllegalArgumentException("--mode must be either 'raw' or 'zscore'.");
    }
}
