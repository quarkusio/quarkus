package io.quarkus.maven.it;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.continuoustesting.ContinuousTestingMavenTestUtils;
import io.quarkus.runtime.LaunchMode;

/**
 * Contains tests that we expect to pass with both quarkus:dev and quarkus:test
 */
@DisableForNative
public abstract class LaunchMojoTestBase extends RunAndCheckMojoTestBase {

    protected abstract ContinuousTestingMavenTestUtils getTestingTestUtils();

    @Test
    public void testThatTheTestsAreReRunMultiModule()
            throws MavenInvocationException, IOException {
        //we also check continuous testing
        testDir = initProject("projects/multimodule", "projects/multimodule-with-deps");
        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = getTestingTestUtils();
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();

        //check that the tests in both modules run
        assertEquals(2, results.getTestsPassed(),
                "Did not meet expectation for number of tests passed, actual results " + results);

        // Edit the "Hello" message.
        File source = new File(testDir, "rest/src/main/java/org/acme/HelloResource.java");
        final String uuid = UUID.randomUUID().toString();
        filter(source, Collections.singletonMap("return \"hello\";", "return \"" + uuid + "\";"));

        // Wait until we get "uuid"
        // We can't poll, so just pause
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            fail(e);
        }
        await()
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(source::isFile);

        results = testingTestUtils.waitForNextCompletion();

        //make sure the test is failing now
        assertEquals(1, results.getTestsFailed());
        //now modify the passing test
        var testSource = new File(testDir, "rest/src/test/java/org/acme/test/SimpleTest.java");
        filter(testSource, Collections.singletonMap("Assertions.assertTrue(true);", "Assertions.assertTrue(false);"));
        results = testingTestUtils.waitForNextCompletion();
        assertEquals(2, results.getTotalTestsFailed(),
                "Did not meet expectation for number of tests failed, actual results " + results);
        //fix it again
        filter(testSource, Collections.singletonMap("Assertions.assertTrue(false);", "Assertions.assertTrue(true);"));
        results = testingTestUtils.waitForNextCompletion();
        assertEquals(1, results.getTotalTestsFailed(), "Failed, actual results " + results);
        assertEquals(1, results.getTotalTestsPassed(), "Failed, actual results " + results);

    }

    @Test
    public void testSelection() throws MavenInvocationException, IOException {
        testDir = initProject("projects/test-selection");
        run(true, "-Dtest=Ba*ic,Enabled?Test,NotEnabled*#executeAnyway*,!NotEnabledHardDisabled,#alwaysExecute,!#neverExecute");

        if (getDefaultLaunchMode() == LaunchMode.DEVELOPMENT) {
            // ignore outcome, just wait for the application to start
            devModeClient.getHttpResponse();
        }

        ContinuousTestingMavenTestUtils.TestStatus tests = getTestingTestUtils().waitForNextCompletion();
        assertEquals(7, tests.getTestsPassed());
    }
}
