package io.quarkus.it.extension.it;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import io.quarkus.maven.it.RunAndCheckMojoTestBase;
import io.quarkus.maven.it.continuoustesting.ContinuousTestingMavenTestUtils;
import io.quarkus.runtime.LaunchMode;

/**
 * Be aware! This test will not run if the name does not start with 'Test'.
 * <p>
 * NOTE to anyone diagnosing failures in this test, to run a single method use:
 * <p>
 * mvn install -Dit.test=DevMojoIT#methodName
 */
@Disabled // because of https://github.com/quarkiverse/quarkus-pact/issues/73
@DisabledIfSystemProperty(named = "quarkus.test.native", matches = "true")
public class TestParameterTestModeIT extends RunAndCheckMojoTestBase {

    @Override
    protected LaunchMode getDefaultLaunchMode() {
        return LaunchMode.TEST;
    }

    @Override
    public void shutdownTheApp() {
        if (running != null) {
            running.stop();
        }

        // There's no http server, so there's nothing to check to make sure we're stopped, except by the maven invoker itself, or the logs
    }

    /**
     * This is actually more like runAndDoNotCheck, because
     * we can't really check anything via a HTTP get, because this is a test mode application
     */
    @Override
    protected void runAndCheck(boolean performCompile, LaunchMode mode, String... options)
            throws FileNotFoundException, MavenInvocationException {
        run(performCompile, mode, options);

        // We don't need to try and pause, because the continuous testing utils will wait for tests to finish

    }

    @Test
    public void testThatTheTestsPassed() throws MavenInvocationException, IOException {
        //we also check continuous testing
        String executionDir = "projects/project-using-test-parameter-injection-processed";
        testDir = initProject("projects/project-using-test-parameter-injection", executionDir);
        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils(getPort());
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();
        // This is a bit brittle when we add tests, but failures are often so catastrophic they're not even reported as failures,
        // so we need to check the pass count explicitly
        Assertions.assertEquals(0, results.getTestsFailed());
        Assertions.assertEquals(1, results.getTestsPassed());
    }

}
