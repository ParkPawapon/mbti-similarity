package com.mbti.similarity.tests;

import com.mbti.similarity.domain.LoadResult;
import com.mbti.similarity.service.StudentProfileCsvRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StudentProfileCsvRepositoryTests {
    private StudentProfileCsvRepositoryTests() {
    }

    public static void runAll() throws Exception {
        loadShouldSkipRowWhenScoresMissing();
        loadShouldTreatBlankFirstHeaderAsId();
        loadShouldHandleQuotedCommas();
    }

    private static void loadShouldSkipRowWhenScoresMissing() throws IOException {
        String csv = ""
            + "ID,Name,Sex,Ne,Ni,Te,Ti,Se,Si,Fe,Fi,Type,Enneagram,Nick\n"
            + "1,A,Male,10,11,12,13,14,15,16,17,INTP,5,a\n"
            + "2,B,Female,10,11,12,,14,15,16,17,INFJ,1,b\n";

        Path tempFile = Files.createTempFile("mbti-sim-", ".csv");
        try {
            Files.writeString(tempFile, csv, StandardCharsets.UTF_8);
            StudentProfileCsvRepository repository = new StudentProfileCsvRepository();
            LoadResult result = repository.load(tempFile);

            Assertions.assertEquals(2, result.getTotalDataRows(), "All non-empty rows should be counted.");
            Assertions.assertEquals(1, result.getProfiles().size(), "Valid profile count should match.");
            Assertions.assertEquals(1, result.getSkippedRecords().size(), "Invalid rows should be skipped.");
            Assertions.assertEquals("2", result.getSkippedRecords().get(0).getId(), "Skipped row ID should be captured.");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void loadShouldTreatBlankFirstHeaderAsId() throws IOException {
        String csv = ""
            + ",Name,Sex,Ne,Ni,Te,Ti,Se,Si,Fe,Fi,Type,Enneagram,Nick\n"
            + "68090500418,Pawapon,Male,25.8,33,32,35,33,34,30,22,ISTP,1,Park\n";

        Path tempFile = Files.createTempFile("mbti-sim-", ".csv");
        try {
            Files.writeString(tempFile, csv, StandardCharsets.UTF_8);
            StudentProfileCsvRepository repository = new StudentProfileCsvRepository();
            LoadResult result = repository.load(tempFile);

            Assertions.assertEquals(1, result.getProfiles().size(), "Blank first header should map to ID.");
            Assertions.assertEquals("68090500418", result.getProfiles().get(0).getId(), "ID should come from the first column.");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void loadShouldHandleQuotedCommas() throws IOException {
        String csv = ""
            + "ID,Name,Sex,Ne,Ni,Te,Ti,Se,Si,Fe,Fi,Type,Enneagram,Nick\n"
            + "1,\"Doe, John\",Male,10,11,12,13,14,15,16,17,INTP,5,\"Johnny, Jr.\"\n";

        Path tempFile = Files.createTempFile("mbti-sim-", ".csv");
        try {
            Files.writeString(tempFile, csv, StandardCharsets.UTF_8);
            StudentProfileCsvRepository repository = new StudentProfileCsvRepository();
            LoadResult result = repository.load(tempFile);

            Assertions.assertEquals(1, result.getProfiles().size(), "Quoted commas must be parsed correctly.");
            Assertions.assertEquals("Doe, John", result.getProfiles().get(0).getName(), "Quoted names must be preserved.");
            Assertions.assertEquals("Johnny, Jr.", result.getProfiles().get(0).getNick(), "Quoted nicknames must be preserved.");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
