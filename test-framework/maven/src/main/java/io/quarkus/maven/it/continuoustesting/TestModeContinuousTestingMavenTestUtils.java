package io.quarkus.maven.it.continuoustesting;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
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
    private static final Pattern ALL_PASSING = Pattern.compile("All (\\d\\d*) tests are passing \\((\\d\\d*) skipped\\)",
            Pattern.MULTILINE);
    private static final Pattern SOME_PASSING = Pattern
            .compile("(\\d\\d*) tests? failed \\((\\d\\d*) passing, (\\d\\d*) skipped\\)", Pattern.MULTILINE);
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
            String log = getLogSinceLastRun();

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
                    fail("Tests were run, but the log is not parseable with the patterns we know. This is the log\n: " + log);
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
