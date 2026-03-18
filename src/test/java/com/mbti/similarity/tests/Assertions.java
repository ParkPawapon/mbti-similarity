package com.mbti.similarity.tests;

import java.util.Objects;

public final class Assertions {
    private Assertions() {
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            fail(message + " Expected: " + expected + " but was: " + actual);
        }
    }

    public static void assertDoubleEquals(double expected, double actual, double tolerance, String message) {
        if (Math.abs(expected - actual) > tolerance) {
            fail(message + " Expected: " + expected + " but was: " + actual);
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            fail(message);
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            fail(message);
        }
    }

    public static void assertContains(String expectedSubstring, String actual, String message) {
        if (actual == null || !actual.contains(expectedSubstring)) {
            fail(message + " Expected substring: " + expectedSubstring + " in: " + actual);
        }
    }

    public static <T extends Throwable> T assertThrows(
        Class<T> expectedType,
        ThrowingRunnable runnable,
        String expectedMessageFragment,
        String message
    ) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (!expectedType.isInstance(throwable)) {
                fail(message + " Expected exception " + expectedType.getName() + " but got " + throwable.getClass().getName());
            }

            if (expectedMessageFragment != null && !expectedMessageFragment.isBlank()) {
                assertContains(expectedMessageFragment, throwable.getMessage(), message);
            }

            return expectedType.cast(throwable);
        }

        fail(message + " Expected exception " + expectedType.getName() + " but nothing was thrown.");
        return null;
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
