package io.quarkus.maven.it;

import java.io.FileNotFoundException;

import org.apache.maven.shared.invoker.MavenInvocationException;

import io.quarkus.maven.it.continuoustesting.ContinuousTestingMavenTestUtils;
import io.quarkus.maven.it.continuoustesting.TestModeContinuousTestingMavenTestUtils;
import io.quarkus.runtime.LaunchMode;

/**
 * Tests the quarkus:test mojo. Most of the behaviour of quarkus:dev is expected to also work with quarkus:test, so tests are
 * in a superclass.
 * <p>
 * NOTE to anyone diagnosing failures in this test, to run a single method use:
 * <p>
 * mvn install -Dit.test=TestMojoIT#methodName
 */
@DisableForNative
public class TestMojoIT extends LaunchMojoTestBase {

    @Override
    protected LaunchMode getDefaultLaunchMode() {
        return LaunchMode.TEST;
    }

    @Override
    protected ContinuousTestingMavenTestUtils getTestingTestUtils() {
        return new TestModeContinuousTestingMavenTestUtils(running);
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

}
