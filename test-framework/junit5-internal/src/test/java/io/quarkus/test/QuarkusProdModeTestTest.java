package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class QuarkusProdModeTestTest {
    @RegisterExtension
    static final QuarkusProdModeTest simpleApp = new QuarkusProdModeTest()
            .setApplicationName("simple-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setRun(true);

    @Test
    public void shouldStartAndStopInnerProcess() {
        thenAppIsRunning();

        whenStopApp();
        thenAppIsNotRunning();

        whenStartApp();
        thenAppIsRunning();
    }

    private void whenStopApp() {
        simpleApp.stop();
    }

    private void whenStartApp() {
        simpleApp.start();
    }

    private void thenAppIsNotRunning() {
        assertNotNull(simpleApp.getExitCode(), "App is running");
    }

    private void thenAppIsRunning() {
        assertNull(simpleApp.getExitCode(), "App is not running");
    }
}
