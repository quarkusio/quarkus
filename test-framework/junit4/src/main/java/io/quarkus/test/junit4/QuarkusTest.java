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

import static io.quarkus.test.common.PathTestHelper.getAppClassLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.IOException;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.PropertyTestUtil;

public class QuarkusTest extends AbstractQuarkusTestRunner {

    public QuarkusTest(Class<?> klass) throws InitializationError {
        super(klass, (c, n) -> new QuarkusRunListener(c, n));
    }

    private static class QuarkusRunListener extends AbstractQuarkusRunListener {

        private RuntimeRunner runtimeRunner;

        QuarkusRunListener(Class<?> testClass, RunNotifier runNotifier) {
            super(testClass, runNotifier);
        }

        @Override
        protected void startQuarkus() {
            PropertyTestUtil.setLogFileProperty();
            runtimeRunner = RuntimeRunner.builder()
                    .setLaunchMode(LaunchMode.TEST)
                    .setClassLoader(getClass().getClassLoader())
                    .setTarget(getAppClassLocation(getTestClass()))
                    .setFrameworkClassesPath(getTestClassesLocation(getTestClass()))
                    .build();
            runtimeRunner.run();
        }

        @Override
        protected void stopQuarkus() throws IOException {
            runtimeRunner.close();
        }
    }
}
