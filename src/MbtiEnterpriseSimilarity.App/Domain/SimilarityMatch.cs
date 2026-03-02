namespace MbtiEnterpriseSimilarity.App.Domain;

public sealed record SimilarityMatch(
    StudentProfile Candidate,
    double Distance,
    IReadOnlyDictionary<string, double> AbsoluteDifferences);
