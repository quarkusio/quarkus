package io.quarkus.vertx.http.deployment.devmode.console;

import java.util.function.Consumer;

import org.junit.platform.launcher.TestIdentifier;

import io.quarkus.deployment.dev.testing.TestController;
import io.quarkus.deployment.dev.testing.TestListener;
import io.quarkus.deployment.dev.testing.TestResult;
import io.quarkus.deployment.dev.testing.TestRunListener;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.dev.testing.ContinuousTestingWebsocketListener;

public class ContinuousTestingWebSocketListener implements TestListener {

    @Override
    public void listenerRegistered(TestController testController) {

    }

    @Override
    public void testsEnabled() {
        ContinuousTestingWebsocketListener
                .setLastState(
                        new ContinuousTestingWebsocketListener.State(true, true, 0L, 0L, 0L, 0L, false, false, false));
    }

    @Override
    public void testsDisabled() {
        ContinuousTestingWebsocketListener.setRunning(false);
    }

    @Override
    public void testRunStarted(Consumer<TestRunListener> listenerConsumer) {
        ContinuousTestingWebsocketListener.setInProgress(true);
        listenerConsumer.accept(new TestRunListener() {
            @Override
            public void runStarted(long toRun) {

            }

            @Override
            public void testComplete(TestResult result) {

            }

            @Override
            public void runComplete(TestRunResults testRunResults) {
                ContinuousTestingWebsocketListener.setLastState(
                        new ContinuousTestingWebsocketListener.State(true, false,
                                testRunResults.getPassedCount() +
                                        testRunResults.getFailedCount() +
                                        testRunResults.getSkippedCount(),
                                testRunResults.getPassedCount(),
                                testRunResults.getFailedCount(), testRunResults.getSkippedCount(),
                                ContinuousTestingWebsocketListener.getLastState().isBrokenOnly,
                                ContinuousTestingWebsocketListener.getLastState().isTestOutput,
                                ContinuousTestingWebsocketListener.getLastState().isInstrumentationBasedReload));
            }

            @Override
            public void runAborted() {
                ContinuousTestingWebsocketListener.setInProgress(false);
            }

            @Override
            public void testStarted(TestIdentifier testIdentifier, String className) {

            }
        });

    }

    @Override
    public void setBrokenOnly(boolean bo) {
        ContinuousTestingWebsocketListener.setBrokenOnly(bo);
    }

    @Override
    public void setTestOutput(boolean to) {
        ContinuousTestingWebsocketListener.setTestOutput(to);
    }

    @Override
    public void setInstrumentationBasedReload(boolean ibr) {
        ContinuousTestingWebsocketListener.setInstrumentationBasedReload(ibr);
    }

}
