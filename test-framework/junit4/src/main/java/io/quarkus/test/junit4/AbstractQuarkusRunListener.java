package io.quarkus.test.junit4;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;

abstract class AbstractQuarkusRunListener extends RunListener {

    private final Class<?> testClass;

    private final RunNotifier runNotifier;

    private TestResourceManager testResourceManager;

    private boolean started = false;

    private boolean failed = false;

    private final RestAssuredURLManager restAssuredURLManager;

    protected AbstractQuarkusRunListener(Class<?> testClass, RunNotifier runNotifier) {
        this.testClass = testClass;
        this.runNotifier = runNotifier;
        this.testResourceManager = new TestResourceManager(testClass);
        this.restAssuredURLManager = new RestAssuredURLManager(false);
    }

    @Override
    public void testStarted(Description description) throws Exception {
        restAssuredURLManager.setURL();
        if (!started) {
            List<RunListener> stopListeners = new ArrayList<>();

            try {
                try {
                    testResourceManager.start();
                } catch (Exception e) {
                    failed = true;
                    throw e;
                }
                stopListeners.add(0, new RunListener() {
                    @Override
                    public void testRunFinished(Result result) throws Exception {
                        testResourceManager.stop();
                    }
                });

                try {
                    startQuarkus();
                    started = true;
                    stopListeners.add(0, new RunListener() {
                        @Override
                        public void testRunFinished(Result result) throws Exception {
                            try {
                                stopQuarkus();
                            } catch (Exception e) {
                                System.err.println("Unable to stop Quarkus");
                            }
                        }
                    });
                } catch (Exception e) {
                    failed = true;
                    throw new RuntimeException("Unable to boot Quarkus", e);
                }
            } finally {
                for (RunListener stopListener : stopListeners) {
                    runNotifier.addListener(stopListener);
                }
            }
        }
    }

    @Override
    public void testFinished(Description description) throws Exception {
        super.testFinished(description);
        restAssuredURLManager.clearURL();
    }

    public void inject(Object testInstance) {
        testResourceManager.inject(testInstance);
    }

    protected abstract void startQuarkus() throws Exception;

    protected abstract void stopQuarkus() throws Exception;

    protected Class<?> getTestClass() {
        return testClass;
    }

    protected boolean isFailed() {
        return failed;
    }

    protected RunNotifier getRunNotifier() {
        return runNotifier;
    }
}
