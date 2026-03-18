package com.mbti.similarity.tests;

import com.mbti.similarity.domain.DimensionWeights;

public final class DimensionWeightsTests {
    private DimensionWeightsTests() {
    }

    public static void runAll() {
        createShouldNormalizeWeightsToOne();
        createShouldRejectAllZeroWeights();
    }

    private static void createShouldNormalizeWeightsToOne() {
        DimensionWeights weights = DimensionWeights.create(2, 2, 1, 1, 1, 1, 1, 1);
        double sum = weights.toMap().values().stream().mapToDouble(Double::doubleValue).sum();

        Assertions.assertDoubleEquals(1.0d, sum, 1e-10, "Weights must sum to one.");
        Assertions.assertDoubleEquals(0.2d, weights.getNe(), 1e-10, "Ne should be normalized.");
        Assertions.assertDoubleEquals(0.1d, weights.getTe(), 1e-10, "Te should be normalized.");
    }

    private static void createShouldRejectAllZeroWeights() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> DimensionWeights.create(0, 0, 0, 0, 0, 0, 0, 0),
            "At least one weight",
            "Zero weights must be rejected."
        );
    }
}
