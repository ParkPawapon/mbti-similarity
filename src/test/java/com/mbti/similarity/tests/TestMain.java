package com.mbti.similarity.tests;

public final class TestMain {
    private TestMain() {
    }

    public static void main(String[] args) throws Exception {
        runSuite("DimensionWeightsTests", DimensionWeightsTests::runAll);
        runSuite("AppOptionsTests", AppOptionsTests::runAll);
        runSuite("StudentProfileCsvRepositoryTests", StudentProfileCsvRepositoryTests::runAll);
        runSuite("SimilarityAnalyzerTests", SimilarityAnalyzerTests::runAll);
        runSuite("MainIntegrationTests", MainIntegrationTests::runAll);
        System.out.println("All tests passed.");
    }

    private static void runSuite(String name, ThrowingRunnable suite) throws Exception {
        try {
            suite.run();
            System.out.println("[PASS] " + name);
        } catch (Throwable throwable) {
            System.err.println("[FAIL] " + name + ": " + throwable.getMessage());
            throw throwable;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
