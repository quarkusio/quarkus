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

package org.jboss.shamrock.test.common;

import static org.jboss.shamrock.test.common.PathTestHelper.getTestClassesLocation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

public class TestResourceManager {

    private final Set<ShamrockTestResourceLifecycleManager> testResources;

    public TestResourceManager(Class<?> testClass) {
        testResources = getTestResources(testClass);
    }

    public void start() {
        for (ShamrockTestResourceLifecycleManager testResource : testResources) {
            try {
                testResource.start();
            } catch (Exception e) {
                throw new RuntimeException("Unable to start Shamrock test resource " + testResource);
            }
        }
    }

    public void stop() {
        for (ShamrockTestResourceLifecycleManager testResource : testResources) {
            try {
                testResource.stop();
            } catch (Exception e) {
                throw new RuntimeException("Unable to start Shamrock test resource " + testResource);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<ShamrockTestResourceLifecycleManager> getTestResources(Class<?> testClass) {
        IndexView index = indexTestClasses(testClass);

        Set<Class<? extends ShamrockTestResourceLifecycleManager>> testResourceRunnerClasses = new LinkedHashSet<>();

        for (AnnotationInstance annotation : index.getAnnotations(DotName.createSimple(ShamrockTestResource.class.getName()))) {
            try {
                testResourceRunnerClasses.add((Class<? extends ShamrockTestResourceLifecycleManager>) Class
                        .forName(annotation.value().asString()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to find the class for the test resource " + annotation.value().asString());
            }
        }

        Set<ShamrockTestResourceLifecycleManager> testResourceRunners = new LinkedHashSet<>();

        for (Class<? extends ShamrockTestResourceLifecycleManager> testResourceRunnerClass : testResourceRunnerClasses) {
            try {
                testResourceRunners.add(testResourceRunnerClass.getConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException("Unable to instantiate the test resource " + testResourceRunnerClass);
            }
        }

        for (ShamrockTestResourceLifecycleManager i : ServiceLoader.load(ShamrockTestResourceLifecycleManager.class)) {
            testResourceRunners.add(i);
        }

        return testResourceRunners;
    }

    private IndexView indexTestClasses(Class<?> testClass) {
        Indexer indexer = new Indexer();

        try {
            Files.walkFileTree(getTestClassesLocation(testClass), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.toString().endsWith(".class")) {
                        return FileVisitResult.CONTINUE;
                    }
                    try (InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
                        indexer.index(inputStream);
                    } catch (Exception e) {
                        // ignore
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to index the test-classes/ directory.", e);
        }

        return indexer.complete();
    }

}
