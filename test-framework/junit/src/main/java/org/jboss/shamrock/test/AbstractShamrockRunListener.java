package org.jboss.shamrock.test;

import static org.jboss.shamrock.test.PathTestHelper.getTestClassesLocation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

abstract class AbstractShamrockRunListener extends RunListener {

    private final Class<?> testClass;

    private final RunNotifier runNotifier;

    private final Set<ShamrockTestResourceLifecycleManager> testResources;

    private boolean started = false;

    private boolean failed = false;

    protected AbstractShamrockRunListener(Class<?> testClass, RunNotifier runNotifier) {
        this.testClass = testClass;
        this.runNotifier = runNotifier;
        this.testResources = getTestResources(testClass);
    }

    @Override
    public void testStarted(Description description) throws Exception {
        if (!started) {
            List<RunListener> stopListeners = new ArrayList<>();

            try {
                for (ShamrockTestResourceLifecycleManager testResource : testResources) {
                    try {
                        testResource.start();
                    } catch (Exception e) {
                        failed = true;
                        throw new RuntimeException("Unable to start Shamrock test resource " + testResource);
                    }
                    stopListeners.add(0, new RunListener() {
                        @Override
                        public void testRunFinished(Result result) throws Exception {
                            try {
                                testResource.stop();
                            } catch (Exception e) {
                                System.err.println("Unable to stop Shamrock test resource " + testResource);
                            }
                        }
                    });
                }

                try {
                    startShamrock();
                    started = true;
                    stopListeners.add(0, new RunListener() {
                        @Override
                        public void testRunFinished(Result result) throws Exception {
                            try {
                                stopShamrock();
                            } catch (Exception e) {
                                System.err.println("Unable to stop Shamrock");
                            }
                        }
                    });
                } catch (Exception e) {
                    failed = true;
                    throw new RuntimeException("Unable to boot Shamrock", e);
                }
            } finally {
                for (RunListener stopListener : stopListeners) {
                    runNotifier.addListener(stopListener);
                }
            }
        }
    }

    protected abstract void startShamrock() throws Exception;

    protected abstract void stopShamrock() throws Exception;

    protected Class<?> getTestClass() {
        return testClass;
    }

    protected boolean isFailed() {
        return failed;
    }

    protected RunNotifier getRunNotifier() {
        return runNotifier;
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
                    try(InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ)) {
                        indexer.index(inputStream);
                    }
                    catch (Exception e) {
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
