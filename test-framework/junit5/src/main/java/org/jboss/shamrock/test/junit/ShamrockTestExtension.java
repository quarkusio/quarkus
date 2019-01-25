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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.Path;

import org.jboss.builder.BuildContext;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.jandex.DotName;
import org.jboss.protean.arc.processor.DotNames;
import org.jboss.shamrock.arc.deployment.BeanDefiningAnnotationBuildItem;
import org.jboss.shamrock.runner.RuntimeRunner;
import org.jboss.shamrock.runtime.InjectionFactory;
import org.jboss.shamrock.runtime.InjectionFactoryTemplate;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.test.common.NativeImageLauncher;
import org.jboss.shamrock.test.common.TestResourceManager;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

public class ShamrockTestExtension implements BeforeAllCallback, TestInstanceFactory {


    public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) throws TestInstantiationException {
        try {
            Class testClass = extensionContext.getRequiredTestClass();
            InjectionFactory injectionFactory = InjectionFactoryTemplate.currentFactory();
            if(injectionFactory == null) {
                //integration test, just create the class
                return testClass.newInstance();
            }


            ProxyFactory<?> factory = new ProxyFactory<>(new ProxyConfiguration<>()
                    .setProxyName(testClass.getName() + "$$ShamrockTestProxy")
                    .setClassLoader(testClass.getClassLoader())
                    .setSuperClass(testClass));
            InjectionInstance<?> injectionInstance = injectionFactory.create(Class.forName(testClass.getName(), true, Thread.currentThread().getContextClassLoader()));
            InjectionInstance.ManagedInstance<?> actualTestInstance = injectionInstance.newManagedInstance();

            extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(ShamrockTestExtension.class.getName() + ".instance", new ExtensionContext.Store.CloseableResource() {
                @Override
                public void close() throws Throwable {
                    actualTestInstance.destroy();
                }
            });
            return factory.newInstance(new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    Method realMethod = actualTestInstance.get().getClass().getMethod(method.getName(), method.getParameterTypes());
                    return realMethod.invoke(actualTestInstance.get(), args);
                }
            });
        } catch (Exception e) {
            throw new TestInstantiationException("Unable to create test proxy", e);
        }
    }

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
        Path testClassesLocation = getTestClassesLocation(context.getRequiredTestClass());
        RuntimeRunner runtimeRunner = RuntimeRunner
                .builder()
                .setClassLoader(getClass().getClassLoader())
                .setApplicationRoot(getAppClassLocation(context.getRequiredTestClass()))
                .setFrameworkClassesPath(testClassesLocation)
                .addAdditionalApplicationRoot(testClassesLocation)
                .addChainCustomizer((b) -> {
                    b.addBuildStep()
                            .produces(BeanDefiningAnnotationBuildItem.class)
                            .setBuildStep(new org.jboss.builder.BuildStep() {
                                @Override
                                public void execute(BuildContext context) {
                                    context.produce(new BeanDefiningAnnotationBuildItem(DotName.createSimple(ShamrockTest.class.getName()), DotNames.DEFAULT, false));
                                }
                            }).build();
                }).build();
        runtimeRunner.run();

        return new ExtensionState(testResourceManager, runtimeRunner, false);
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
