using MbtiEnterpriseSimilarity.App.Services;

namespace MbtiEnterpriseSimilarity.Tests;

public class CsvStudentProfileRepositoryTests
{
    [Fact]
    public void Load_ShouldSkipRow_WhenScoresMissing()
    {
        var csv =
            "ID,Name,Sex,Ne,Ni,Te,Ti,Se,Si,Fe,Fi,Type,Enneagram,Nick\n" +
            "1,A,Male,10,11,12,13,14,15,16,17,INTP,5,a\n" +
            "2,B,Female,10,11,12,,14,15,16,17,INFJ,1,b\n";

        var tempPath = Path.GetTempFileName();
        File.WriteAllText(tempPath, csv);

        try
        {
            var repository = new CsvStudentProfileRepository();
            var result = repository.Load(tempPath);

            Assert.Equal(2, result.TotalDataRows);
            Assert.Single(result.Profiles);
            Assert.Single(result.SkippedRecords);
            Assert.Equal("2", result.SkippedRecords[0].Id);
        }
        finally
        {
            File.Delete(tempPath);
        }
    }
}
