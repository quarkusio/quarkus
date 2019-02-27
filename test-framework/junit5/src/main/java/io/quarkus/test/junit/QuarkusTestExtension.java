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
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

import io.quarkus.bootstrap.BootstrapClassLoaderBuilder;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.runner.RuntimeRunner;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.http.TestHttpResourceManager;

public class QuarkusTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, TestInstanceFactory {

    private URLClassLoader appCl;

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

        //final long start = System.nanoTime();
        final Path appClasses = getAppClassLocation(context.getRequiredTestClass());
        final Path frameworkClasses = getTestClassesLocation(context.getRequiredTestClass());

        try {
            appCl = BootstrapClassLoaderBuilder.newInstance()
                    .setAppClasses(appClasses)
                    .setFrameworkClasses(frameworkClasses)
                    .setLocalProjectsDiscovery(true)
                    .setOffline(true)
                    .setParent(Thread.currentThread().getContextClassLoader())
                    .buildDeploymentCl();
        } catch (BootstrapException e) {
            throw new IllegalStateException("Failed to create the boostrap class loader", e);
        }
        /*
         * try {
         * BootstrapUtils.logUrlDiff(appCl, "Complete CL", BootstrapClassLoaderBuilder.newInstance()
         * .setAppClasses(appClasses)
         * .setFrameworkClasses(frameworkClasses)
         * .setLocalProjectsDiscovery(true)
         * .setOffline(true)
         * .setParent(Thread.currentThread().getContextClassLoader())
         * .buildDeploymentCl(), "Deployment CL");
         * } catch (BootstrapException e1) {
         * // TODO Auto-generated catch block
         * e1.printStackTrace();
         * }
         */
        final ClassLoader originalCl = setCCL(appCl);
        try {

            final RuntimeRunner runtimeRunner = RuntimeRunner.builder()
                    .setLaunchMode(LaunchMode.TEST)
                    .setClassLoader(Thread.currentThread().getContextClassLoader())
                    .setTarget(appClasses)
                    .setFrameworkClassesPath(frameworkClasses)
                    .build();
            runtimeRunner.run();
            return new ExtensionState(testResourceManager, runtimeRunner, false);
            /*
             * final Class<?> rrClass = appCl.loadClass("io.quarkus.runner.RuntimeRunner");
             * final Method m = rrClass.getMethod("runTest", Path.class, Path.class);
             * final Closeable c = (Closeable) m.invoke(null, appClasses, frameworkClasses);
             * return new ExtensionState(testResourceManager, c, false);
             * } catch (ClassNotFoundException e) {
             * throw new IllegalStateException("Failed to load runner class", e);
             * } catch (NoSuchMethodException | SecurityException e) {
             * throw new IllegalStateException("Failed to locate runner method", e);
             * } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
             * throw new IllegalStateException("Failed to invoke runner", e);
             */
        } finally {
            setCCL(originalCl);
        }
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

    private static ClassLoader setCCL(ClassLoader cl) {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(cl);
        return original;
    }

    class ExtensionState implements ExtensionContext.Store.CloseableResource {

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
            final ClassLoader originalCl = appCl == null ? null : setCCL(appCl);
            try {
                resource.close();
            } finally {
                if (originalCl != null) {
                    setCCL(originalCl);
                }
            }
            if (appCl != null) {
                appCl.close();
            }
        }

        public boolean isSubstrate() {
            return substrate;
        }
    }
}
