package io.quarkus.gradle.devmode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.gradle.continuoustesting.ContinuousTestingClient;

public class MultiModuleWithNoTestModuleDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "multi-module-with-lib-module";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev" };
    }

    @Override
    protected void testDevMode() throws Exception {
        // ignore outcome, just wait for the application to start
        devModeClient.getHttpResponse();

        ContinuousTestingClient.TestStatus tests = new ContinuousTestingClient().waitForNextCompletion();
        assertEquals(1, tests.getTestsPassed());
        assertEquals(0, tests.getTestsFailed());
    }
}
