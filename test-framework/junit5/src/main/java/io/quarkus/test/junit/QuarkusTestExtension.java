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

package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.Closeable;

import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.http.TestHttpResourceManager;

public class QuarkusTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, TestInstanceFactory {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ExtensionContext root = context.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        ExtensionState state = (ExtensionState) store.get(ExtensionState.class.getName());
        boolean substrateTest = context.getRequiredTestClass().isAnnotationPresent(SubstrateTest.class);
        if (state == null) {
            TestResourceManager testResourceManager = new TestResourceManager(context.getRequiredTestClass());
            testResourceManager.start();

            if (substrateTest) {
                NativeImageLauncher launcher = new NativeImageLauncher(context.getRequiredTestClass());
                launcher.start();
                state = new ExtensionState(testResourceManager, launcher, true);
            } else {
                state = doJavaStart(context, testResourceManager);
            }
            store.put(ExtensionState.class.getName(), state);
        } else {
            if (substrateTest != state.isSubstrate()) {
                throw new RuntimeException(
                        "Attempted to mix @SubstrateTest and JVM mode tests in the same test run. This is not allowed.");
            }
        }
    }

    private ExtensionState doJavaStart(ExtensionContext context, TestResourceManager testResourceManager) {
        RuntimeRunner runtimeRunner = RuntimeRunner.builder()
                .setLaunchMode(LaunchMode.TEST)
                .setClassLoader(getClass().getClassLoader())
                .setTarget(getAppClassLocation(context.getRequiredTestClass()))
                .setFrameworkClassesPath(getTestClassesLocation(context.getRequiredTestClass()))
                .build();
        runtimeRunner.run();
        return new ExtensionState(testResourceManager, runtimeRunner, false);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        RestAssuredURLManager.clearURL();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        RestAssuredURLManager.setURL();
    }

    @Override
    public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
            throws TestInstantiationException {
        try {
            Object instance = factoryContext.getTestClass().newInstance();
            TestHttpResourceManager.inject(instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TestInstantiationException("Failed to create test instance", e);
        }
    }

    static class ExtensionState implements ExtensionContext.Store.CloseableResource {

        private final TestResourceManager testResourceManager;
        private final Closeable resource;
        private final boolean substrate;

        ExtensionState(TestResourceManager testResourceManager, Closeable resource, boolean substrate) {
            this.testResourceManager = testResourceManager;
            this.resource = resource;
            this.substrate = substrate;
        }

        @Override
        public void close() throws Throwable {
            testResourceManager.stop();
            resource.close();
        }

        public boolean isSubstrate() {
            return substrate;
        }
    }
}
