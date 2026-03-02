using System.Globalization;
using Microsoft.VisualBasic.FileIO;
using MbtiEnterpriseSimilarity.App.Domain;

namespace MbtiEnterpriseSimilarity.App.Services;

public sealed class CsvStudentProfileRepository
{
    private static readonly string[] RequiredColumns =
    [
        "ID", "Name", "Sex", "Ne", "Ni", "Te", "Ti", "Se", "Si", "Fe", "Fi", "Type", "Enneagram", "Nick"
    ];

    public LoadResult Load(string csvPath)
    {
        if (string.IsNullOrWhiteSpace(csvPath))
        {
            throw new ArgumentException("CSV path is required.", nameof(csvPath));
        }

        if (!File.Exists(csvPath))
        {
            throw new FileNotFoundException("CSV file not found.", csvPath);
        }

        using var parser = new TextFieldParser(csvPath)
        {
            TextFieldType = FieldType.Delimited,
            HasFieldsEnclosedInQuotes = true,
            TrimWhiteSpace = true
        };

        parser.SetDelimiters(",");

        if (parser.EndOfData)
        {
            throw new InvalidOperationException("CSV file is empty.");
        }

        var headers = parser.ReadFields() ?? throw new InvalidOperationException("CSV header is invalid.");
        var headerIndex = BuildHeaderIndex(headers);
        ValidateRequiredColumns(headerIndex);

        var profiles = new List<StudentProfile>();
        var skippedRecords = new List<SkippedRecord>();
        var totalDataRows = 0;
        var lineNumber = 1; // Header line.

        while (!parser.EndOfData)
        {
            lineNumber++;
            var fields = parser.ReadFields();

            if (fields is null || fields.Length == 0 || fields.All(string.IsNullOrWhiteSpace))
            {
                continue;
            }

            totalDataRows++;

            try
            {
                var id = GetValue(fields, headerIndex, "ID");

                if (string.IsNullOrWhiteSpace(id))
                {
                    throw new FormatException("ID is empty.");
                }

                var scores = new CognitiveScores(
                    ParseScore(GetValue(fields, headerIndex, "Ne"), "Ne"),
                    ParseScore(GetValue(fields, headerIndex, "Ni"), "Ni"),
                    ParseScore(GetValue(fields, headerIndex, "Te"), "Te"),
                    ParseScore(GetValue(fields, headerIndex, "Ti"), "Ti"),
                    ParseScore(GetValue(fields, headerIndex, "Se"), "Se"),
                    ParseScore(GetValue(fields, headerIndex, "Si"), "Si"),
                    ParseScore(GetValue(fields, headerIndex, "Fe"), "Fe"),
                    ParseScore(GetValue(fields, headerIndex, "Fi"), "Fi"));

                profiles.Add(
                    new StudentProfile(
                        Id: id,
                        Name: GetValue(fields, headerIndex, "Name"),
                        Sex: GetValue(fields, headerIndex, "Sex"),
                        Scores: scores,
                        Type: GetValue(fields, headerIndex, "Type"),
                        Enneagram: GetValue(fields, headerIndex, "Enneagram"),
                        Nick: GetValue(fields, headerIndex, "Nick")));
            }
            catch (Exception ex)
            {
                var id = SafeGetValue(fields, headerIndex, "ID");
                skippedRecords.Add(new SkippedRecord(lineNumber, id, ex.Message));
            }
        }

        return new LoadResult(profiles, skippedRecords, totalDataRows);
    }

    private static Dictionary<string, int> BuildHeaderIndex(string[] headers)
    {
        var index = new Dictionary<string, int>(StringComparer.OrdinalIgnoreCase);

        for (var i = 0; i < headers.Length; i++)
        {
            var header = headers[i].Trim();
            if (!string.IsNullOrWhiteSpace(header))
            {
                index[header] = i;
            }
        }

        return index;
    }

    private static void ValidateRequiredColumns(IReadOnlyDictionary<string, int> headerIndex)
    {
        var missing = RequiredColumns
            .Where(column => !headerIndex.ContainsKey(column))
            .ToList();

        if (missing.Count > 0)
        {
            throw new InvalidOperationException($"CSV missing required columns: {string.Join(", ", missing)}");
        }
    }

    private static string SafeGetValue(IReadOnlyList<string> fields, IReadOnlyDictionary<string, int> headerIndex, string columnName)
    {
        try
        {
            return GetValue(fields, headerIndex, columnName);
        }
        catch
        {
            return string.Empty;
        }
    }

    private static string GetValue(IReadOnlyList<string> fields, IReadOnlyDictionary<string, int> headerIndex, string columnName)
    {
        var index = headerIndex[columnName];
        if (index >= fields.Count)
        {
            return string.Empty;
        }

        return fields[index].Trim();
    }

    private static double ParseScore(string rawValue, string columnName)
    {
        if (string.IsNullOrWhiteSpace(rawValue))
        {
            throw new FormatException($"{columnName} is empty.");
        }

        if (double.TryParse(rawValue, NumberStyles.Float, CultureInfo.InvariantCulture, out var value))
        {
            return value;
        }

        if (double.TryParse(rawValue, NumberStyles.Float, CultureInfo.CurrentCulture, out value))
        {
            return value;
        }

        throw new FormatException($"{columnName} is not numeric: '{rawValue}'.");
    }
}
