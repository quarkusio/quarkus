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
            ContinuousTestingSharedStateManager.setRunning(true);
        } else {
            ContinuousTestingSharedStateManager
                    .setLastState((s) -> {
                        if (s.getLastRun() > lastState.lastRun) {
                            //small chance of race, next run already compled
                            return s.setRunning(true).build();
                        } else {
                            return s.setRunning(true)
                                    .setCurrentFailed(lastState.currentFailed)
                                    .setCurrentSkipped(lastState.currentSkipped)
                                    .setCurrentFailed(lastState.currentFailed)
                                    .setFailed(lastState.failed)
                                    .setSkipped(lastState.skipped)
                                    .setPassed(lastState.passed)
                                    .setRun(lastState.run)
                                    .build();
                        }
                    });
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
                ContinuousTestingSharedStateManager.setLastState(s -> {
                    return lastState = s.setLastRun(testRunResults.getId())
                            .setInProgress(false)
                            .setRun(testRunResults.getPassedCount() +
                                    testRunResults.getFailedCount() +
                                    testRunResults.getSkippedCount())
                            .setPassed(testRunResults.getPassedCount())
                            .setFailed(testRunResults.getFailedCount())
                            .setSkipped(testRunResults.getSkippedCount())
                            .setCurrentPassed(testRunResults.getCurrentPassedCount())
                            .setCurrentFailed(testRunResults.getCurrentFailedCount())
                            .setCurrentSkipped(testRunResults.getCurrentSkippedCount())
                            .build();

                });
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
