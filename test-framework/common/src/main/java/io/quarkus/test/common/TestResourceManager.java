package io.quarkus.test.common;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class TestResourceManager implements Closeable {

    private final List<TestResourceEntry> sequentialTestResourceEntries;
    private final List<TestResourceEntry> parallelTestResourceEntries;
    private final List<TestResourceEntry> allTestResourceEntries;
    private Map<String, String> oldSystemProps;
    private boolean started = false;

    public TestResourceManager(Class<?> testClass) {
        this(testClass, Collections.emptyList());
    }

    public TestResourceManager(Class<?> testClass, List<TestResourceClassEntry> additionalTestResources) {
        this.parallelTestResourceEntries = new ArrayList<>();
        this.sequentialTestResourceEntries = new ArrayList<>();

        // we need to keep track of duplicate entries to make sure we don't start the same resource
        // multiple times even if there are multiple same @QuarkusTestResource annotations
        Set<TestResourceClassEntry> uniqueEntries = getUniqueTestResourceClassEntries(testClass, additionalTestResources);
        Set<TestResourceClassEntry> remainingUniqueEntries = initParallelTestResources(uniqueEntries);
        initSequentialTestResources(remainingUniqueEntries);

        this.allTestResourceEntries = new ArrayList<>(sequentialTestResourceEntries);
        this.allTestResourceEntries.addAll(parallelTestResourceEntries);
    }

    public void init() {
        for (TestResourceEntry entry : allTestResourceEntries) {
            try {
                entry.getTestResource().init(entry.getArgs());
            } catch (Exception e) {
                throw new RuntimeException("Unable initialize test resource " + entry.getTestResource(), e);
            }
        }
    }

    public Map<String, String> start() {
        started = true;
        Map<String, String> ret = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future> startFutures = new ArrayList<>();
        for (TestResourceEntry entry : parallelTestResourceEntries) {
            try {
                Future startFuture = executor.submit(() -> {
                    Map<String, String> start = entry.getTestResource().start();
                    if (start != null) {
                        ret.putAll(start);
                    }
                });
                startFutures.add(startFuture);
            } catch (Exception e) {
                throw new RuntimeException("Unable to start Quarkus test resource " + entry.getTestResource(), e);
            }
        }

        Future sequentialStartFuture = executor.submit(() -> {
            for (TestResourceEntry entry : sequentialTestResourceEntries) {
                try {

                    Map<String, String> start = entry.getTestResource().start();
                    if (start != null) {
                        ret.putAll(start);
                    }

                    startFutures.add(startFuture);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to start Quarkus test resource " + entry.getTestResource(), e);
                }
            }
        });
        startFutures.add(sequentialStartFuture);

        for (Future future : startFutures) {
            future.get();
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
        for (TestResourceEntry entry : allTestResourceEntries) {
            entry.getTestResource().inject(testInstance);
        }
    }

    public void close() {
        if (!started) {
            return;
        }
        started = false;
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
        for (TestResourceEntry entry : allTestResourceEntries) {
            try {
                entry.getTestResource().stop();
            } catch (Exception e) {
                throw new RuntimeException("Unable to stop Quarkus test resource " + entry.getTestResource(), e);
            }
        }
        try {
            ConfigProviderResolver cpr = ConfigProviderResolver.instance();
            cpr.releaseConfig(cpr.getConfig());
        } catch (Throwable ignored) {
        }
    }

    private Set<TestResourceClassEntry> initParallelTestResources(Set<TestResourceClassEntry> uniqueEntries) {
        Set<TestResourceClassEntry> remainingUniqueEntries = new HashSet<>(uniqueEntries);
        for (TestResourceClassEntry entry : uniqueEntries) {
            if (entry.isParallel()) {
                TestResourceEntry testResourceEntry = buildTestResourceEntry(entry);
                this.parallelTestResourceEntries.add(testResourceEntry);
                remainingUniqueEntries.remove(entry);
            }
        }
        parallelTestResourceEntries.sort(new Comparator<TestResourceEntry>() {

            private final QuarkusTestResourceLifecycleManagerComparator lifecycleManagerComparator = new QuarkusTestResourceLifecycleManagerComparator();

            @Override
            public int compare(TestResourceEntry o1, TestResourceEntry o2) {
                return lifecycleManagerComparator.compare(o1.getTestResource(), o2.getTestResource());
            }
        });

        return remainingUniqueEntries;
    }

    private Set<TestResourceClassEntry> initSequentialTestResources(Set<TestResourceClassEntry> uniqueEntries) {
        Set<TestResourceClassEntry> remainingUniqueEntries = new HashSet<>(uniqueEntries);
        Set<TestResourceEntry> testResourceEntries = new HashSet<>();
        for (TestResourceClassEntry entry : uniqueEntries) {
            if (!entry.isParallel()) {
                TestResourceEntry testResourceEntry = buildTestResourceEntry(entry);
                this.sequentialTestResourceEntries.add(testResourceEntry);
                remainingUniqueEntries.remove(entry);
            }
        }

        for (QuarkusTestResourceLifecycleManager quarkusTestResourceLifecycleManager : ServiceLoader.load(
                QuarkusTestResourceLifecycleManager.class,
                Thread.currentThread().getContextClassLoader())) {
            TestResourceEntry testResourceEntry = new TestResourceEntry(quarkusTestResourceLifecycleManager);
            this.sequentialTestResourceEntries.add(testResourceEntry);
        }

        this.sequentialTestResourceEntries.sort(new Comparator<TestResourceEntry>() {

            private final QuarkusTestResourceLifecycleManagerComparator lifecycleManagerComparator = new QuarkusTestResourceLifecycleManagerComparator();

            @Override
            public int compare(TestResourceEntry o1, TestResourceEntry o2) {
                return lifecycleManagerComparator.compare(o1.getTestResource(), o2.getTestResource());
            }
        });

        return remainingUniqueEntries;
    }

    private TestResourceManager.TestResourceEntry buildTestResourceEntry(TestResourceClassEntry entry) {
        Class<? extends QuarkusTestResourceLifecycleManager> testResourceClass = entry.clazz;
        try {
            return new TestResourceEntry(testResourceClass.getConstructor().newInstance(), entry.args);
        } catch (InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            throw new RuntimeException("Unable to instantiate the test resource " + testResourceClass.getName(), e);
        }
    }

    private Set<TestResourceClassEntry> getUniqueTestResourceClassEntries(Class<?> testClass,
            List<TestResourceClassEntry> additionalTestResources) {
        IndexView index = TestClassIndexer.readIndex(testClass);
        Set<TestResourceClassEntry> uniqueEntries = new HashSet<>();
        for (AnnotationInstance annotation : findQuarkusTestResourceInstances(index)) {
            try {
                Class<? extends QuarkusTestResourceLifecycleManager> testResourceClass = loadTestResourceClassFromTCCL(
                        annotation.value().asString());

                AnnotationValue argsAnnotationValue = annotation.value("initArgs");
                Map<String, String> args;
                if (argsAnnotationValue == null) {
                    args = Collections.emptyMap();
                } else {
                    args = new HashMap<>();
                    AnnotationInstance[] resourceArgsInstances = argsAnnotationValue.asNestedArray();
                    for (AnnotationInstance resourceArgsInstance : resourceArgsInstances) {
                        args.put(resourceArgsInstance.value("name").asString(), resourceArgsInstance.value().asString());
                    }
                }

                boolean isParallel = annotation.value("parallel").asBoolean();

                uniqueEntries.add(new TestResourceClassEntry(testResourceClass, args, isParallel));
            } catch (IllegalArgumentException | SecurityException e) {
                throw new RuntimeException("Unable to instantiate the test resource " + annotation.value().asString(), e);
            }
        }

        uniqueEntries.addAll(additionalTestResources);
        return uniqueEntries;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends QuarkusTestResourceLifecycleManager> loadTestResourceClassFromTCCL(String className) {
        try {
            return (Class<? extends QuarkusTestResourceLifecycleManager>) Class.forName(className, true,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<AnnotationInstance> findQuarkusTestResourceInstances(IndexView index) {
        Set<AnnotationInstance> testResourceAnnotations = new HashSet<>(
                index.getAnnotations(DotName.createSimple(QuarkusTestResource.class.getName())));
        for (AnnotationInstance annotation : index
                .getAnnotations(DotName.createSimple(QuarkusTestResource.List.class.getName()))) {
            Collections.addAll(testResourceAnnotations, annotation.value().asNestedArray());
        }
        return testResourceAnnotations;
    }

    public static class TestResourceClassEntry {

        private Class<? extends QuarkusTestResourceLifecycleManager> clazz;
        private Map<String, String> args;
        private boolean parallel;

        public TestResourceClassEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz, Map<String, String> args,
                boolean parallel) {
            this.clazz = clazz;
            this.args = args;
            this.parallel = parallel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestResourceClassEntry that = (TestResourceClassEntry) o;
            return clazz.equals(that.clazz) && args.equals(that.args) && parallel == that.parallel;
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, args, parallel);
        }

        public boolean isParallel() {
            return parallel;
        }
    }

    private static class TestResourceEntry {

        private final QuarkusTestResourceLifecycleManager testResource;
        private final Map<String, String> args;

        public TestResourceEntry(QuarkusTestResourceLifecycleManager testResource) {
            this(testResource, Collections.emptyMap());
        }

        public TestResourceEntry(QuarkusTestResourceLifecycleManager testResource, Map<String, String> args) {
            this.testResource = testResource;
            this.args = args;
        }

        public QuarkusTestResourceLifecycleManager getTestResource() {
            return testResource;
        }

        public Map<String, String> getArgs() {
            return args;
        }
    }

}
