/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    protected AbstractQuarkusRunListener(Class<?> testClass, RunNotifier runNotifier) {
        this.testClass = testClass;
        this.runNotifier = runNotifier;
        this.testResourceManager = new TestResourceManager(testClass);
    }

    @Override
    public void testStarted(Description description) throws Exception {
        RestAssuredURLManager.setURL();
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
        RestAssuredURLManager.clearURL();
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
