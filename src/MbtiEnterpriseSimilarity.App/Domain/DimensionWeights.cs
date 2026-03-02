using System.Globalization;

namespace MbtiEnterpriseSimilarity.App.Domain;

public sealed record DimensionWeights(
    double Ne,
    double Ni,
    double Te,
    double Ti,
    double Se,
    double Si,
    double Fe,
    double Fi)
{
    private const double Epsilon = 1e-12;

    public static DimensionWeights Equal { get; } = Create(1, 1, 1, 1, 1, 1, 1, 1);

    public bool IsEqualProfile =>
        AlmostEqual(Ne, Equal.Ne) &&
        AlmostEqual(Ni, Equal.Ni) &&
        AlmostEqual(Te, Equal.Te) &&
        AlmostEqual(Ti, Equal.Ti) &&
        AlmostEqual(Se, Equal.Se) &&
        AlmostEqual(Si, Equal.Si) &&
        AlmostEqual(Fe, Equal.Fe) &&
        AlmostEqual(Fi, Equal.Fi);

    public static DimensionWeights Parse(string rawValue)
    {
        if (string.IsNullOrWhiteSpace(rawValue))
        {
            throw new ArgumentException("--weights must not be empty.");
        }

        var values = CognitiveScores.Dimensions.ToDictionary(
            dimension => dimension,
            _ => 1d,
            StringComparer.OrdinalIgnoreCase);

        var tokens = rawValue.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);

        if (tokens.Length == 0)
        {
            throw new ArgumentException("--weights format is invalid.");
        }

        foreach (var token in tokens)
        {
            var split = token.Split('=', StringSplitOptions.TrimEntries);
            if (split.Length != 2)
            {
                throw new ArgumentException($"Invalid weight token '{token}'. Use format like Ne=1.2");
            }

            var dimension = NormalizeDimension(split[0]);

            if (!double.TryParse(split[1], NumberStyles.Float, CultureInfo.InvariantCulture, out var parsedValue))
            {
                throw new ArgumentException($"Weight for '{dimension}' is not numeric: '{split[1]}'.");
            }

            values[dimension] = parsedValue;
        }

        return Create(
            values["Ne"],
            values["Ni"],
            values["Te"],
            values["Ti"],
            values["Se"],
            values["Si"],
            values["Fe"],
            values["Fi"]);
    }

    public static DimensionWeights Create(
        double ne,
        double ni,
        double te,
        double ti,
        double se,
        double si,
        double fe,
        double fi)
    {
        ValidateWeight(ne, "Ne");
        ValidateWeight(ni, "Ni");
        ValidateWeight(te, "Te");
        ValidateWeight(ti, "Ti");
        ValidateWeight(se, "Se");
        ValidateWeight(si, "Si");
        ValidateWeight(fe, "Fe");
        ValidateWeight(fi, "Fi");

        var total = ne + ni + te + ti + se + si + fe + fi;
        if (total <= Epsilon)
        {
            throw new ArgumentException("At least one weight must be greater than 0.");
        }

        return new DimensionWeights(
            Ne: ne / total,
            Ni: ni / total,
            Te: te / total,
            Ti: ti / total,
            Se: se / total,
            Si: si / total,
            Fe: fe / total,
            Fi: fi / total);
    }

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

    public string ToCompactDisplay() =>
        string.Join(", ", ToDictionary().Select(pair => $"{pair.Key}={pair.Value.ToString("F4", CultureInfo.InvariantCulture)}"));

    private static bool AlmostEqual(double left, double right) => Math.Abs(left - right) < Epsilon;

    private static string NormalizeDimension(string rawDimension)
    {
        if (string.IsNullOrWhiteSpace(rawDimension))
        {
            throw new ArgumentException("Weight dimension key is empty.");
        }

        var match = CognitiveScores.Dimensions
            .FirstOrDefault(dimension => dimension.Equals(rawDimension.Trim(), StringComparison.OrdinalIgnoreCase));

        return match ?? throw new ArgumentException(
            $"Unknown dimension '{rawDimension}'. Valid keys: {string.Join(", ", CognitiveScores.Dimensions)}");
    }

    private static void ValidateWeight(double value, string name)
    {
        if (double.IsNaN(value) || double.IsInfinity(value))
        {
            throw new ArgumentException($"Weight for '{name}' must be a finite number.");
        }

        if (value < 0)
        {
            throw new ArgumentException($"Weight for '{name}' must be >= 0.");
        }
    }
}
