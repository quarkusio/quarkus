package io.quarkus.maven.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.it.verifier.MavenProcessInvocationResult;
import io.quarkus.maven.it.verifier.RunningInvoker;

/**
 * Test that `mvn -Dsurefire.rerunFailingTestsCount=xx test` (where xx is more than one) works with QuarkusTests,
 * win the case when a test fails.
 */
@DisableForNative
public class SurefireRerunFailedTestsIT extends RunAndCheckMojoTestBase {

    @Test
    public void testFlakyTestsInSingleProfileCase() throws MavenInvocationException, IOException {
        testDir = initProject("projects/test-flaky-test-single-profile", "projects/test-flaky-test-single-profile-processed");
        final RunningInvoker running = new RunningInvoker(testDir, false);

        // trigger mvn verify -Dsurefire.rerunFailingTestsCount=2
        final String[] mvnArgs = new String[] { "verify", "-Dsurefire.rerunFailingTestsCount=3" };
        final MavenProcessInvocationResult result = running.execute(Arrays.asList(mvnArgs), Collections.emptyMap());
        await().atMost(5, TimeUnit.MINUTES)
                .until(() -> result.getProcess() != null && !result.getProcess()
                        .isAlive());
        final String processLog = running.log();
        assertEquals(running.getResult()
                .getExitCode(), 0);
        assertThat(processLog).containsIgnoringCase("BUILD SUCCESS");
        // Also do a sense check that the test is functioning correctly and did have a failed execution first
        assertThat(processLog).containsIgnoringCase("deliberate failure run 1");
        assertThat(processLog).containsIgnoringCase("Run 2: PASS");
    }

    @Test
    public void testFlakyTestsInMultipleProfileCase() throws MavenInvocationException, IOException {
        testDir = initProject("projects/test-flaky-test-multiple-profiles",
                "projects/test-flaky-test-multiple-profiles-processed");
        final RunningInvoker running = new RunningInvoker(testDir, false);

        // trigger mvn verify -Dsurefire.rerunFailingTestsCount=2
        final String[] mvnArgs = new String[] { "verify", "-Dsurefire.rerunFailingTestsCount=3" };
        final MavenProcessInvocationResult result = running.execute(Arrays.asList(mvnArgs), Collections.emptyMap());
        await().atMost(5, TimeUnit.MINUTES)
                .until(() -> result.getProcess() != null && !result.getProcess()
                        .isAlive());
        final String processLog = running.log();
        assertEquals(running.getResult()
                .getExitCode(), 0);
        assertThat(processLog).containsIgnoringCase("BUILD SUCCESS");
        // Also do a sense check that the test is functioning correctly and did have a failed execution first
        assertThat(processLog).containsIgnoringCase("deliberate failure run 1");
        assertThat(processLog).containsIgnoringCase("Run 2: PASS");
    }
}
