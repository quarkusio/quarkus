package io.quarkus.test.common;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class TestResourceManager implements Closeable {

    private final List<QuarkusTestResourceLifecycleManager> testResources;
    private Map<String, String> oldSystemProps;

    public TestResourceManager(Class<?> testClass) {
        testResources = getTestResources(testClass);
    }

    public Map<String, String> start() {
        Map<String, String> ret = new HashMap<>();
        for (QuarkusTestResourceLifecycleManager testResource : testResources) {
            try {
                Map<String, String> start = testResource.start();
                if (start != null) {
                    ret.putAll(start);
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to start Quarkus test resource " + testResource, e);
            }
        }
        oldSystemProps = new HashMap<>();
        for (Map.Entry<String, String> i : ret.entrySet()) {
            oldSystemProps.put(i.getKey(), System.getProperty(i.getKey()));
            if (i.getValue() == null) {
                System.clearProperty(i.getKey());
            } else {
                System.setProperty(i.getKey(), i.getValue());
            }
        }
        return ret;
    }

    public void inject(Object testInstance) {
        for (QuarkusTestResourceLifecycleManager testResource : testResources) {
            testResource.inject(testInstance);
        }
    }

    public void close() {
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
        try {
            ConfigProviderResolver cpr = ConfigProviderResolver.instance();
            cpr.releaseConfig(cpr.getConfig());
        } catch (Throwable ignored) {
        }
    }

    public void cleanup(Object testInstance) {
        for (QuarkusTestResourceLifecycleManager testResource : testResources) {
            if (!QuarkusTestResourceWithCleanupLifecycleManager.class.isAssignableFrom(testResource.getClass())) {
                return;
            }
            QuarkusTestResourceWithCleanupLifecycleManager testResourceWithCleanup = (QuarkusTestResourceWithCleanupLifecycleManager) testResource;
            QuarkusTestResource[] annotations = testInstance.getClass()
                    .getDeclaredAnnotationsByType(QuarkusTestResource.class);
            if (annotations.length > 0) {
                if (Arrays.stream(annotations)
                        .anyMatch(it -> QuarkusTestResourceWithCleanupLifecycleManager.class.isAssignableFrom(it.value()))) {
                    testResourceWithCleanup.cleanup();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<QuarkusTestResourceLifecycleManager> getTestResources(Class<?> testClass) {
        IndexView index = TestClassIndexer.readIndex(testClass);

        Set<Class<? extends QuarkusTestResourceLifecycleManager>> testResourceRunnerClasses = new LinkedHashSet<>();

        Set<AnnotationInstance> testResourceAnnotations = new HashSet<>();
        testResourceAnnotations.addAll(index.getAnnotations(DotName.createSimple(QuarkusTestResource.class.getName())));
        for (AnnotationInstance annotation : index
                .getAnnotations(DotName.createSimple(QuarkusTestResource.List.class.getName()))) {
            Collections.addAll(testResourceAnnotations, annotation.value().asNestedArray());
        }
        for (AnnotationInstance annotation : testResourceAnnotations) {
            try {
                testResourceRunnerClasses.add((Class<? extends QuarkusTestResourceLifecycleManager>) Class
                        .forName(annotation.value().asString(), true, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to find the class for the test resource " + annotation.value().asString());
            }
        }

        List<QuarkusTestResourceLifecycleManager> testResourceRunners = new ArrayList<>();

        for (Class<? extends QuarkusTestResourceLifecycleManager> testResourceRunnerClass : testResourceRunnerClasses) {
            try {
                testResourceRunners.add(testResourceRunnerClass.getConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException("Unable to instantiate the test resource " + testResourceRunnerClass);
            }
        }

        for (QuarkusTestResourceLifecycleManager quarkusTestResourceLifecycleManager : ServiceLoader
                .load(QuarkusTestResourceLifecycleManager.class, Thread.currentThread().getContextClassLoader())) {
            testResourceRunners.add(quarkusTestResourceLifecycleManager);
        }

        Collections.sort(testResourceRunners, new QuarkusTestResourceLifecycleManagerComparator());

        return testResourceRunners;
    }

}
