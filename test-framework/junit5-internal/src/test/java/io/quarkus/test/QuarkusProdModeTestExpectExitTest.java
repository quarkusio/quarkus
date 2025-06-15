package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.common.RestAssuredURLManager;

@TestMethodOrder(OrderAnnotation.class)
public class QuarkusProdModeTestExpectExitTest {

    @RegisterExtension
    static final QuarkusProdModeTest simpleApp = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClass(Main.class)).setApplicationName("simple-app")
            .setApplicationVersion("0.1-SNAPSHOT").setExpectExit(true).setRun(true);

    private static String startupConsoleOutput;

    @BeforeAll
    static void captureStartupConsoleLog() {
        startupConsoleOutput = simpleApp.getStartupConsoleOutput();
        assertNotNull(startupConsoleOutput, "Missing startupConsoleOutput");
    }

    @Test
    @Order(1)
    public void shouldStartManually() {
        thenAppIsNotRunning();
        thenAppWasNotRestarted();

        try (var urlMgrMock = Mockito.mockStatic(RestAssuredURLManager.class)) {
            whenStartApp();
            thenAppIsNotRunning();
            thenAppWasRestarted();

            urlMgrMock.verifyNoInteractions();
        }
    }

    @Test
    @Order(2)
    public void shouldNotBeRestartedInSubsequentTest() {
        thenAppIsNotRunning();
        thenAppWasNotRestarted();
    }

    private void whenStartApp() {
        simpleApp.start();
    }

    private void thenAppIsNotRunning() {
        assertNotNull(simpleApp.getExitCode(), "App is running");
    }

    private void thenAppWasNotRestarted() {
        assertEquals(startupConsoleOutput, simpleApp.getStartupConsoleOutput(), "App was restarted");
    }

    private void thenAppWasRestarted() {
        var newStartupConsoleOutput = simpleApp.getStartupConsoleOutput();
        assertNotEquals(startupConsoleOutput, newStartupConsoleOutput, "App was not restarted");
        startupConsoleOutput = newStartupConsoleOutput;
    }

    @QuarkusMain
    public static class Main {

        public static void main(String[] args) {
            System.out.printf("current nano time: %s%n", System.nanoTime());
        }
    }
}
