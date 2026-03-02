using MbtiEnterpriseSimilarity.App.Domain;

namespace MbtiEnterpriseSimilarity.Tests;

public class DimensionWeightsTests
{
    [Fact]
    public void Create_ShouldNormalizeWeightsToOne()
    {
        var weights = DimensionWeights.Create(2, 2, 1, 1, 1, 1, 1, 1);

        var sum = weights.ToDictionary().Values.Sum();

        Assert.Equal(1.0, sum, precision: 10);
        Assert.Equal(0.2, weights.Ne, precision: 10);
        Assert.Equal(0.1, weights.Te, precision: 10);
    }

    [Fact]
    public void Create_ShouldRejectAllZeroWeights()
    {
        var ex = Assert.Throws<ArgumentException>(() => DimensionWeights.Create(0, 0, 0, 0, 0, 0, 0, 0));

        Assert.Contains("At least one weight", ex.Message);
    }
}
