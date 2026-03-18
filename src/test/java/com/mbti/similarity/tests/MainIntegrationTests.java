package com.mbti.similarity.tests;

import com.mbti.similarity.Main;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MainIntegrationTests {
    private MainIntegrationTests() {
    }

    public static void runAll() throws Exception {
        mainShouldFailWhenSkippedRatioExceedsThreshold();
        mainShouldSucceedWhenSkippedRatioWithinThreshold();
        mainShouldSucceedOnReferenceDataset();
    }

    private static void mainShouldFailWhenSkippedRatioExceedsThreshold() throws IOException {
        try (TempWorkspace workspace = new TempWorkspace()) {
            Path inputPath = workspace.writeCsv(buildCsvWithOneInvalidRow());
            Path outputDir = workspace.resolve("fail-out");

            int exitCode = Main.run(new String[] {
                "--input", inputPath.toString(),
                "--target-id", "1",
                "--top", "1",
                "--max-skipped-ratio", "0.2",
                "--output-dir", outputDir.toString()
            });

            Assertions.assertEquals(1, exitCode, "Main should fail when skipped ratio exceeds threshold.");
            Assertions.assertFalse(Files.exists(outputDir) && hasFiles(outputDir), "No reports should be created on failure.");
        }
    }

    private static void mainShouldSucceedWhenSkippedRatioWithinThreshold() throws IOException {
        try (TempWorkspace workspace = new TempWorkspace()) {
            Path inputPath = workspace.writeCsv(buildCsvWithOneInvalidRow());
            Path outputDir = workspace.resolve("ok-out");

            int exitCode = Main.run(new String[] {
                "--input", inputPath.toString(),
                "--target-id", "1",
                "--top", "1",
                "--max-skipped-ratio", "0.4",
                "--output-dir", outputDir.toString()
            });

            Assertions.assertEquals(0, exitCode, "Main should succeed when skipped ratio is within threshold.");
            Assertions.assertTrue(Files.isDirectory(outputDir), "Output directory should be created.");
            Assertions.assertTrue(hasExtension(outputDir, ".md"), "Markdown report should be created.");
            Assertions.assertTrue(hasExtension(outputDir, ".json"), "JSON report should be created.");
        }
    }

    private static void mainShouldSucceedOnReferenceDataset() throws IOException {
        try (TempWorkspace workspace = new TempWorkspace()) {
            Path outputDir = workspace.resolve("reference-out");
            int exitCode = Main.run(new String[] {
                "--input", Path.of("data", "CSS121_MBTI_2026_68_2.csv").toAbsolutePath().toString(),
                "--target-id", "68090500418",
                "--top", "5",
                "--max-skipped-ratio", "0.2",
                "--exclude-id", "99999999999",
                "--mode", "zscore",
                "--output-dir", outputDir.toString()
            });

            Assertions.assertEquals(0, exitCode, "Reference dataset run should succeed.");
            Assertions.assertTrue(hasExtension(outputDir, ".md"), "Reference run should create Markdown output.");
            Assertions.assertTrue(hasExtension(outputDir, ".json"), "Reference run should create JSON output.");
        }
    }

    private static String buildCsvWithOneInvalidRow() {
        return ""
            + "ID,Name,Sex,Ne,Ni,Te,Ti,Se,Si,Fe,Fi,Type,Enneagram,Nick\n"
            + "1,Target,Male,10,11,12,13,14,15,16,17,INTP,5,T\n"
            + "2,Peer,Female,11,12,13,14,15,16,17,18,INTJ,3,P\n"
            + "3,Bad,Female,,12,13,14,15,16,17,18,INFJ,2,B\n";
    }

    private static boolean hasFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            return stream.iterator().hasNext();
        }
    }

    private static boolean hasExtension(Path directory, String extension) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + extension)) {
            return stream.iterator().hasNext();
        }
    }

    private static final class TempWorkspace implements AutoCloseable {
        private final Path root;

        private TempWorkspace() throws IOException {
            this.root = Files.createTempDirectory("mbti-sim-tests-");
        }

        private Path writeCsv(String csvContent) throws IOException {
            Path path = root.resolve("input.csv");
            Files.writeString(path, csvContent, StandardCharsets.UTF_8);
            return path;
        }

        private Path resolve(String name) {
            return root.resolve(name);
        }

        @Override
        public void close() throws IOException {
            deleteRecursively(root);
        }

        private void deleteRecursively(Path path) throws IOException {
            if (!Files.exists(path)) {
                return;
            }

            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path child : stream) {
                        deleteRecursively(child);
                    }
                }
            }

            Files.deleteIfExists(path);
        }
    }
}
