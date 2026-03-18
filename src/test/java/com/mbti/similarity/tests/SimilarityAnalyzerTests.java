package com.mbti.similarity.tests;

import com.mbti.similarity.domain.CognitiveScores;
import com.mbti.similarity.domain.DimensionWeights;
import com.mbti.similarity.domain.SimilarityMatch;
import com.mbti.similarity.domain.SimilarityMode;
import com.mbti.similarity.domain.StudentProfile;
import com.mbti.similarity.service.SimilarityAnalyzer;
import java.util.List;

public final class SimilarityAnalyzerTests {
    private SimilarityAnalyzerTests() {
    }

    public static void runAll() {
        euclideanDistanceShouldBeCorrect();
        findNearestShouldOrderByDistanceThenId();
        findNearestShouldRespectExcludedIds();
        findNearestZScoreModeShouldChangeRankingComparedToRaw();
        findNearestCustomWeightsShouldAffectRanking();
    }

    private static void euclideanDistanceShouldBeCorrect() {
        CognitiveScores a = new CognitiveScores(1, 2, 3, 4, 5, 6, 7, 8);
        CognitiveScores b = new CognitiveScores(1, 2, 3, 4, 5, 6, 7, 10);

        Assertions.assertDoubleEquals(2.0d, a.euclideanDistanceTo(b), 1e-10, "Euclidean distance should be correct.");
    }

    private static void findNearestShouldOrderByDistanceThenId() {
        List<StudentProfile> profiles = List.of(
            buildProfile("T", "Target", new CognitiveScores(0, 0, 0, 0, 0, 0, 0, 0)),
            buildProfile("002", "A", new CognitiveScores(1, 0, 0, 0, 0, 0, 0, 0)),
            buildProfile("001", "B", new CognitiveScores(1, 0, 0, 0, 0, 0, 0, 0)),
            buildProfile("003", "C", new CognitiveScores(2, 0, 0, 0, 0, 0, 0, 0))
        );

        SimilarityAnalyzer analyzer = new SimilarityAnalyzer();
        List<SimilarityMatch> nearest = analyzer.findNearest(profiles, "T", 3, SimilarityMode.RAW, List.of(), DimensionWeights.EQUAL);

        Assertions.assertEquals("001", nearest.get(0).getCandidate().getId(), "First result should be ordered by ID on ties.");
        Assertions.assertEquals("002", nearest.get(1).getCandidate().getId(), "Second result should be ordered by ID on ties.");
        Assertions.assertEquals("003", nearest.get(2).getCandidate().getId(), "Third result should be the farthest.");
    }

    private static void findNearestShouldRespectExcludedIds() {
        List<StudentProfile> profiles = List.of(
            buildProfile("T", "Target", new CognitiveScores(0, 0, 0, 0, 0, 0, 0, 0)),
            buildProfile("001", "A", new CognitiveScores(1, 0, 0, 0, 0, 0, 0, 0)),
            buildProfile("002", "B", new CognitiveScores(2, 0, 0, 0, 0, 0, 0, 0))
        );

        SimilarityAnalyzer analyzer = new SimilarityAnalyzer();
        List<SimilarityMatch> nearest = analyzer.findNearest(profiles, "T", 2, SimilarityMode.RAW, List.of("001"), DimensionWeights.EQUAL);

        Assertions.assertEquals(1, nearest.size(), "Excluded IDs should be removed from candidates.");
        Assertions.assertEquals("002", nearest.get(0).getCandidate().getId(), "Remaining candidate should be returned.");
    }

    private static void findNearestZScoreModeShouldChangeRankingComparedToRaw() {
        List<StudentProfile> profiles = List.of(
            buildProfile("T", "Target", new CognitiveScores(1000, 0, 0, 0, 0, 0, 0, 0)),
            buildProfile("A", "A", new CognitiveScores(1010, 0, 0, 0, 0, 0, 0, 0)),
            buildProfile("B", "B", new CognitiveScores(1000, 5, 0, 0, 0, 0, 0, 0)),
            buildProfile("C", "C", new CognitiveScores(2000, 0, 0, 0, 0, 0, 0, 0))
        );

        SimilarityAnalyzer analyzer = new SimilarityAnalyzer();
        List<SimilarityMatch> rawNearest = analyzer.findNearest(profiles, "T", 1, SimilarityMode.RAW, List.of(), DimensionWeights.EQUAL);
        List<SimilarityMatch> zscoreNearest = analyzer.findNearest(profiles, "T", 1, SimilarityMode.ZSCORE, List.of(), DimensionWeights.EQUAL);

        Assertions.assertEquals("B", rawNearest.get(0).getCandidate().getId(), "Raw mode ranking should match the baseline.");
        Assertions.assertEquals("A", zscoreNearest.get(0).getCandidate().getId(), "Z-score mode should change the ranking.");
    }

    private static void findNearestCustomWeightsShouldAffectRanking() {
        List<StudentProfile> profiles = List.of(
            buildProfile("T", "Target", new CognitiveScores(0, 0, 0, 0, 0, 0, 0, 0)),
            buildProfile("A", "A", new CognitiveScores(3, 0, 0, 0, 0, 0, 0, 0)),
            buildProfile("B", "B", new CognitiveScores(0, 4, 0, 0, 0, 0, 0, 0))
        );

        SimilarityAnalyzer analyzer = new SimilarityAnalyzer();
        List<SimilarityMatch> equalWeightNearest = analyzer.findNearest(
            profiles, "T", 1, SimilarityMode.RAW, List.of(), DimensionWeights.EQUAL
        );
        List<SimilarityMatch> neHeavyNearest = analyzer.findNearest(
            profiles, "T", 1, SimilarityMode.RAW, List.of(), DimensionWeights.create(10, 1, 1, 1, 1, 1, 1, 1)
        );

        Assertions.assertEquals("A", equalWeightNearest.get(0).getCandidate().getId(), "Equal weights should prefer A.");
        Assertions.assertEquals("B", neHeavyNearest.get(0).getCandidate().getId(), "Custom weights should change the ranking.");
    }

    private static StudentProfile buildProfile(String id, String name, CognitiveScores scores) {
        return new StudentProfile(id, name, "NA", scores, "INTP", "5", name);
    }
}
