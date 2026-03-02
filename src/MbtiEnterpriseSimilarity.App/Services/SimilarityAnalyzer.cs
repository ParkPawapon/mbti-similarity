using MbtiEnterpriseSimilarity.App.Domain;

namespace MbtiEnterpriseSimilarity.App.Services;

public sealed class SimilarityAnalyzer
{
    public StudentProfile FindTarget(IReadOnlyCollection<StudentProfile> profiles, string targetId)
    {
        if (profiles.Count == 0)
        {
            throw new InvalidOperationException("No valid profiles available for analysis.");
        }

        if (string.IsNullOrWhiteSpace(targetId))
        {
            throw new ArgumentException("Target ID is required.", nameof(targetId));
        }

        var target = profiles.FirstOrDefault(p => p.Id.Equals(targetId, StringComparison.OrdinalIgnoreCase));

        return target ?? throw new InvalidOperationException($"Target ID '{targetId}' not found in valid profiles.");
    }

    public StudentProfile FindTarget(
        IReadOnlyCollection<StudentProfile> profiles,
        string targetId,
        IReadOnlyCollection<string> excludedIds)
    {
        var exclusionSet = BuildExclusionSet(excludedIds);
        var normalizedTargetId = targetId.Trim();

        if (exclusionSet.Contains(normalizedTargetId))
        {
            throw new InvalidOperationException($"Target ID '{normalizedTargetId}' is excluded by --exclude-id.");
        }

        var analysisProfiles = ApplyExclusions(profiles, exclusionSet);
        return FindTarget(analysisProfiles, normalizedTargetId);
    }

    public IReadOnlyList<SimilarityMatch> FindNearest(
        IReadOnlyCollection<StudentProfile> profiles,
        string targetId,
        int topN)
        => FindNearest(profiles, targetId, topN, SimilarityMode.Raw, [], DimensionWeights.Equal);

    public IReadOnlyList<SimilarityMatch> FindNearest(
        IReadOnlyCollection<StudentProfile> profiles,
        string targetId,
        int topN,
        SimilarityMode mode,
        IReadOnlyCollection<string> excludedIds)
        => FindNearest(profiles, targetId, topN, mode, excludedIds, DimensionWeights.Equal);

    public IReadOnlyList<SimilarityMatch> FindNearest(
        IReadOnlyCollection<StudentProfile> profiles,
        string targetId,
        int topN,
        SimilarityMode mode,
        IReadOnlyCollection<string> excludedIds,
        DimensionWeights weights)
    {
        if (topN <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(topN), "Top N must be greater than 0.");
        }

        var exclusionSet = BuildExclusionSet(excludedIds);
        var normalizedTargetId = targetId.Trim();

        if (exclusionSet.Contains(normalizedTargetId))
        {
            throw new InvalidOperationException($"Target ID '{normalizedTargetId}' is excluded by --exclude-id.");
        }

        var analysisProfiles = ApplyExclusions(profiles, exclusionSet);
        var target = FindTarget(analysisProfiles, normalizedTargetId);
        var scoreSpace = mode switch
        {
            SimilarityMode.Raw => BuildRawScoreMap(analysisProfiles),
            SimilarityMode.ZScore => BuildZScoreMap(analysisProfiles),
            _ => throw new ArgumentOutOfRangeException(nameof(mode), mode, "Unsupported similarity mode.")
        };

        var nearest = analysisProfiles
            .Where(profile => !profile.Id.Equals(target.Id, StringComparison.OrdinalIgnoreCase))
            .Select(profile =>
            {
                var distance = scoreSpace[target.Id].WeightedEuclideanDistanceTo(scoreSpace[profile.Id], weights);
                var differences = target.Scores.AbsoluteDifferenceFrom(profile.Scores);
                return new SimilarityMatch(profile, distance, differences);
            })
            .OrderBy(match => match.Distance)
            .ThenBy(match => match.Candidate.Id, StringComparer.Ordinal)
            .Take(topN)
            .ToList();

        return nearest;
    }

    private static IReadOnlyDictionary<string, CognitiveScores> BuildRawScoreMap(IReadOnlyCollection<StudentProfile> profiles)
    {
        var map = new Dictionary<string, CognitiveScores>(StringComparer.OrdinalIgnoreCase);

        foreach (var profile in profiles)
        {
            if (!map.TryAdd(profile.Id, profile.Scores))
            {
                throw new InvalidOperationException($"Duplicate ID found in dataset: '{profile.Id}'.");
            }
        }

        return map;
    }

    private static IReadOnlyDictionary<string, CognitiveScores> BuildZScoreMap(IReadOnlyCollection<StudentProfile> profiles)
    {
        var profileList = profiles.ToList();

        if (profileList.Count < 2)
        {
            throw new InvalidOperationException("At least 2 valid profiles are required for z-score normalization.");
        }

        var matrix = profileList
            .Select(profile => ToVector(profile.Scores))
            .ToList();

        var mean = new double[8];
        var stddev = new double[8];

        for (var dim = 0; dim < 8; dim++)
        {
            mean[dim] = matrix.Select(vector => vector[dim]).Average();
            var variance = matrix
                .Select(vector => Math.Pow(vector[dim] - mean[dim], 2))
                .Average();
            stddev[dim] = Math.Sqrt(variance);
        }

        var zScoreMap = new Dictionary<string, CognitiveScores>(StringComparer.OrdinalIgnoreCase);

        for (var i = 0; i < profileList.Count; i++)
        {
            var source = matrix[i];
            var z = new double[8];

            for (var dim = 0; dim < 8; dim++)
            {
                z[dim] = stddev[dim] > 1e-12
                    ? (source[dim] - mean[dim]) / stddev[dim]
                    : 0d;
            }

            if (!zScoreMap.TryAdd(profileList[i].Id, FromVector(z)))
            {
                throw new InvalidOperationException($"Duplicate ID found in dataset: '{profileList[i].Id}'.");
            }
        }

        return zScoreMap;
    }

    private static double[] ToVector(CognitiveScores scores) =>
    [
        scores.Ne,
        scores.Ni,
        scores.Te,
        scores.Ti,
        scores.Se,
        scores.Si,
        scores.Fe,
        scores.Fi
    ];

    private static CognitiveScores FromVector(IReadOnlyList<double> vector) => new(
        vector[0],
        vector[1],
        vector[2],
        vector[3],
        vector[4],
        vector[5],
        vector[6],
        vector[7]);

    private static IReadOnlyList<StudentProfile> ApplyExclusions(
        IReadOnlyCollection<StudentProfile> profiles,
        ISet<string> exclusionSet)
    {
        if (profiles.Count == 0)
        {
            throw new InvalidOperationException("No valid profiles available for analysis.");
        }

        var filtered = profiles
            .Where(profile => !exclusionSet.Contains(profile.Id))
            .ToList();

        if (filtered.Count == 0)
        {
            throw new InvalidOperationException("No profiles remain after applying --exclude-id filters.");
        }

        return filtered;
    }

    private static HashSet<string> BuildExclusionSet(IReadOnlyCollection<string> excludedIds)
    {
        var set = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        foreach (var id in excludedIds)
        {
            if (!string.IsNullOrWhiteSpace(id))
            {
                set.Add(id.Trim());
            }
        }

        return set;
    }
}
