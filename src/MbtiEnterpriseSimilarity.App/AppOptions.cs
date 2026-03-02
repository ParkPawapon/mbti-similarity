using System.Globalization;
using MbtiEnterpriseSimilarity.App.Domain;

namespace MbtiEnterpriseSimilarity.App;

public sealed record AppOptions(
    string InputPath,
    string TargetId,
    int TopN,
    string OutputDirectory,
    double MaxSkippedRatio,
    SimilarityMode SimilarityMode,
    DimensionWeights Weights,
    IReadOnlyList<string> ExcludedIds,
    bool ShowHelp)
{
    public static AppOptions Parse(string[] args)
    {
        if (args.Length == 0)
        {
            return new AppOptions(string.Empty, string.Empty, 5, string.Empty, 1d, SimilarityMode.Raw, DimensionWeights.Equal, [], true);
        }

        string? inputPath = null;
        string? targetId = null;
        var topN = 5;
        string? outputDirectory = null;
        var maxSkippedRatio = 1d;
        var mode = SimilarityMode.Raw;
        var weights = DimensionWeights.Equal;
        var excludedIds = new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        for (var i = 0; i < args.Length; i++)
        {
            var current = args[i];

            switch (current)
            {
                case "--help":
                case "-h":
                    return new AppOptions(string.Empty, string.Empty, 5, string.Empty, 1d, SimilarityMode.Raw, DimensionWeights.Equal, [], true);

                case "--input":
                    inputPath = ReadRequiredValue(args, ref i, "--input");
                    break;

                case "--target-id":
                    targetId = ReadRequiredValue(args, ref i, "--target-id");
                    break;

                case "--top":
                    var topRaw = ReadRequiredValue(args, ref i, "--top");
                    if (!int.TryParse(topRaw, out topN) || topN <= 0)
                    {
                        throw new ArgumentException("--top must be a positive integer.");
                    }

                    break;

                case "--output-dir":
                    outputDirectory = ReadRequiredValue(args, ref i, "--output-dir");
                    break;

                case "--max-skipped-ratio":
                    var skippedRatioRaw = ReadRequiredValue(args, ref i, "--max-skipped-ratio");
                    maxSkippedRatio = ParseMaxSkippedRatio(skippedRatioRaw);
                    break;

                case "--exclude-id":
                    var excludeRaw = ReadRequiredValue(args, ref i, "--exclude-id");
                    AddExcludedIds(excludedIds, excludeRaw);
                    break;

                case "--mode":
                    var modeRaw = ReadRequiredValue(args, ref i, "--mode");
                    mode = ParseMode(modeRaw);
                    break;

                case "--weights":
                    var weightsRaw = ReadRequiredValue(args, ref i, "--weights");
                    weights = DimensionWeights.Parse(weightsRaw);
                    break;

                default:
                    throw new ArgumentException($"Unknown argument: {current}");
            }
        }

        if (string.IsNullOrWhiteSpace(inputPath))
        {
            throw new ArgumentException("--input is required.");
        }

        if (string.IsNullOrWhiteSpace(targetId))
        {
            throw new ArgumentException("--target-id is required.");
        }

        outputDirectory ??= Path.Combine(Environment.CurrentDirectory, "output");

        return new AppOptions(
            InputPath: Path.GetFullPath(inputPath),
            TargetId: targetId.Trim(),
            TopN: topN,
            OutputDirectory: Path.GetFullPath(outputDirectory),
            MaxSkippedRatio: maxSkippedRatio,
            SimilarityMode: mode,
            Weights: weights,
            ExcludedIds: excludedIds.OrderBy(id => id, StringComparer.Ordinal).ToList(),
            ShowHelp: false);
    }

    public static void PrintUsage()
    {
        Console.WriteLine("MBTI Enterprise Similarity Analyzer");
        Console.WriteLine();
        Console.WriteLine("Usage:");
        Console.WriteLine("  dotnet run --project src/MbtiEnterpriseSimilarity.App -- \\");
        Console.WriteLine("    --input <csv-path> --target-id <student-id> [--top 5] [--output-dir <folder>] [--max-skipped-ratio 0.2] [--exclude-id <id>] [--mode raw|zscore] [--weights Ne=1,Ni=1,Te=1,Ti=1,Se=1,Si=1,Fe=1,Fi=1]");
        Console.WriteLine();
        Console.WriteLine("Examples:");
        Console.WriteLine("  dotnet run --project src/MbtiEnterpriseSimilarity.App -- \\");
        Console.WriteLine("    --input \"./data/CSS121_MBTI_2026_68.csv\" --target-id 68090500418 --top 5 --max-skipped-ratio 0.2 --exclude-id 99999999999 --mode zscore");
        Console.WriteLine("  dotnet run --project src/MbtiEnterpriseSimilarity.App -- \\");
        Console.WriteLine("    --input \"./data/CSS121_MBTI_2026_68.csv\" --target-id 68090500418 --mode zscore --weights \"Ne=1.2,Ni=1.2,Te=1,Ti=1,Se=0.8,Si=0.8,Fe=1,Fi=1\"");
    }

    private static string ReadRequiredValue(string[] args, ref int index, string flag)
    {
        if (index + 1 >= args.Length)
        {
            throw new ArgumentException($"{flag} requires a value.");
        }

        index++;
        return args[index];
    }

    private static void AddExcludedIds(ISet<string> destination, string rawValue)
    {
        foreach (var token in rawValue.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            if (!string.IsNullOrWhiteSpace(token))
            {
                destination.Add(token);
            }
        }
    }

    private static SimilarityMode ParseMode(string rawValue)
    {
        if (rawValue.Equals("raw", StringComparison.OrdinalIgnoreCase))
        {
            return SimilarityMode.Raw;
        }

        if (rawValue.Equals("zscore", StringComparison.OrdinalIgnoreCase) ||
            rawValue.Equals("z-score", StringComparison.OrdinalIgnoreCase))
        {
            return SimilarityMode.ZScore;
        }

        throw new ArgumentException("--mode must be either 'raw' or 'zscore'.");
    }

    private static double ParseMaxSkippedRatio(string rawValue)
    {
        if (!double.TryParse(rawValue, NumberStyles.Float, CultureInfo.InvariantCulture, out var ratio))
        {
            throw new ArgumentException("--max-skipped-ratio must be a number between 0 and 1.");
        }

        if (double.IsNaN(ratio) || double.IsInfinity(ratio) || ratio < 0 || ratio > 1)
        {
            throw new ArgumentException("--max-skipped-ratio must be in range [0, 1].");
        }

        return ratio;
    }
}
