namespace MbtiEnterpriseSimilarity.App.Domain;

public sealed record StudentProfile(
    string Id,
    string Name,
    string Sex,
    CognitiveScores Scores,
    string Type,
    string Enneagram,
    string Nick);
