package io.quarkus.it.extension.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import io.quarkus.maven.it.RunAndCheckMojoTestBase;
import io.quarkus.maven.it.continuoustesting.ContinuousTestingMavenTestUtils;

@DisabledIfSystemProperty(named = "quarkus.test.native", matches = "true")
@Disabled // Tracked by #22611
public class CallbackDevModeIT extends RunAndCheckMojoTestBase {

    /*
     * We have a few tests that will run in parallel, so set a unique port
     */
    protected int getPort() {
        return 8094;
    }

    protected void runAndCheck(boolean performCompile, String... options)
            throws MavenInvocationException, FileNotFoundException {
        run(performCompile, options);

        try {
            String resp = devModeClient.getHttpResponse();
            assertThat(resp).containsIgnoringCase("ready").containsIgnoringCase("application")
                    .containsIgnoringCase("SNAPSHOT");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // There's no json endpoints, so nothing else to check
    }

    @Test
    public void testThatTheTestsPassed() throws MavenInvocationException, IOException {
        //we also check continuous testing
        String executionDir = "projects/project-using-test-callback-from-extension-processed";
        testDir = initProject("projects/project-using-test-callback-from-extension", executionDir);
        runAndCheck();

        ContinuousTestingMavenTestUtils testingTestUtils = new ContinuousTestingMavenTestUtils(getPort());
        ContinuousTestingMavenTestUtils.TestStatus results = testingTestUtils.waitForNextCompletion();
        // This is a bit brittle when we add tests, but failures are often so catastrophic they're not even reported as failures,
        // so we need to check the pass count explicitly
        Assertions.assertEquals(0, results.getTestsFailed());
        Assertions.assertEquals(2, results.getTestsPassed());
    }

}
