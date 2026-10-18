package io.quarkus.gradle.continuoustesting;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for testing continuous testing by parsing log output.
 * <p>
 * This is an alternative to {@link ContinuousTestingClient} that doesn't require
 * an HTTP endpoint (DevUI). It parses the build output log for test results,
 * similar to Maven's TestModeContinuousTestingMavenTestUtils.
 * <p>
 * Use this for quarkusTest mode where there's no HTTP server for the application.
 */
public class ContinuousTestingLogClient {

    // Patterns to parse test results from the console output
    // Example: "All 1 test is passing (0 skipped)"
    // Example: "All 2 tests are passing (0 skipped)"
    // Example: "1 test failed (0 passing, 0 skipped)"
    // Example: "2 tests failed (1 passing, 0 skipped)"
    // Note: The output may contain ANSI escape codes, so we strip them before matching.
    private static final Pattern ALL_PASSING = Pattern.compile(
            "All (\\d+) tests? (?:are|is) passing \\((\\d+) skipped\\)");
    private static final Pattern SOME_FAILING = Pattern.compile(
            "(\\d+) tests? failed \\((\\d+) passing, (\\d+) skipped\\)");
    private static final String TESTS_COMPLETED = "Tests completed at";
    // Pattern to strip ANSI escape codes
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\x1B\\[[;\\d]*m");

    private final File logFile;
    private int logPosition = 0;

    public ContinuousTestingLogClient(File logFile) {
        this.logFile = logFile;
    }

    /**
     * Waits for the next test run to complete and returns the results.
     *
     * @return the test status after completion
     */
    public TestStatus waitForNextCompletion() {
        await()
                .pollDelay(2, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.MINUTES)
                .until(() -> {
                    String log = getLogSinceLastCheck();
                    boolean found = log.contains(TESTS_COMPLETED);
                    if (!found && !log.isEmpty()) {
                        // Debug: print a snippet of the log to help diagnose issues
                        final boolean logFileExists = logFile.exists();
                        System.out.println(
                                "[ContinuousTestingLogClient] Waiting for tests... Log file exists: " + logFileExists
                                        + ", size: " + (logFileExists ? logFile.length() : 0)
                                        + ", checking from position: " + logPosition);
                    }
                    return found;
                });

        String log = getLogSinceLastCheck();
        TestStatus result = parseTestResult(log);

        // Update position for next check
        updateLogPosition();

        return result;
    }

    private String getLogSinceLastCheck() {
        try {
            if (!logFile.exists()) {
                return "";
            }
            String fullLog = Files.readString(logFile.toPath());
            return fullLog.substring(Math.min(logPosition, fullLog.length()));
        } catch (IOException e) {
            return "";
        }
    }

    private void updateLogPosition() {
        try {
            if (logFile.exists()) {
                logPosition = Files.readString(logFile.toPath()).length();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read log file", e);
        }
    }

    private TestStatus parseTestResult(String log) {
        // Strip ANSI escape codes before matching
        String cleanLog = ANSI_ESCAPE.matcher(log).replaceAll("");

        Matcher matcher = ALL_PASSING.matcher(cleanLog);
        if (matcher.find()) {
            int passed = Integer.parseInt(matcher.group(1));
            int skipped = Integer.parseInt(matcher.group(2));
            return new TestStatus(passed, 0, skipped);
        }

        matcher = SOME_FAILING.matcher(cleanLog);
        if (matcher.find()) {
            int failed = Integer.parseInt(matcher.group(1));
            int passed = Integer.parseInt(matcher.group(2));
            int skipped = Integer.parseInt(matcher.group(3));
            return new TestStatus(passed, failed, skipped);
        }

        throw new AssertionError("Could not parse test results from log. " +
                "Expected pattern like 'All N tests are passing' or 'N tests failed'. Log content:\n" + cleanLog);
    }

    /**
     * Test result status.
     */
    public static class TestStatus {
        private final int testsPassed;
        private final int testsFailed;
        private final int testsSkipped;

        public TestStatus(int testsPassed, int testsFailed, int testsSkipped) {
            this.testsPassed = testsPassed;
            this.testsFailed = testsFailed;
            this.testsSkipped = testsSkipped;
        }

        public int getTestsPassed() {
            return testsPassed;
        }

        public int getTestsFailed() {
            return testsFailed;
        }

        public int getTestsSkipped() {
            return testsSkipped;
        }

        @Override
        public String toString() {
            return "TestStatus{" +
                    "testsPassed=" + testsPassed +
                    ", testsFailed=" + testsFailed +
                    ", testsSkipped=" + testsSkipped +
                    '}';
        }
    }
}
