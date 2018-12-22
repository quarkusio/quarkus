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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;

import org.jboss.builder.BuildException;
import org.jboss.shamrock.runner.RuntimeRunner;
import org.jboss.shamrock.runtime.InjectionFactoryTemplate;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class ShamrockUnitTest extends BlockJUnit4ClassRunner {

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    static boolean started = false;

    // We need to have them static as they are accessed from the super constructor call.
    // It will do as long as we keep the test execution sequential and don't parallelize things.
    private static RuntimeRunner runtimeRunner;
    private static Path deploymentDir;
    private static BuildShouldFailWith buildShouldFailWith;

    public ShamrockUnitTest(Class<?> klass) throws InitializationError {
        // We need to do it this way as we need to refresh the class once Shamrock is started
        super(doSetup(klass));
    }

    @Override
    protected Object createTest() throws Exception {
        InjectionInstance<?> factory = InjectionFactoryTemplate.currentFactory().create(getTestClass().getJavaClass());
        return factory.newInstance();
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        if (started) {
            super.runChild(method, notifier);
        } else if (buildShouldFailWith != null) {
            notifier.fireTestFinished(describeChild(method));
        } else {
            notifier.fireTestIgnored(describeChild(method));
        }
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        Statement existing = super.withAfterClasses(statement);

        return new StopShamrockAndCleanDeploymentDirStatement(existing);
    }

    private static Class<?> doSetup(Class<?> testClass) {
        try {
            deploymentDir = Files.createTempDirectory("shamrock-unit-test");
            Method deploymentMethod = getDeploymentMethod(testClass);
            buildShouldFailWith = deploymentMethod.getAnnotation(BuildShouldFailWith.class);

            exportArchive(deploymentDir, testClass, deploymentMethod);

            runtimeRunner = new RuntimeRunner(testClass.getClassLoader(), deploymentDir,
                    getTestClassesLocation(testClass), null, new ArrayList<>());

            try {
                runtimeRunner.run();
                if (buildShouldFailWith != null) {
                    fail("Build did not fail");
                }
                started = true;
            } catch (Exception e) {
                started = false;
                if (buildShouldFailWith != null) {
                    if (e instanceof RuntimeException) {
                        Throwable cause = e.getCause();
                        if (cause != null && cause instanceof BuildException) {
                            assertEquals("Build failed with wrong exception", buildShouldFailWith.value(),
                                    cause.getCause().getClass());
                        } else {
                            fail("Build did not fail with build exception: " + e);
                        }
                    } else {
                        fail("Unable to unwrap build exception from: " + e);
                    }
                } else {
                    throw e;
                }
            }

            // refresh the class
            return Class.forName(testClass.getName(), true, Thread.currentThread().getContextClassLoader());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Method getDeploymentMethod(Class<?> testClass) {
        for (Method method : testClass.getMethods()) {
            if (method.isAnnotationPresent(Deployment.class)) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new RuntimeException("@Deployment method must be static" + method);
                }

                return method;
            }
        }

        throw new RuntimeException("Could not find @Deployment method on " + testClass);
    }

    private static void exportArchive(Path deploymentDir, Class<?> testClass, Method deploymentMethod) {
        try {
            JavaArchive archive = (JavaArchive) deploymentMethod.invoke(null);
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
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IOException e) {
            throw new RuntimeException("Unable to create the archive", e);
        }
    }

    private class StopShamrockAndCleanDeploymentDirStatement extends Statement {

        private final Statement existing;


        private StopShamrockAndCleanDeploymentDirStatement(Statement existing) {
            this.existing = existing;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                existing.evaluate();
            } finally {
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
    }
}
