package com.mbti.similarity.service;

import com.mbti.similarity.domain.CognitiveScores;
import com.mbti.similarity.domain.DimensionWeights;
import com.mbti.similarity.domain.SimilarityMatch;
import com.mbti.similarity.domain.SimilarityMode;
import com.mbti.similarity.domain.StudentProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class SimilarityAnalyzer {
    public StudentProfile findTarget(Collection<StudentProfile> profiles, String targetId) {
        List<StudentProfile> profileList = List.copyOf(Objects.requireNonNull(profiles, "profiles"));
        if (profileList.isEmpty()) {
            throw new IllegalStateException("No valid profiles available for analysis.");
        }

        String normalizedTargetId = normalizeId(targetId, "Target ID is required.");
        for (StudentProfile profile : profileList) {
            if (profile.getId().equalsIgnoreCase(normalizedTargetId)) {
                return profile;
            }
        }

        throw new IllegalStateException("Target ID '" + normalizedTargetId + "' not found in valid profiles.");
    }

    public StudentProfile findTarget(Collection<StudentProfile> profiles, String targetId, Collection<String> excludedIds) {
        Set<String> exclusionSet = buildExclusionSet(excludedIds);
        String normalizedTargetId = normalizeId(targetId, "Target ID is required.");
        if (exclusionSet.contains(normalizedTargetId.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Target ID '" + normalizedTargetId + "' is excluded by --exclude-id.");
        }

        return findTarget(applyExclusions(profiles, exclusionSet), normalizedTargetId);
    }

    public List<SimilarityMatch> findNearest(
        Collection<StudentProfile> profiles,
        String targetId,
        int topN,
        SimilarityMode mode,
        Collection<String> excludedIds,
        DimensionWeights weights
    ) {
        if (topN <= 0) {
            throw new IllegalArgumentException("Top N must be greater than 0.");
        }

        Set<String> exclusionSet = buildExclusionSet(excludedIds);
        String normalizedTargetId = normalizeId(targetId, "Target ID is required.");
        if (exclusionSet.contains(normalizedTargetId.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Target ID '" + normalizedTargetId + "' is excluded by --exclude-id.");
        }

        List<StudentProfile> analysisProfiles = applyExclusions(profiles, exclusionSet);
        StudentProfile target = findTarget(analysisProfiles, normalizedTargetId);
        Map<String, CognitiveScores> scoreSpace = switch (Objects.requireNonNull(mode, "mode")) {
            case RAW -> buildRawScoreMap(analysisProfiles);
            case ZSCORE -> buildZScoreMap(analysisProfiles);
        };

        List<SimilarityMatch> matches = new ArrayList<>();
        for (StudentProfile profile : analysisProfiles) {
            if (profile.getId().equalsIgnoreCase(target.getId())) {
                continue;
            }

            double distance = scoreSpace.get(target.getId().toLowerCase(Locale.ROOT))
                .weightedEuclideanDistanceTo(scoreSpace.get(profile.getId().toLowerCase(Locale.ROOT)), weights);

            matches.add(new SimilarityMatch(
                profile,
                distance,
                target.getScores().absoluteDifferenceFrom(profile.getScores())
            ));
        }

        matches.sort(
            Comparator.comparingDouble(SimilarityMatch::getDistance)
                .thenComparing(match -> match.getCandidate().getId())
        );

        int limit = Math.min(topN, matches.size());
        return List.copyOf(matches.subList(0, limit));
    }

    private static Map<String, CognitiveScores> buildRawScoreMap(List<StudentProfile> profiles) {
        Map<String, CognitiveScores> scoresById = new LinkedHashMap<>();
        for (StudentProfile profile : profiles) {
            String key = profile.getId().toLowerCase(Locale.ROOT);
            if (scoresById.putIfAbsent(key, profile.getScores()) != null) {
                throw new IllegalStateException("Duplicate ID found in dataset: '" + profile.getId() + "'.");
            }
        }
        return scoresById;
    }

    private static Map<String, CognitiveScores> buildZScoreMap(List<StudentProfile> profiles) {
        if (profiles.size() < 2) {
            throw new IllegalStateException("At least 2 valid profiles are required for z-score normalization.");
        }

        List<double[]> matrix = new ArrayList<>(profiles.size());
        for (StudentProfile profile : profiles) {
            matrix.add(profile.getScores().toVector());
        }

        double[] mean = new double[CognitiveScores.DIMENSIONS.size()];
        double[] stddev = new double[CognitiveScores.DIMENSIONS.size()];

        for (int dimension = 0; dimension < CognitiveScores.DIMENSIONS.size(); dimension++) {
            double sum = 0d;
            for (double[] vector : matrix) {
                sum += vector[dimension];
            }
            mean[dimension] = sum / matrix.size();

            double varianceSum = 0d;
            for (double[] vector : matrix) {
                double diff = vector[dimension] - mean[dimension];
                varianceSum += diff * diff;
            }
            stddev[dimension] = Math.sqrt(varianceSum / matrix.size());
        }

        Map<String, CognitiveScores> zScoreMap = new LinkedHashMap<>();
        for (int rowIndex = 0; rowIndex < profiles.size(); rowIndex++) {
            double[] source = matrix.get(rowIndex);
            double[] z = new double[CognitiveScores.DIMENSIONS.size()];
            for (int dimension = 0; dimension < CognitiveScores.DIMENSIONS.size(); dimension++) {
                z[dimension] = stddev[dimension] > 1e-12
                    ? (source[dimension] - mean[dimension]) / stddev[dimension]
                    : 0d;
            }

            StudentProfile profile = profiles.get(rowIndex);
            String key = profile.getId().toLowerCase(Locale.ROOT);
            if (zScoreMap.putIfAbsent(key, CognitiveScores.fromVector(z)) != null) {
                throw new IllegalStateException("Duplicate ID found in dataset: '" + profile.getId() + "'.");
            }
        }

        return zScoreMap;
    }

    private static List<StudentProfile> applyExclusions(Collection<StudentProfile> profiles, Set<String> exclusionSet) {
        List<StudentProfile> profileList = List.copyOf(Objects.requireNonNull(profiles, "profiles"));
        if (profileList.isEmpty()) {
            throw new IllegalStateException("No valid profiles available for analysis.");
        }

        List<StudentProfile> filtered = new ArrayList<>();
        for (StudentProfile profile : profileList) {
            if (!exclusionSet.contains(profile.getId().toLowerCase(Locale.ROOT))) {
                filtered.add(profile);
            }
        }

        if (filtered.isEmpty()) {
            throw new IllegalStateException("No profiles remain after applying --exclude-id filters.");
        }

        return filtered;
    }

    private static Set<String> buildExclusionSet(Collection<String> excludedIds) {
        Set<String> exclusionSet = new LinkedHashSet<>();
        if (excludedIds == null) {
            return exclusionSet;
        }

        for (String id : excludedIds) {
            if (id != null && !id.trim().isEmpty()) {
                exclusionSet.add(id.trim().toLowerCase(Locale.ROOT));
            }
        }

        return exclusionSet;
    }

    private static String normalizeId(String id, String message) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return id.trim();
    }
}
