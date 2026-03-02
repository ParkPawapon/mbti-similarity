using MbtiEnterpriseSimilarity.App;
using MbtiEnterpriseSimilarity.App.Domain;

namespace MbtiEnterpriseSimilarity.Tests;

public class AppOptionsTests
{
    [Fact]
    public void Parse_ShouldSupportExcludeIdsAndZScoreMode()
    {
        var args = new[]
        {
            "--input", "./data.csv",
            "--target-id", "68090500418",
            "--exclude-id", "99999999999",
            "--exclude-id", "68090500001,68090500002",
            "--mode", "zscore",
            "--weights", "Ne=2,Ni=2,Te=1,Ti=1,Se=1,Si=1,Fe=1,Fi=1"
        };

        var options = AppOptions.Parse(args);

        Assert.Equal(SimilarityMode.ZScore, options.SimilarityMode);
        Assert.Equal(new[] { "68090500001", "68090500002", "99999999999" }, options.ExcludedIds);
        Assert.Equal(0.2, options.Weights.Ne, precision: 10);
        Assert.Equal(0.1, options.Weights.Te, precision: 10);
    }

    [Fact]
    public void Parse_ShouldRejectUnknownMode()
    {
        var args = new[]
        {
            "--input", "./data.csv",
            "--target-id", "68090500418",
            "--mode", "weighted"
        };

        var ex = Assert.Throws<ArgumentException>(() => AppOptions.Parse(args));

        Assert.Contains("--mode", ex.Message);
    }

    [Fact]
    public void Parse_ShouldRejectUnknownWeightDimension()
    {
        var args = new[]
        {
            "--input", "./data.csv",
            "--target-id", "68090500418",
            "--weights", "XX=2"
        };

        var ex = Assert.Throws<ArgumentException>(() => AppOptions.Parse(args));

        Assert.Contains("Unknown dimension", ex.Message);
    }

    [Fact]
    public void Parse_ShouldRejectInvalidMaxSkippedRatio()
    {
        var args = new[]
        {
            "--input", "./data.csv",
            "--target-id", "68090500418",
            "--max-skipped-ratio", "1.2"
        };

        var ex = Assert.Throws<ArgumentException>(() => AppOptions.Parse(args));

        Assert.Contains("--max-skipped-ratio", ex.Message);
    }
}
