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

package org.jboss.shamrock.test.junit4;

import java.io.IOException;

import org.jboss.shamrock.test.common.NativeImageLauncher;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

/**
 * A test runner for GraalVM native images.
 */
public class SubstrateTest extends AbstractShamrockTestRunner {

    public SubstrateTest(Class<?> klass) throws InitializationError {
        super(klass, (c, n) -> new ShamrockNativeImageRunListener(c, n));
    }

    private static class ShamrockNativeImageRunListener extends AbstractShamrockRunListener {

        private NativeImageLauncher shamrockProcess;

        ShamrockNativeImageRunListener(Class<?> testClass, RunNotifier runNotifier) {
            super(testClass, runNotifier);
        }

        @Override
        protected void startShamrock() throws IOException {
            shamrockProcess = new NativeImageLauncher(getTestClass());
            try {
                shamrockProcess.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void stopShamrock() {
            shamrockProcess.close();
        }
    }
}
