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

package org.jboss.shamrock.test.junit;

import static org.jboss.shamrock.test.common.PathTestHelper.getAppClassLocation;
import static org.jboss.shamrock.test.common.PathTestHelper.getTestClassesLocation;

import java.io.Closeable;
import java.util.Collections;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shamrock.runner.RuntimeRunner;
import org.jboss.shamrock.runtime.LaunchMode;
import org.jboss.shamrock.test.common.NativeImageLauncher;
import org.jboss.shamrock.test.common.RestAssuredPortManager;
import org.jboss.shamrock.test.common.TestResourceManager;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ShamrockTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {


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
                throw new RuntimeException("Attempted to mix @SubstrateTest and JVM mode tests in the same test run. This is not allowed.");
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
        RestAssuredPortManager.clearPort();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        RestAssuredPortManager.setPort();
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
