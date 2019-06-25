package io.quarkus.test.common;

import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

public class TestResourceManager {

    private final Set<QuarkusTestResourceLifecycleManager> testResources;
    private Map<String, String> oldSystemProps;

    public TestResourceManager(Class<?> testClass) {
        testResources = getTestResources(testClass);
    }

    public Map<String, String> start() {
        Map<String, String> ret = new HashMap<>();
        for (QuarkusTestResourceLifecycleManager testResource : testResources) {
            try {
                ret.putAll(testResource.start());
            } catch (Exception e) {
                throw new RuntimeException("Unable to start Quarkus test resource " + testResource, e);
            }
        }
        oldSystemProps = new HashMap<>();
        for (Map.Entry<String, String> i : ret.entrySet()) {
            oldSystemProps.put(i.getKey(), System.getProperty(i.getKey()));
            System.setProperty(i.getKey(), i.getValue());
        }
        return ret;
    }

    public void stop() {
        if (oldSystemProps != null) {
            for (Map.Entry<String, String> e : oldSystemProps.entrySet()) {
                if (e.getValue() == null) {
                    System.clearProperty(e.getKey());
                } else {
                    System.setProperty(e.getKey(), e.getValue());
                }

            }
        }
        oldSystemProps = null;
        for (QuarkusTestResourceLifecycleManager testResource : testResources) {
            try {
                testResource.stop();
            } catch (Exception e) {
                throw new RuntimeException("Unable to stop Quarkus test resource " + testResource, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<QuarkusTestResourceLifecycleManager> getTestResources(Class<?> testClass) {
        IndexView index = indexTestClasses(testClass);

        Set<Class<? extends QuarkusTestResourceLifecycleManager>> testResourceRunnerClasses = new LinkedHashSet<>();

        for (AnnotationInstance annotation : index.getAnnotations(DotName.createSimple(QuarkusTestResource.class.getName()))) {
            try {
                testResourceRunnerClasses.add((Class<? extends QuarkusTestResourceLifecycleManager>) Class
                        .forName(annotation.value().asString()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to find the class for the test resource " + annotation.value().asString());
            }
        }

        Set<QuarkusTestResourceLifecycleManager> testResourceRunners = new LinkedHashSet<>();

        for (Class<? extends QuarkusTestResourceLifecycleManager> testResourceRunnerClass : testResourceRunnerClasses) {
            try {
                testResourceRunners.add(testResourceRunnerClass.getConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException("Unable to instantiate the test resource " + testResourceRunnerClass);
            }
        }

        for (QuarkusTestResourceLifecycleManager i : ServiceLoader.load(QuarkusTestResourceLifecycleManager.class)) {
            testResourceRunners.add(i);
        }

        return testResourceRunners;
    }

    private IndexView indexTestClasses(Class<?> testClass) {
        final Indexer indexer = new Indexer();
        final Path testClassesLocation = getTestClassesLocation(testClass);
        try {
            if (Files.isDirectory(testClassesLocation)) {
                indexTestClassesDir(indexer, testClassesLocation);
            } else {
                try (FileSystem jarFs = FileSystems.newFileSystem(testClassesLocation, null)) {
                    for (Path p : jarFs.getRootDirectories()) {
                        indexTestClassesDir(indexer, p);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to index the test-classes/ directory.", e);
        }
        return indexer.complete();
    }

    private void indexTestClassesDir(Indexer indexer, final Path testClassesLocation) throws IOException {
        Files.walkFileTree(testClassesLocation, new FileVisitor<Path>() {
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
    }
}
