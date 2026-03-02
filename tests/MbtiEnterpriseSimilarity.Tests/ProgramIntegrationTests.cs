using AppProgram = MbtiEnterpriseSimilarity.App.Program;

namespace MbtiEnterpriseSimilarity.Tests;

public class ProgramIntegrationTests
{
    [Fact]
    public void Main_ShouldFail_WhenSkippedRatioExceedsThreshold()
    {
        using var workspace = new TempWorkspace();
        var inputPath = workspace.WriteCsv(BuildCsvWithOneInvalidRow());
        var outputDir = workspace.CreateOutputDir("fail-out");

        var exitCode = AppProgram.Main(
        [
            "--input", inputPath,
            "--target-id", "1",
            "--top", "1",
            "--max-skipped-ratio", "0.2",
            "--output-dir", outputDir
        ]);

        Assert.Equal(1, exitCode);
        Assert.False(Directory.Exists(outputDir) && Directory.EnumerateFiles(outputDir).Any());
    }

    [Fact]
    public void Main_ShouldSucceed_WhenSkippedRatioWithinThreshold()
    {
        using var workspace = new TempWorkspace();
        var inputPath = workspace.WriteCsv(BuildCsvWithOneInvalidRow());
        var outputDir = workspace.CreateOutputDir("ok-out");

        var exitCode = AppProgram.Main(
        [
            "--input", inputPath,
            "--target-id", "1",
            "--top", "1",
            "--max-skipped-ratio", "0.4",
            "--output-dir", outputDir
        ]);

        Assert.Equal(0, exitCode);
        Assert.True(Directory.Exists(outputDir));

        var outputs = Directory.GetFiles(outputDir);
        Assert.Contains(outputs, file => file.EndsWith(".md", StringComparison.OrdinalIgnoreCase));
        Assert.Contains(outputs, file => file.EndsWith(".json", StringComparison.OrdinalIgnoreCase));
    }

    private static string BuildCsvWithOneInvalidRow() =>
        "ID,Name,Sex,Ne,Ni,Te,Ti,Se,Si,Fe,Fi,Type,Enneagram,Nick\n" +
        "1,Target,Male,10,11,12,13,14,15,16,17,INTP,5,T\n" +
        "2,Peer,Female,11,12,13,14,15,16,17,18,INTJ,3,P\n" +
        "3,Bad,Female,,12,13,14,15,16,17,18,INFJ,2,B\n";

    private sealed class TempWorkspace : IDisposable
    {
        private readonly string _root = Path.Combine(Path.GetTempPath(), $"mbti-sim-tests-{Guid.NewGuid():N}");

        public TempWorkspace()
        {
            Directory.CreateDirectory(_root);
        }

        public string WriteCsv(string csvContent)
        {
            var path = Path.Combine(_root, "input.csv");
            File.WriteAllText(path, csvContent);
            return path;
        }

        public string CreateOutputDir(string name) => Path.Combine(_root, name);

        public void Dispose()
        {
            try
            {
                if (Directory.Exists(_root))
                {
                    Directory.Delete(_root, recursive: true);
                }
            }
            catch
            {
                // Ignore temp cleanup errors.
            }
        }
    }
}
