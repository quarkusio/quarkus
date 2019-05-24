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

import java.io.IOException;
import java.util.Map;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;

import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.configuration.ConfigurationPropertiesExtractor;

/**
 * A test runner for GraalVM native images.
 */
public class SubstrateTest extends AbstractQuarkusTestRunner {

    public SubstrateTest(Class<?> klass) throws InitializationError {
        super(klass, (c, n) -> new QuarkusNativeImageRunListener(c, n));
    }

    private static class QuarkusNativeImageRunListener extends AbstractQuarkusRunListener {

        private NativeImageLauncher quarkusProcess;

        QuarkusNativeImageRunListener(Class<?> testClass, RunNotifier runNotifier) {
            super(testClass, runNotifier);
        }

        @Override
        protected void startQuarkus() throws IOException {
            quarkusProcess = new NativeImageLauncher(getTestClass());
            final Map<String, String> configurationPropertiesForSubstrateTests = extractTestConfigurationPropertiesForSubstrateTests();
            quarkusProcess.addSystemProperties(configurationPropertiesForSubstrateTests);

            try {
                quarkusProcess.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private Map<String, String> extractTestConfigurationPropertiesForSubstrateTests() {
            final ConfigurationPropertiesExtractor configurationPropertiesExtractor = new ConfigurationPropertiesExtractor();
            return configurationPropertiesExtractor.extract(getTestClass());
        }

        @Override
        protected void stopQuarkus() {
            quarkusProcess.close();
        }
    }
}
