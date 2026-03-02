using MbtiEnterpriseSimilarity.App.Domain;

namespace MbtiEnterpriseSimilarity.App.Services;

public sealed record SkippedRecord(int LineNumber, string Id, string Reason);

public sealed record LoadResult(
    IReadOnlyList<StudentProfile> Profiles,
    IReadOnlyList<SkippedRecord> SkippedRecords,
    int TotalDataRows);
