package io.quarkus.gradle.devmode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.gradle.continuoustesting.ContinuousTestingClient;

public class TestSelectionTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "test-selection";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev", "--tests", "MyBean*Test.test", "--tests", "com.example.Ano*" };
    }

    @Override
    protected void testDevMode() throws Exception {
        // ignore outcome, just wait for the application to start
        devModeClient.getHttpResponse();

        ContinuousTestingClient.TestStatus tests = new ContinuousTestingClient().waitForNextCompletion();
        assertEquals(5, tests.getTestsPassed());

    }
}
