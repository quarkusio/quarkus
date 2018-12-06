/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.shamrock.test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.jboss.shamrock.runner.RuntimeRunner;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class ShamrockTest extends BlockJUnit4ClassRunner {

    static boolean first = true;
    static boolean started = false;
    static boolean failed = false;

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public ShamrockTest(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void run(final RunNotifier notifier) {
        runInternal(notifier);
        super.run(notifier);
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        if (!failed) {
            super.runChild(method, notifier);
        } else {
            notifier.fireTestIgnored(describeChild(method));
        }
    }

    private void runInternal(RunNotifier notifier) {
        if (first) {
            first = false;
            //now we need to bootstrap shamrock
            notifier.addListener(new RunListener() {


                @Override
                public void testStarted(Description description) {
                    if (ShamrockUnitTest.started) {
                        notifier.fireTestFailure(new Failure(Description.createSuiteDescription(ShamrockTest.class), new RuntimeException("Cannot mix ShamrockTest and ShamrockUnitTest in the same test suite")));
                        return;
                    }
                    if (!started) {
                        started = true;
                        //TODO: so much hacks...
                        try {
                            Class<?> theClass = description.getTestClass();
                            String classFileName = theClass.getName().replace('.', '/') + ".class";
                            URL resource = theClass.getClassLoader().getResource(classFileName);
                            String testClassLocation = resource.getPath().substring(0, resource.getPath().length() - classFileName.length());
                            String appClassLocation = testClassLocation.replace("test-classes", "classes");
                            Path appRoot = Paths.get(appClassLocation);
                            RuntimeRunner runtimeRunner = new RuntimeRunner(getClass().getClassLoader(), appRoot, Paths.get(testClassLocation), null, new ArrayList<>());
                            runtimeRunner.run();
                        } catch (RuntimeException e) {
                            failed = true;
                            throw new RuntimeException("Failed to boot Shamrock during @ShamrockTest runner!", e);
                        }
                    }
                }
            });
        }
    }
}
