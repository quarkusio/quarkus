package io.quarkus.deployment.dev.testing;

import java.util.function.Consumer;

import io.quarkus.dev.testing.ContinuousTestingSharedStateManager;

public class ContinuousTestingSharedStateListener implements TestListener {

    private volatile ContinuousTestingSharedStateManager.State lastState;

    @Override
    public void listenerRegistered(TestController testController) {

    }

    @Override
    public void testsEnabled() {
        if (lastState == null) {
            ContinuousTestingSharedStateManager
                    .setLastState(new ContinuousTestingSharedStateManager.State(
                            ContinuousTestingSharedStateManager.getLastState().lastRun, true, false,
                            0, 0, 0, 0,
                            0, 0, 0, ContinuousTestingSharedStateManager.getLastState().isBrokenOnly,
                            ContinuousTestingSharedStateManager.getLastState().isTestOutput,
                            ContinuousTestingSharedStateManager.getLastState().isInstrumentationBasedReload,
                            ContinuousTestingSharedStateManager.getLastState().isLiveReload));
        } else {
            ContinuousTestingSharedStateManager
                    .setLastState(lastState);
        }
    }

    @Override
    public void testsDisabled() {
        ContinuousTestingSharedStateManager.setRunning(false);
    }

    @Override
    public void testRunStarted(Consumer<TestRunListener> listenerConsumer) {
        ContinuousTestingSharedStateManager.setInProgress(true);
        listenerConsumer.accept(new TestRunListener() {

            @Override
            public void runComplete(TestRunResults testRunResults) {
                lastState = new ContinuousTestingSharedStateManager.State(testRunResults.getId(), true, false,
                        testRunResults.getPassedCount() +
                                testRunResults.getFailedCount() +
                                testRunResults.getSkippedCount(),
                        testRunResults.getPassedCount(),
                        testRunResults.getFailedCount(), testRunResults.getSkippedCount(),
                        testRunResults.getCurrentPassedCount(), testRunResults.getCurrentFailedCount(),
                        testRunResults.getCurrentSkippedCount(),
                        ContinuousTestingSharedStateManager.getLastState().isBrokenOnly,
                        ContinuousTestingSharedStateManager.getLastState().isTestOutput,
                        ContinuousTestingSharedStateManager.getLastState().isInstrumentationBasedReload,
                        ContinuousTestingSharedStateManager.getLastState().isLiveReload);
                ContinuousTestingSharedStateManager.setLastState(lastState);
            }

            @Override
            public void noTests(TestRunResults results) {
                runComplete(results);
            }

            @Override
            public void runAborted() {
                ContinuousTestingSharedStateManager.setInProgress(false);
            }
        });

    }

    @Override
    public void setBrokenOnly(boolean bo) {
        ContinuousTestingSharedStateManager.setBrokenOnly(bo);
    }

    @Override
    public void setTestOutput(boolean to) {
        ContinuousTestingSharedStateManager.setTestOutput(to);
    }

    @Override
    public void setInstrumentationBasedReload(boolean ibr) {
        ContinuousTestingSharedStateManager.setInstrumentationBasedReload(ibr);
    }

    @Override
    public void setLiveReloadEnabled(boolean lre) {
        ContinuousTestingSharedStateManager.setLiveReloadEnabled(lre);
    }

}
