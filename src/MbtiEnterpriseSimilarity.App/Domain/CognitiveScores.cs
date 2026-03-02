namespace MbtiEnterpriseSimilarity.App.Domain;

public sealed record CognitiveScores(
    double Ne,
    double Ni,
    double Te,
    double Ti,
    double Se,
    double Si,
    double Fe,
    double Fi)
{
    public static IReadOnlyList<string> Dimensions { get; } =
    [
        "Ne", "Ni", "Te", "Ti", "Se", "Si", "Fe", "Fi"
    ];

    public IReadOnlyDictionary<string, double> ToDictionary() => new Dictionary<string, double>
    {
        ["Ne"] = Ne,
        ["Ni"] = Ni,
        ["Te"] = Te,
        ["Ti"] = Ti,
        ["Se"] = Se,
        ["Si"] = Si,
        ["Fe"] = Fe,
        ["Fi"] = Fi
    };

    public double EuclideanDistanceTo(CognitiveScores other)
    {
        var sumSquares =
            Math.Pow(Ne - other.Ne, 2) +
            Math.Pow(Ni - other.Ni, 2) +
            Math.Pow(Te - other.Te, 2) +
            Math.Pow(Ti - other.Ti, 2) +
            Math.Pow(Se - other.Se, 2) +
            Math.Pow(Si - other.Si, 2) +
            Math.Pow(Fe - other.Fe, 2) +
            Math.Pow(Fi - other.Fi, 2);

        return Math.Sqrt(sumSquares);
    }

    public double WeightedEuclideanDistanceTo(CognitiveScores other, DimensionWeights weights)
    {
        var sumSquares =
            weights.Ne * Math.Pow(Ne - other.Ne, 2) +
            weights.Ni * Math.Pow(Ni - other.Ni, 2) +
            weights.Te * Math.Pow(Te - other.Te, 2) +
            weights.Ti * Math.Pow(Ti - other.Ti, 2) +
            weights.Se * Math.Pow(Se - other.Se, 2) +
            weights.Si * Math.Pow(Si - other.Si, 2) +
            weights.Fe * Math.Pow(Fe - other.Fe, 2) +
            weights.Fi * Math.Pow(Fi - other.Fi, 2);

        return Math.Sqrt(sumSquares);
    }

    public IReadOnlyDictionary<string, double> AbsoluteDifferenceFrom(CognitiveScores other) =>
        new Dictionary<string, double>
        {
            ["Ne"] = Math.Abs(Ne - other.Ne),
            ["Ni"] = Math.Abs(Ni - other.Ni),
            ["Te"] = Math.Abs(Te - other.Te),
            ["Ti"] = Math.Abs(Ti - other.Ti),
            ["Se"] = Math.Abs(Se - other.Se),
            ["Si"] = Math.Abs(Si - other.Si),
            ["Fe"] = Math.Abs(Fe - other.Fe),
            ["Fi"] = Math.Abs(Fi - other.Fi)
        };
}
