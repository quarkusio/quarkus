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

import static org.jboss.shamrock.test.PathTestHelper.getTestClassesLocation;
import static org.jboss.shamrock.test.PathTestHelper.getAppClassLocation;

import java.io.IOException;
import java.util.Collections;

import org.jboss.shamrock.runner.RuntimeRunner;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

public class ShamrockTest extends AbstractShamrockTestRunner {

    public ShamrockTest(Class<?> klass) throws InitializationError {
        super(klass, (c, n) -> new ShamrockRunListener(c, n));
    }

    private static class ShamrockRunListener extends AbstractShamrockRunListener {

        private RuntimeRunner runtimeRunner;

        ShamrockRunListener(Class<?> testClass, RunNotifier runNotifier) {
            super(testClass, runNotifier);
        }

        @Override
        protected void startShamrock() {
            if (ShamrockUnitTest.started) {
                getRunNotifier().fireTestFailure(new Failure(Description.createSuiteDescription(ShamrockTest.class),
                        new RuntimeException("Cannot mix ShamrockTest and ShamrockUnitTest in the same test suite")));
                return;
            }

            runtimeRunner = new RuntimeRunner(getClass().getClassLoader(), getAppClassLocation(getTestClass()),
                    getTestClassesLocation(getTestClass()), null, Collections.emptyList());
            runtimeRunner.run();
        }

        @Override
        protected void stopShamrock() throws IOException {
            runtimeRunner.close();
        }
    }
}
