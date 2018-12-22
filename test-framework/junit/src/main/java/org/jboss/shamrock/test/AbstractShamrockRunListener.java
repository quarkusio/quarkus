package org.jboss.shamrock.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

abstract class AbstractShamrockRunListener extends RunListener {

    private final Class<?> testClass;

    private final RunNotifier runNotifier;

    private boolean started = false;

    private boolean failed = false;

    protected AbstractShamrockRunListener(Class<?> testClass, RunNotifier runNotifier) {
        this.testClass = testClass;
        this.runNotifier = runNotifier;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        if (!started) {
            List<RunListener> stopListeners = new ArrayList<>();

            try {
                startShamrock();
                started = true;
                stopListeners.add(0, new RunListener() {
                    @Override
                    public void testRunFinished(Result result) throws Exception {
                        try {
                            stopShamrock();
                        } catch (Exception e) {
                            System.err.println("Unable to stop Shamrock");
                        }
                    }

                    @Override
                    public void testFailure(Failure failure) throws Exception {
                        try {
                            stopShamrock();
                        } catch (Exception e) {
                            System.err.println("Unable to stop Shamrock");
                        }
                    }
                });
            } catch (Exception e) {
                failed = true;
                throw new RuntimeException("Unable to boot Shamrock", e);
            } finally {
                for (RunListener stopListener : stopListeners) {
                    runNotifier.addListener(stopListener);
                }
            }
        }
    }

    protected abstract void startShamrock() throws Exception;

    protected abstract void stopShamrock() throws Exception;

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
