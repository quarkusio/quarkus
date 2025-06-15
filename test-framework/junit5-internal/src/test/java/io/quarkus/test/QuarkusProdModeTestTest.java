package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.test.common.RestAssuredURLManager;

@TestMethodOrder(OrderAnnotation.class)
public class QuarkusProdModeTestTest {

    @RegisterExtension
    static final QuarkusProdModeTest simpleApp = new QuarkusProdModeTest().setApplicationName("simple-app")
            .setApplicationVersion("0.1-SNAPSHOT").setRun(true);

    @Test
    @Order(1)
    public void shouldStopAndStartManually() {
        thenAppIsRunning();

        try (var urlMgrMock = Mockito.mockStatic(RestAssuredURLManager.class)) {
            whenStopApp();
            thenAppIsNotRunning();

            whenStartApp();
            thenAppIsRunning();

            whenStopApp(); // stop again to verify in next test method that app was auto-restarted
            thenAppIsNotRunning();

            urlMgrMock.verify(() -> RestAssuredURLManager.setURL(false, 8081));
            urlMgrMock.verify(RestAssuredURLManager::clearURL, times(2));
        }
    }

    @Test
    @Order(2)
    public void shouldBeStartedAfterPreviousTestStopped() {
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
