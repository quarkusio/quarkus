package io.quarkus.maven.it.continuoustesting;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * Utilities for testing behaviour with `mvn quarkus:test`. This is harder than dev mode, since
 * we don't have an http endpoint to query for test status, but we can do our best effort.
 */
public class TestModeContinuousTestingMavenTestUtils extends ContinuousTestingMavenTestUtils {
    // Example output we look for
    // 1 test failed (1 passing, 0 skipped), 1 test was run in 217ms. Tests completed at 21:22:34 due to changes to HelloResource$Blah.class and 1 other files.
    // All 2 tests are passing (0 skipped), 2 tests were run in 1413ms. Tests completed at 21:22:33.
    // All 1 test is passing (0 skipped), ...
    // Windows log, despite `quarkus.console.basic=true', might contain terminal control symbols, colour decorations.
    // e.g. the matcher is then fighting: [39m[91m1 test failed[39m ([32m1 passing[39m, [94m0 skipped[39m)
    private static final Pattern ALL_PASSING = Pattern.compile(
            "(?:\\e\\[[\\d;]+m)*All (\\d+) tests? (?:are|is) passing \\((\\d+) skipped\\)",
            Pattern.MULTILINE);
    private static final Pattern SOME_PASSING = Pattern
            .compile(
                    "(?:\\e\\[[\\d;]+m)*(\\d+) tests? failed(?:\\e\\[[\\d;]+m)* \\((?:\\e\\[[\\d;]+m)*(\\d+) " +
                            "passing(?:\\e\\[[\\d;]+m)*, (?:\\e\\[[\\d;]+m)*(\\d+) skipped(?:\\e\\[[\\d;]+m)*\\)",
                    Pattern.MULTILINE);
    private static final String TESTS_COMPLETED = "Tests completed at";
    private final RunningInvoker running;
    private int startPosition = 0;

    public TestModeContinuousTestingMavenTestUtils(RunningInvoker running) {
        this.running = running;
    }

    @Override
    public TestStatus waitForNextCompletion() {

        // We have to scrape test status, because in test mode we do not have an API
        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(3, TimeUnit.MINUTES).until(() -> getLogSinceLastRun().contains(TESTS_COMPLETED));
        TestStatus testStatus = new TestStatus();
        try {
            final String log = getLogSinceLastRun();

            Matcher matcher = ALL_PASSING.matcher(log);
            int failCount;
            int passCount;
            int skipCount;
            if (matcher.find()) {
                passCount = Integer.parseInt(matcher.group(1));
                skipCount = Integer.parseInt(matcher.group(2));
                failCount = 0;
            } else {
                matcher = SOME_PASSING.matcher(log);
                if (!matcher.find()) {
                    final Path f = File.createTempFile("quarkus-maven-test-debug-log", ".txt").toPath();
                    Files.writeString(f, log, StandardCharsets.UTF_8);
                    fail("Tests were run, but the log is not parseable with the patterns we know, " + System.lineSeparator()
                            + "i.e. neither \"" + ALL_PASSING.pattern() + "\" nor \"" + SOME_PASSING.pattern() + "\"."
                            + System.lineSeparator() +
                            " Note that possible terminal control characters might not be seen here. " + System.lineSeparator()
                            +
                            "Check the text file dump too: " + f.toAbsolutePath() + ". This is the log:"
                            + System.lineSeparator() + log);
                }
                failCount = Integer.parseInt(matcher.group(1));
                passCount = Integer.parseInt(matcher.group(2));
                skipCount = Integer.parseInt(matcher.group(3));
            }
            testStatus.setTestsFailed(failCount);
            testStatus.setTestsPassed(passCount);
            testStatus.setTestsSkipped(skipCount);

            // Note: slight fudging of total counts!
            // io.quarkus.test.ContinuousTestingTestUtils treats the total counts the same as the current counts,
            // so we will do the same.
            // it's not ideal, so if it causes problems we may want to invest in more elaborate parsing
            testStatus.setTotalTestsFailed(failCount);
            testStatus.setTotalTestsPassed(passCount);
            testStatus.setTotalTestsSkipped(skipCount);

            setHighWaterMark();
        } catch (IOException e) {
            fail(e);
        }
        return testStatus;

    }

    private void setHighWaterMark() throws IOException {
        // We only want to check the logs in the section which was updated after the last completion,
        // so make a note of what the position was
        startPosition = startPosition + getLogSinceLastRun().indexOf(TESTS_COMPLETED) + TESTS_COMPLETED.length();
    }

    private String getLogSinceLastRun() throws IOException {
        String log = running.log();
        return log.substring(startPosition);

    }
}
