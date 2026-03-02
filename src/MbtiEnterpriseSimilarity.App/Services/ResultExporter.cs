using System.Globalization;
using System.Text;
using System.Text.Json;
using MbtiEnterpriseSimilarity.App.Domain;

namespace MbtiEnterpriseSimilarity.App.Services;

public sealed class ResultExporter
{
    public void WriteConsoleSummary(
        StudentProfile target,
        IReadOnlyList<SimilarityMatch> matches,
        LoadResult loadResult,
        string sourcePath,
        SimilarityMode mode,
        IReadOnlyCollection<string> excludedIds,
        DimensionWeights weights,
        double skippedRatio,
        double maxSkippedRatio)
    {
        var normalizedExcludedIds = NormalizeExcludedIds(excludedIds);

        Console.WriteLine("=== MBTI Cognitive Similarity Report ===");
        Console.WriteLine($"Source CSV      : {sourcePath}");
        Console.WriteLine($"Similarity mode : {mode}");
        Console.WriteLine($"Weights (norm)  : {weights.ToCompactDisplay()}");
        Console.WriteLine($"Data quality    : skippedRatio={FormatNumber(skippedRatio, 4)}, threshold={FormatNumber(maxSkippedRatio, 4)}");
        Console.WriteLine($"Excluded IDs    : {(normalizedExcludedIds.Count > 0 ? string.Join(", ", normalizedExcludedIds) : "(none)")}");
        Console.WriteLine($"Target          : {target.Id} | {target.Name} ({target.Nick})");
        Console.WriteLine($"Type/Enneagram  : {target.Type} / {target.Enneagram}");
        Console.WriteLine($"Rows loaded     : {loadResult.Profiles.Count}/{loadResult.TotalDataRows} valid");
        Console.WriteLine($"Rows skipped    : {loadResult.SkippedRecords.Count}");

        if (loadResult.SkippedRecords.Count > 0)
        {
            var preview = string.Join(", ", loadResult.SkippedRecords.Take(5).Select(record => record.Id));
            Console.WriteLine($"Skipped IDs     : {preview}{(loadResult.SkippedRecords.Count > 5 ? ", ..." : string.Empty)}");
        }

        Console.WriteLine();
        Console.WriteLine($"{"Rank",-4} {"ID",-11} {"Name",-26} {"Type",-5} {"Distance",9}");
        Console.WriteLine(new string('-', 62));

        for (var i = 0; i < matches.Count; i++)
        {
            var match = matches[i];
            var shortName = Truncate(match.Candidate.Name, 26);
            Console.WriteLine($"{i + 1,-4} {match.Candidate.Id,-11} {shortName,-26} {match.Candidate.Type,-5} {FormatNumber(match.Distance, 4),9}");
        }

        Console.WriteLine();
    }

    public void WriteMarkdownReport(
        string outputPath,
        StudentProfile target,
        IReadOnlyList<SimilarityMatch> matches,
        LoadResult loadResult,
        string sourcePath,
        SimilarityMode mode,
        IReadOnlyCollection<string> excludedIds,
        DimensionWeights weights,
        double skippedRatio,
        double maxSkippedRatio)
    {
        var normalizedExcludedIds = NormalizeExcludedIds(excludedIds);

        var sb = new StringBuilder();
        sb.AppendLine("# MBTI Cognitive Similarity Report");
        sb.AppendLine();
        sb.AppendLine($"- Generated at: {DateTimeOffset.Now.ToString("yyyy-MM-dd HH:mm:ss zzz", CultureInfo.InvariantCulture)}");
        sb.AppendLine($"- Source CSV: `{sourcePath}`");
        sb.AppendLine($"- Similarity Mode: `{mode}`");
        sb.AppendLine($"- Weights (normalized): {string.Join(", ", weights.ToDictionary().Select(pair => $"`{pair.Key}={FormatNumber(pair.Value, 4)}`"))}");
        sb.AppendLine($"- Data Quality Threshold: `{FormatNumber(maxSkippedRatio, 4)}`");
        sb.AppendLine($"- Data Quality Actual Skipped Ratio: `{FormatNumber(skippedRatio, 4)}`");
        sb.AppendLine($"- Excluded IDs: {(normalizedExcludedIds.Count > 0 ? string.Join(", ", normalizedExcludedIds.Select(id => $"`{id}`")) : "(none)")}");
        sb.AppendLine($"- Target ID: `{target.Id}`");
        sb.AppendLine($"- Target Name: {target.Name} ({target.Nick})");
        sb.AppendLine($"- Target Type: {target.Type}");
        sb.AppendLine($"- Total Rows: {loadResult.TotalDataRows}");
        sb.AppendLine($"- Valid Rows: {loadResult.Profiles.Count}");
        sb.AppendLine($"- Skipped Rows: {loadResult.SkippedRecords.Count}");
        sb.AppendLine();

        sb.AppendLine("## Top Similar Profiles");
        sb.AppendLine();
        sb.AppendLine("| Rank | ID | Name | Type | Distance | NeΔ | NiΔ | TeΔ | TiΔ | SeΔ | SiΔ | FeΔ | FiΔ |");
        sb.AppendLine("| ---: | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |");

        for (var i = 0; i < matches.Count; i++)
        {
            var match = matches[i];
            sb.AppendLine(
                $"| {i + 1} | {match.Candidate.Id} | {EscapePipe(match.Candidate.Name)} | {match.Candidate.Type} | {FormatNumber(match.Distance, 4)} | " +
                $"{FormatNumber(match.AbsoluteDifferences["Ne"], 2)} | {FormatNumber(match.AbsoluteDifferences["Ni"], 2)} | {FormatNumber(match.AbsoluteDifferences["Te"], 2)} | {FormatNumber(match.AbsoluteDifferences["Ti"], 2)} | " +
                $"{FormatNumber(match.AbsoluteDifferences["Se"], 2)} | {FormatNumber(match.AbsoluteDifferences["Si"], 2)} | {FormatNumber(match.AbsoluteDifferences["Fe"], 2)} | {FormatNumber(match.AbsoluteDifferences["Fi"], 2)} |");
        }

        if (loadResult.SkippedRecords.Count > 0)
        {
            sb.AppendLine();
            sb.AppendLine("## Skipped Rows (Missing/Invalid Scores)");
            sb.AppendLine();
            sb.AppendLine("| Line | ID | Reason |");
            sb.AppendLine("| ---: | --- | --- |");
            foreach (var skipped in loadResult.SkippedRecords)
            {
                sb.AppendLine($"| {skipped.LineNumber} | {skipped.Id} | {EscapePipe(skipped.Reason)} |");
            }
        }

        File.WriteAllText(outputPath, sb.ToString(), Encoding.UTF8);
    }

    public void WriteJsonReport(
        string outputPath,
        StudentProfile target,
        IReadOnlyList<SimilarityMatch> matches,
        LoadResult loadResult,
        string sourcePath,
        SimilarityMode mode,
        IReadOnlyCollection<string> excludedIds,
        DimensionWeights weights,
        double skippedRatio,
        double maxSkippedRatio)
    {
        var normalizedExcludedIds = NormalizeExcludedIds(excludedIds);

        var payload = new
        {
            generatedAt = DateTimeOffset.Now,
            sourceCsv = sourcePath,
            analysis = new
            {
                similarityMode = mode.ToString(),
                normalizedWeights = weights.ToDictionary(),
                maxSkippedRatio = maxSkippedRatio,
                actualSkippedRatio = skippedRatio,
                excludedIds = normalizedExcludedIds
            },
            summary = new
            {
                totalRows = loadResult.TotalDataRows,
                validRows = loadResult.Profiles.Count,
                skippedRows = loadResult.SkippedRecords.Count
            },
            target = new
            {
                target.Id,
                target.Name,
                target.Nick,
                target.Type,
                target.Enneagram,
                target.Sex,
                scores = target.Scores.ToDictionary()
            },
            topMatches = matches.Select((match, index) => new
            {
                rank = index + 1,
                id = match.Candidate.Id,
                name = match.Candidate.Name,
                nick = match.Candidate.Nick,
                type = match.Candidate.Type,
                enneagram = match.Candidate.Enneagram,
                distance = Math.Round(match.Distance, 6),
                absoluteDifferences = match.AbsoluteDifferences
            }),
            skipped = loadResult.SkippedRecords
        };

        var json = JsonSerializer.Serialize(payload, new JsonSerializerOptions
        {
            WriteIndented = true
        });

        File.WriteAllText(outputPath, json, Encoding.UTF8);
    }

    private static string EscapePipe(string value) => value.Replace("|", "\\|");

    private static string Truncate(string value, int maxLength)
    {
        if (value.Length <= maxLength)
        {
            return value;
        }

        return value[..(maxLength - 3)] + "...";
    }

    private static string FormatNumber(double value, int decimals) =>
        value.ToString($"F{decimals}", CultureInfo.InvariantCulture);

    private static IReadOnlyList<string> NormalizeExcludedIds(IReadOnlyCollection<string> excludedIds) =>
        excludedIds
            .Where(id => !string.IsNullOrWhiteSpace(id))
            .Select(id => id.Trim())
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .OrderBy(id => id, StringComparer.Ordinal)
            .ToList();
}
