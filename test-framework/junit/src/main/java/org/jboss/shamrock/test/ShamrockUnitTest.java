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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import org.jboss.shamrock.runner.RuntimeRunner;
import org.jboss.shamrock.runtime.InjectionFactoryTemplate;
import org.jboss.shamrock.runtime.InjectionInstance;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class ShamrockUnitTest extends BlockJUnit4ClassRunner {

    private static Path deploymentDir;
    private static RuntimeRunner runtimeRunner;

    static boolean started = false;

    public ShamrockUnitTest(Class<?> klass) throws InitializationError {
        super(doSetup(klass));
        started = true;
    }

    @Override
    protected Object createTest() throws Exception {
        InjectionInstance<?> factory = InjectionFactoryTemplate.currentFactory().create(getTestClass().getJavaClass());
        return factory.newInstance();
    }

    static Class<?> doSetup(Class<?> clazz) {
        try {
            Class<?> theClass = clazz;
            Method deploymentMethod = null;
            for (Method m : theClass.getMethods()) {
                if (m.isAnnotationPresent(Deployment.class)) {
                    deploymentMethod = m;
                    break;
                }
            }
            if (deploymentMethod == null) {
                throw new RuntimeException("Could not find @Deployment method on " + theClass);
            }
            if (!Modifier.isStatic(deploymentMethod.getModifiers())) {
                throw new RuntimeException("@Deployment method must be static" + deploymentMethod);
            }

            JavaArchive archive = (JavaArchive) deploymentMethod.invoke(null);
            archive.addClass(theClass);
            deploymentDir = Files.createTempDirectory("shamrock-unit-test");

            archive.as(ExplodedExporter.class).exportExplodedInto(deploymentDir.toFile());

            String classFileName = theClass.getName().replace('.', '/') + ".class";
            URL resource = theClass.getClassLoader().getResource(classFileName);
            String testClassLocation = resource.getPath().substring(0, resource.getPath().length() - classFileName.length());
            runtimeRunner = new RuntimeRunner(clazz.getClassLoader(), deploymentDir, Paths.get(testClassLocation), null, new ArrayList<>());
            runtimeRunner.run();

            String javaClass = clazz.getName();
            return Class.forName(javaClass, true, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Statement withBeforeClasses(Statement statement) {
        Statement existing = super.withBeforeClasses(statement);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                existing.evaluate();

            }
        };
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        Statement existing = super.withAfterClasses(statement);
        return new Statement() {
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
                                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
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
        };
    }
}
