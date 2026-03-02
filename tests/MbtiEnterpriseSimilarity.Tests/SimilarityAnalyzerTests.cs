using MbtiEnterpriseSimilarity.App.Domain;
using MbtiEnterpriseSimilarity.App.Services;

namespace MbtiEnterpriseSimilarity.Tests;

public class SimilarityAnalyzerTests
{
    [Fact]
    public void EuclideanDistance_ShouldBeCorrect()
    {
        var a = new CognitiveScores(1, 2, 3, 4, 5, 6, 7, 8);
        var b = new CognitiveScores(1, 2, 3, 4, 5, 6, 7, 10);

        var distance = a.EuclideanDistanceTo(b);

        Assert.Equal(2.0, distance, precision: 10);
    }

    [Fact]
    public void FindNearest_ShouldOrderByDistanceThenId()
    {
        var profiles = new List<StudentProfile>
        {
            BuildProfile("T", "Target", new CognitiveScores(0, 0, 0, 0, 0, 0, 0, 0)),
            BuildProfile("002", "A", new CognitiveScores(1, 0, 0, 0, 0, 0, 0, 0)),
            BuildProfile("001", "B", new CognitiveScores(1, 0, 0, 0, 0, 0, 0, 0)),
            BuildProfile("003", "C", new CognitiveScores(2, 0, 0, 0, 0, 0, 0, 0))
        };

        var analyzer = new SimilarityAnalyzer();
        var nearest = analyzer.FindNearest(profiles, "T", 3);

        Assert.Collection(
            nearest,
            first => Assert.Equal("001", first.Candidate.Id),
            second => Assert.Equal("002", second.Candidate.Id),
            third => Assert.Equal("003", third.Candidate.Id));
    }

    [Fact]
    public void FindNearest_ShouldRespectExcludedIds()
    {
        var profiles = new List<StudentProfile>
        {
            BuildProfile("T", "Target", new CognitiveScores(0, 0, 0, 0, 0, 0, 0, 0)),
            BuildProfile("001", "A", new CognitiveScores(1, 0, 0, 0, 0, 0, 0, 0)),
            BuildProfile("002", "B", new CognitiveScores(2, 0, 0, 0, 0, 0, 0, 0))
        };

        var analyzer = new SimilarityAnalyzer();
        var nearest = analyzer.FindNearest(profiles, "T", 2, SimilarityMode.Raw, ["001"]);

        Assert.Single(nearest);
        Assert.Equal("002", nearest[0].Candidate.Id);
    }

    [Fact]
    public void FindNearest_ZScoreMode_ShouldChangeRankingComparedToRaw()
    {
        var profiles = new List<StudentProfile>
        {
            BuildProfile("T", "Target", new CognitiveScores(1000, 0, 0, 0, 0, 0, 0, 0)),
            BuildProfile("A", "A", new CognitiveScores(1010, 0, 0, 0, 0, 0, 0, 0)),
            BuildProfile("B", "B", new CognitiveScores(1000, 5, 0, 0, 0, 0, 0, 0)),
            BuildProfile("C", "C", new CognitiveScores(2000, 0, 0, 0, 0, 0, 0, 0))
        };

        var analyzer = new SimilarityAnalyzer();

        var rawNearest = analyzer.FindNearest(profiles, "T", 1, SimilarityMode.Raw, []);
        var zScoreNearest = analyzer.FindNearest(profiles, "T", 1, SimilarityMode.ZScore, []);

        Assert.Equal("B", rawNearest[0].Candidate.Id);
        Assert.Equal("A", zScoreNearest[0].Candidate.Id);
    }

    [Fact]
    public void FindNearest_CustomWeights_ShouldAffectRanking()
    {
        var profiles = new List<StudentProfile>
        {
            BuildProfile("T", "Target", new CognitiveScores(0, 0, 0, 0, 0, 0, 0, 0)),
            BuildProfile("A", "A", new CognitiveScores(3, 0, 0, 0, 0, 0, 0, 0)),
            BuildProfile("B", "B", new CognitiveScores(0, 4, 0, 0, 0, 0, 0, 0))
        };

        var analyzer = new SimilarityAnalyzer();

        var equalWeightNearest = analyzer.FindNearest(
            profiles,
            "T",
            1,
            SimilarityMode.Raw,
            [],
            DimensionWeights.Equal);

        var neHeavyNearest = analyzer.FindNearest(
            profiles,
            "T",
            1,
            SimilarityMode.Raw,
            [],
            DimensionWeights.Create(10, 1, 1, 1, 1, 1, 1, 1));

        Assert.Equal("A", equalWeightNearest[0].Candidate.Id);
        Assert.Equal("B", neHeavyNearest[0].Candidate.Id);
    }

    private static StudentProfile BuildProfile(string id, string name, CognitiveScores scores)
        => new(id, name, "NA", scores, "INTP", "5", name);
}
