using System.Globalization;
using Microsoft.Extensions.Logging;
using MbtiEnterpriseSimilarity.App.Services;

namespace MbtiEnterpriseSimilarity.App;

public static class Program
{
    public static int Main(string[] args)
    {
        var runId = Guid.NewGuid().ToString("N");
        using var loggerFactory = LoggerFactory.Create(builder =>
        {
            builder.ClearProviders();
            builder.AddJsonConsole(options =>
            {
                options.IncludeScopes = true;
                options.TimestampFormat = "yyyy-MM-ddTHH:mm:ss.fffzzz ";
            });
            builder.SetMinimumLevel(LogLevel.Information);
        });

        var logger = loggerFactory.CreateLogger("MbtiEnterpriseSimilarity");

        using var scope = logger.BeginScope(new Dictionary<string, object>
        {
            ["runId"] = runId
        });

        try
        {
            CultureInfo.CurrentCulture = CultureInfo.InvariantCulture;
            CultureInfo.CurrentUICulture = CultureInfo.InvariantCulture;

            var options = AppOptions.Parse(args);

            if (options.ShowHelp)
            {
                AppOptions.PrintUsage();
                return 0;
            }

            logger.LogInformation(
                "Analysis started for targetId={TargetId}, mode={Mode}, outputDir={OutputDir}, maxSkippedRatio={MaxSkippedRatio}",
                options.TargetId,
                options.SimilarityMode,
                options.OutputDirectory,
                options.MaxSkippedRatio);

            var repository = new CsvStudentProfileRepository();
            var loadResult = repository.Load(options.InputPath);
            var skippedRatio = CalculateSkippedRatio(loadResult);

            logger.LogInformation(
                "CSV loaded. totalRows={TotalRows}, validRows={ValidRows}, skippedRows={SkippedRows}, skippedRatio={SkippedRatio}",
                loadResult.TotalDataRows,
                loadResult.Profiles.Count,
                loadResult.SkippedRecords.Count,
                skippedRatio);

            if (skippedRatio > options.MaxSkippedRatio)
            {
                logger.LogError(
                    "Data quality gate failed. skippedRatio={SkippedRatio} exceeded threshold={Threshold}",
                    skippedRatio,
                    options.MaxSkippedRatio);

                Console.Error.WriteLine(
                    $"[DATA QUALITY ERROR] skipped ratio {skippedRatio:F4} exceeds --max-skipped-ratio {options.MaxSkippedRatio:F4}.");
                return 1;
            }

            var analyzer = new SimilarityAnalyzer();
            var target = analyzer.FindTarget(loadResult.Profiles, options.TargetId, options.ExcludedIds);
            var nearest = analyzer.FindNearest(
                loadResult.Profiles,
                options.TargetId,
                options.TopN,
                options.SimilarityMode,
                options.ExcludedIds,
                options.Weights);

            var exporter = new ResultExporter();
            exporter.WriteConsoleSummary(
                target,
                nearest,
                loadResult,
                options.InputPath,
                options.SimilarityMode,
                options.ExcludedIds,
                options.Weights,
                skippedRatio,
                options.MaxSkippedRatio);

            Directory.CreateDirectory(options.OutputDirectory);

            var timestamp = DateTimeOffset.Now.ToString("yyyyMMdd_HHmmss", CultureInfo.InvariantCulture);
            var modeText = options.SimilarityMode.ToString().ToLowerInvariant();
            var weightsText = options.Weights.IsEqualProfile ? "equal" : "custom";
            var baseName = $"similarity_{options.TargetId}_{modeText}_{weightsText}_top{nearest.Count}_{timestamp}";
            var markdownPath = Path.Combine(options.OutputDirectory, $"{baseName}.md");
            var jsonPath = Path.Combine(options.OutputDirectory, $"{baseName}.json");

            exporter.WriteMarkdownReport(
                markdownPath,
                target,
                nearest,
                loadResult,
                options.InputPath,
                options.SimilarityMode,
                options.ExcludedIds,
                options.Weights,
                skippedRatio,
                options.MaxSkippedRatio);

            exporter.WriteJsonReport(
                jsonPath,
                target,
                nearest,
                loadResult,
                options.InputPath,
                options.SimilarityMode,
                options.ExcludedIds,
                options.Weights,
                skippedRatio,
                options.MaxSkippedRatio);

            Console.WriteLine($"Markdown report saved: {markdownPath}");
            Console.WriteLine($"JSON report saved    : {jsonPath}");
            logger.LogInformation("Analysis completed successfully. markdownReport={MarkdownPath}, jsonReport={JsonPath}", markdownPath, jsonPath);

            return 0;
        }
        catch (ArgumentException ex)
        {
            logger.LogWarning(ex, "Input validation failed");
            Console.Error.WriteLine($"[INPUT ERROR] {ex.Message}");
            Console.WriteLine();
            AppOptions.PrintUsage();
            return 1;
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Unhandled error during analysis");
            Console.Error.WriteLine($"[ERROR] {ex.Message}");
            return 1;
        }
    }

    private static double CalculateSkippedRatio(LoadResult loadResult)
    {
        if (loadResult.TotalDataRows <= 0)
        {
            return 0d;
        }

        return (double)loadResult.SkippedRecords.Count / loadResult.TotalDataRows;
    }
}
