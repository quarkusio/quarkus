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

package org.jboss.shamrock.test;

import static org.jboss.shamrock.test.PathTestHelper.getTestClassesLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.builder.BuildChainBuilder;
import org.jboss.builder.BuildContext;
import org.jboss.builder.BuildException;
import org.jboss.builder.BuildStep;
import org.jboss.builder.item.BuildItem;
import org.jboss.invocation.proxy.ProxyConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.shamrock.runner.RuntimeRunner;
import org.jboss.shamrock.runtime.InjectionFactoryTemplate;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shamrock.test.common.TestResourceManager;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;

/**
 * A test extension for testing Shamrock internals, not intended for end user consumption
 */
public class ShamrockUnitTest implements BeforeAllCallback, AfterAllCallback, TestInstanceFactory {

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    boolean started = false;

    private RuntimeRunner runtimeRunner;
    private Path deploymentDir;
    private Class<? extends Throwable> expectedException;
    private Supplier<JavaArchive> archiveProducer;

    public Class<? extends Throwable> getExpectedException() {
        return expectedException;
    }

    public ShamrockUnitTest setExpectedException(Class<? extends Throwable> expectedException) {
        this.expectedException = expectedException;
        return this;
    }

    public Supplier<JavaArchive> getArchiveProducer() {
        return archiveProducer;
    }

    public ShamrockUnitTest setArchiveProducer(Supplier<JavaArchive> archiveProducer) {
        this.archiveProducer = archiveProducer;
        return this;
    }

    public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext) throws TestInstantiationException {
        try {
            Class testClass = extensionContext.getRequiredTestClass();
            ProxyFactory<?> factory = new ProxyFactory<>(new ProxyConfiguration<>()
                    .setProxyName(testClass.getName() + "$$ShamrockUnitTestProxy")
                    .setClassLoader(testClass.getClassLoader())
                    .setSuperClass(testClass));

            Object actualTestInstance = extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).get(testClass.getName());

            return factory.newInstance(new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (expectedException != null) {
                        return null;
                    }
                    Method realMethod = actualTestInstance.getClass().getMethod(method.getName(), method.getParameterTypes());
                    return realMethod.invoke(actualTestInstance, args);
                }
            });
        } catch (Exception e) {
            throw new TestInstantiationException("Unable to create test proxy", e);
        }
    }


    private void exportArchive(Path deploymentDir, Class<?> testClass) {
        try {
            JavaArchive archive = archiveProducer.get();
            archive.addClass(testClass);
            archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.toFile());

            String exportPath = System.getProperty("shamrock.deploymentExportPath");
            if (exportPath != null) {
                File exportDir = new File(exportPath);
                if (exportDir.exists()) {
                    if (!exportDir.isDirectory()) {
                        throw new IllegalStateException("Export path is not a directory: " + exportPath);
                    }
                    Files.walk(exportDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile)
                            .forEach(File::delete);
                } else if (!exportDir.mkdirs()) {
                    throw new IllegalStateException("Export path could not be created: " + exportPath);
                }
                File exportFile = new File(exportDir, archive.getName());
                archive.as(ZipExporter.class).exportTo(exportFile);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to create the archive", e);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (archiveProducer == null) {
            throw new RuntimeException("ShamrockUnitTest does not have archive producer set");
        }

        ExtensionContext.Store store = extensionContext.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        if (store.get(TestResourceManager.class.getName()) == null) {
            TestResourceManager manager = new TestResourceManager(extensionContext.getRequiredTestClass());
            manager.start();
            store.put(TestResourceManager.class.getName(), new ExtensionContext.Store.CloseableResource() {

                @Override
                public void close() throws Throwable {
                    manager.stop();
                }
            });
        }

        Class<?> testClass = extensionContext.getRequiredTestClass();
        try {
            deploymentDir = Files.createTempDirectory("shamrock-unit-test");

            exportArchive(deploymentDir, testClass);

            List<Consumer<BuildChainBuilder>> customiers = new ArrayList<>();

            try {
                //this is a bit of a hack to avoid requiring a dep on the arc extension,
                //as this would mean we cannot use this to test the extension
                Class<? extends BuildItem> buildItem = (Class<? extends BuildItem>) Class.forName("org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem");
                customiers.add(new Consumer<BuildChainBuilder>() {
                    @Override
                    public void accept(BuildChainBuilder buildChainBuilder) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                try {
                                    Constructor<? extends BuildItem> ctor = buildItem.getConstructor(boolean.class, String[].class);
                                    context.produce(ctor.newInstance(false, new String[]{testClass.getName()}));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }).produces(buildItem)
                                .build();
                    }
                });
            } catch (ClassNotFoundException e) {
                //ignore
            }

            runtimeRunner = new RuntimeRunner(testClass.getClassLoader(), deploymentDir,
                    getTestClassesLocation(testClass), null, new ArrayList<>(), customiers);

            try {
                runtimeRunner.run();
                if (expectedException != null) {
                    fail("Build did not fail");
                }
                started = true;
                InjectionInstance<?> factory = null;
                try {
                    factory = InjectionFactoryTemplate.currentFactory().create(Class.forName(testClass.getName(), true, Thread.currentThread().getContextClassLoader()));
                } catch (Exception e) {
                    throw new TestInstantiationException("Failed to create test instance", e);
                }

                Object actualTest = factory.newInstance();
                extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).put(testClass.getName(), actualTest);
            } catch (Exception e) {
                started = false;
                if (expectedException != null) {
                    if (e instanceof RuntimeException) {
                        Throwable cause = e.getCause();
                        if (cause != null && cause instanceof BuildException) {
                            assertEquals(expectedException,
                                    cause.getCause().getClass(), "Build failed with wrong exception");
                        } else if (cause != null) {
                            assertEquals(expectedException,
                                    cause.getClass(), "Build failed with wrong exception");
                        } else {
                            fail("Unable to unwrap build exception from: " + e);
                        }
                    } else {
                        fail("Unable to unwrap build exception from: " + e);
                    }
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        try {
            if (runtimeRunner != null) {
                runtimeRunner.close();
            }
        } finally {
            if (deploymentDir != null) {
                Files.walkFileTree(deploymentDir, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

}
