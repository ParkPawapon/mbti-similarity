package com.mbti.similarity.tests;

import com.mbti.similarity.app.AppOptions;
import com.mbti.similarity.domain.SimilarityMode;
import java.util.List;

public final class AppOptionsTests {
    private AppOptionsTests() {
    }

    public static void runAll() {
        parseShouldSupportExcludeIdsAndZScoreMode();
        parseShouldRejectUnknownMode();
        parseShouldRejectUnknownWeightDimension();
        parseShouldRejectInvalidMaxSkippedRatio();
    }

    private static void parseShouldSupportExcludeIdsAndZScoreMode() {
        AppOptions options = AppOptions.parse(new String[] {
            "--input", "./data.csv",
            "--target-id", "68090500418",
            "--exclude-id", "99999999999",
            "--exclude-id", "68090500001,68090500002",
            "--mode", "zscore",
            "--weights", "Ne=2,Ni=2,Te=1,Ti=1,Se=1,Si=1,Fe=1,Fi=1"
        });

        Assertions.assertEquals(SimilarityMode.ZSCORE, options.getSimilarityMode(), "Mode should be parsed.");
        Assertions.assertEquals(
            List.of("68090500001", "68090500002", "99999999999"),
            options.getExcludedIds(),
            "Excluded IDs should be normalized and sorted."
        );
        Assertions.assertDoubleEquals(0.2d, options.getWeights().getNe(), 1e-10, "Ne weight should be normalized.");
        Assertions.assertDoubleEquals(0.1d, options.getWeights().getTe(), 1e-10, "Te weight should be normalized.");
    }

    private static void parseShouldRejectUnknownMode() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> AppOptions.parse(new String[] {
                "--input", "./data.csv",
                "--target-id", "68090500418",
                "--mode", "weighted"
            }),
            "--mode",
            "Unknown mode should be rejected."
        );
    }

    private static void parseShouldRejectUnknownWeightDimension() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> AppOptions.parse(new String[] {
                "--input", "./data.csv",
                "--target-id", "68090500418",
                "--weights", "XX=2"
            }),
            "Unknown dimension",
            "Unknown weight dimension should be rejected."
        );
    }

    private static void parseShouldRejectInvalidMaxSkippedRatio() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> AppOptions.parse(new String[] {
                "--input", "./data.csv",
                "--target-id", "68090500418",
                "--max-skipped-ratio", "1.2"
            }),
            "--max-skipped-ratio",
            "Invalid skipped ratio should be rejected."
        );
    }
}
