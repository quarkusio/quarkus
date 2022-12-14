package io.quarkus.test.common;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class TestResourceManager implements Closeable {

    public static final String CLOSEABLE_NAME = TestResourceManager.class.getName() + ".closeable";

    private final List<TestResourceEntry> sequentialTestResourceEntries;
    private final List<TestResourceEntry> parallelTestResourceEntries;
    private final List<TestResourceEntry> allTestResourceEntries;
    private final Map<String, String> configProperties = new ConcurrentHashMap<>();
    private boolean started = false;
    private boolean hasPerTestResources = false;
    private TestStatus testStatus = new TestStatus(null);

    public TestResourceManager(Class<?> testClass) {
        this(testClass, null, Collections.emptyList(), false);
    }

    public TestResourceManager(Class<?> testClass, Class<?> profileClass, List<TestResourceClassEntry> additionalTestResources,
            boolean disableGlobalTestResources) {
        this(testClass, profileClass, additionalTestResources, disableGlobalTestResources, Collections.emptyMap(),
                Optional.empty());
    }

    public TestResourceManager(Class<?> testClass, Class<?> profileClass, List<TestResourceClassEntry> additionalTestResources,
            boolean disableGlobalTestResources, Map<String, String> devServicesProperties,
            Optional<String> containerNetworkId) {
        this(testClass, profileClass, additionalTestResources, disableGlobalTestResources, devServicesProperties,
                containerNetworkId, PathTestHelper.getTestClassesLocation(testClass));
    }

    public TestResourceManager(Class<?> testClass, Class<?> profileClass, List<TestResourceClassEntry> additionalTestResources,
            boolean disableGlobalTestResources, Map<String, String> devServicesProperties,
            Optional<String> containerNetworkId, Path testClassLocation) {
        this.parallelTestResourceEntries = new ArrayList<>();
        this.sequentialTestResourceEntries = new ArrayList<>();

        // we need to keep track of duplicate entries to make sure we don't start the same resource
        // multiple times even if there are multiple same @QuarkusTestResource annotations
        Set<TestResourceClassEntry> uniqueEntries;
        if (disableGlobalTestResources) {
            uniqueEntries = new HashSet<>(additionalTestResources);
        } else {
            uniqueEntries = getUniqueTestResourceClassEntries(testClassLocation, testClass, profileClass,
                    additionalTestResources);
        }
        Set<TestResourceClassEntry> remainingUniqueEntries = initParallelTestResources(uniqueEntries);
        initSequentialTestResources(remainingUniqueEntries);

        this.allTestResourceEntries = new ArrayList<>(sequentialTestResourceEntries);
        this.allTestResourceEntries.addAll(parallelTestResourceEntries);
        DevServicesContext context = new DevServicesContext() {
            @Override
            public Map<String, String> devServicesProperties() {
                return devServicesProperties;
            }

            @Override
            public Optional<String> containerNetworkId() {
                return containerNetworkId;
            }
        };
        for (var i : allTestResourceEntries) {
            if (i.getTestResource() instanceof DevServicesContext.ContextAware) {
                ((DevServicesContext.ContextAware) i.getTestResource()).setIntegrationTestContext(context);
            }
        }
    }

    public void setTestErrorCause(Throwable testErrorCause) {
        this.testStatus = new TestStatus(testErrorCause);
    }

    public void init(String testProfileName) {
        for (TestResourceEntry entry : allTestResourceEntries) {
            try {
                QuarkusTestResourceLifecycleManager testResource = entry.getTestResource();
                testResource.setContext(new QuarkusTestResourceLifecycleManager.Context() {
                    @Override
                    public String testProfile() {
                        return testProfileName;
                    }

                    @Override
                    public TestStatus getTestStatus() {
                        return TestResourceManager.this.testStatus;
                    }
                });
                if (testResource instanceof QuarkusTestResourceConfigurableLifecycleManager
                        && entry.getConfigAnnotation() != null) {
                    ((QuarkusTestResourceConfigurableLifecycleManager<Annotation>) testResource)
                            .init(entry.getConfigAnnotation());
                } else {
                    testResource.init(entry.getArgs());
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable initialize test resource " + entry.getTestResource(), e);
            }
        }
    }

    public Map<String, String> start() {
        started = true;
        Map<String, String> allProps = new ConcurrentHashMap<>();
        int taskSize = parallelTestResourceEntries.size() + 1;
        ExecutorService executor = Executors.newFixedThreadPool(taskSize);
        List<Runnable> tasks = new ArrayList<>(taskSize);
        for (TestResourceEntry entry : parallelTestResourceEntries) {
            tasks.add(new TestResourceEntryRunnable(entry, allProps));
        }
        tasks.add(new TestResourceEntryRunnable(sequentialTestResourceEntries, allProps));

        try {
            // convert the tasks into an array of CompletableFuture
            CompletableFuture
                    .allOf(
                            tasks.stream()
                                    .map(task -> CompletableFuture.runAsync(task, executor))
                                    .toArray(CompletableFuture[]::new))
                    // this returns when all tasks complete
                    .join();
        } finally {
            executor.shutdown();
        }
        configProperties.putAll(allProps);
        return allProps;
    }

    public void inject(Object testInstance) {
        for (TestResourceEntry entry : allTestResourceEntries) {
            QuarkusTestResourceLifecycleManager quarkusTestResourceLifecycleManager = entry.getTestResource();
            quarkusTestResourceLifecycleManager.inject(testInstance);
            quarkusTestResourceLifecycleManager.inject(new DefaultTestInjector(testInstance));
        }
    }

    public void close() {
        if (!started) {
            return;
        }
        started = false;
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
        configProperties.clear();
    }

    public Map<String, String> getConfigProperties() {
        return configProperties;
    }

    private Set<TestResourceClassEntry> initParallelTestResources(Set<TestResourceClassEntry> uniqueEntries) {
        Set<TestResourceClassEntry> remainingUniqueEntries = new LinkedHashSet<>(uniqueEntries);
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
        Set<TestResourceClassEntry> remainingUniqueEntries = new LinkedHashSet<>(uniqueEntries);
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
            return new TestResourceEntry(testResourceClass.getConstructor().newInstance(), entry.args, entry.configAnnotation);
        } catch (InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            throw new RuntimeException("Unable to instantiate the test resource " + testResourceClass.getName(), e);
        }
    }

    private Set<TestResourceClassEntry> getUniqueTestResourceClassEntries(Path testClassLocation, Class<?> testClass,
            Class<?> profileClass,
            List<TestResourceClassEntry> additionalTestResources) {
        IndexView index = TestClassIndexer.readIndex(testClassLocation, testClass);
        Set<TestResourceClassEntry> uniqueEntries = new LinkedHashSet<>();
        // reload the test and profile classes in the right CL
        Class<?> testClassFromTCCL;
        Class<?> profileClassFromTCCL;
        try {
            testClassFromTCCL = Class.forName(testClass.getName(), false, Thread.currentThread().getContextClassLoader());
            if (profileClass != null) {
                profileClassFromTCCL = Class.forName(profileClass.getName(), false,
                        Thread.currentThread().getContextClassLoader());
            } else {
                profileClassFromTCCL = null;
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // handle meta-annotations: in this case we must rely on reflection because meta-annotations are not indexed
        // because they are not in the user's test folder but come from test extensions
        collectMetaAnnotations(testClassFromTCCL, Class::getSuperclass, uniqueEntries);
        collectMetaAnnotations(testClassFromTCCL, Class::getEnclosingClass, uniqueEntries);
        if (profileClassFromTCCL != null) {
            collectMetaAnnotations(profileClassFromTCCL, Class::getSuperclass, uniqueEntries);
        }
        for (AnnotationInstance annotation : findQuarkusTestResourceInstances(testClass, index)) {
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

                boolean isParallel = false;
                AnnotationValue parallelAnnotationValue = annotation.value("parallel");
                if (parallelAnnotationValue != null) {
                    isParallel = parallelAnnotationValue.asBoolean();
                }

                AnnotationValue restrict = annotation.value("restrictToAnnotatedClass");
                if (restrict != null && restrict.asBoolean()) {
                    hasPerTestResources = true;
                }

                uniqueEntries.add(new TestResourceClassEntry(testResourceClass, args, null, isParallel));
            } catch (IllegalArgumentException | SecurityException e) {
                throw new RuntimeException("Unable to instantiate the test resource " + annotation.value().asString(), e);
            }
        }

        uniqueEntries.addAll(additionalTestResources);
        return uniqueEntries;
    }

    private void collectMetaAnnotations(Class<?> testClassFromTCCL, Function<Class<?>, Class<?>> next,
            Set<TestResourceClassEntry> uniqueEntries) {
        while (testClassFromTCCL != null && !testClassFromTCCL.getName().equals("java.lang.Object")) {
            for (Annotation metaAnnotation : testClassFromTCCL.getAnnotations()) {
                for (Annotation ann : metaAnnotation.annotationType().getAnnotations()) {
                    if (ann.annotationType() == QuarkusTestResource.class) {
                        addTestResourceEntry((QuarkusTestResource) ann, metaAnnotation, uniqueEntries);
                        hasPerTestResources = true;
                        break;
                    } else if (ann.annotationType() == QuarkusTestResource.List.class) {
                        for (QuarkusTestResource quarkusTestResource : ((QuarkusTestResource.List) ann).value()) {
                            addTestResourceEntry(quarkusTestResource, metaAnnotation, uniqueEntries);
                        }
                        hasPerTestResources = true;
                        break;
                    }
                }
            }
            testClassFromTCCL = next.apply(testClassFromTCCL);
        }
    }

    private void addTestResourceEntry(QuarkusTestResource quarkusTestResource, Annotation originalAnnotation,
            Set<TestResourceClassEntry> uniqueEntries) {

        // NOTE: we don't need to check restrictToAnnotatedClass because by design config-based annotations
        // are not discovered outside the test class, so they're restricted
        Class<? extends QuarkusTestResourceLifecycleManager> testResourceClass = quarkusTestResource.value();

        ResourceArg[] argsAnnotationValue = quarkusTestResource.initArgs();
        Map<String, String> args;
        if (argsAnnotationValue.length == 0) {
            args = Collections.emptyMap();
        } else {
            args = new HashMap<>();
            for (ResourceArg arg : argsAnnotationValue) {
                args.put(arg.name(), arg.value());
            }
        }
        uniqueEntries
                .add(new TestResourceClassEntry(testResourceClass, args, originalAnnotation, quarkusTestResource.parallel()));
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

    private Collection<AnnotationInstance> findQuarkusTestResourceInstances(Class<?> testClass, IndexView index) {
        // collect all test supertypes for matching per-test targets
        Set<String> testClasses = new HashSet<>();
        Class<?> current = testClass;
        while (current != Object.class) {
            testClasses.add(current.getName());
            current = current.getSuperclass();
        }
        current = testClass.getEnclosingClass();
        while (current != null) {
            testClasses.add(current.getName());
            current = current.getEnclosingClass();
        }
        Set<AnnotationInstance> testResourceAnnotations = new LinkedHashSet<>();
        for (AnnotationInstance annotation : index.getAnnotations(DotName.createSimple(QuarkusTestResource.class.getName()))) {
            if (keepTestResourceAnnotation(annotation, annotation.target().asClass(), testClasses)) {
                testResourceAnnotations.add(annotation);
            }
        }

        for (AnnotationInstance annotation : index
                .getAnnotations(DotName.createSimple(QuarkusTestResource.List.class.getName()))) {
            for (AnnotationInstance nestedAnnotation : annotation.value().asNestedArray()) {
                // keep the list target
                if (keepTestResourceAnnotation(nestedAnnotation, annotation.target().asClass(), testClasses)) {
                    testResourceAnnotations.add(nestedAnnotation);
                }
            }
        }
        return testResourceAnnotations;
    }

    // NOTE: called by reflection in QuarkusTestExtension
    public boolean hasPerTestResources() {
        return hasPerTestResources;
    }

    private boolean keepTestResourceAnnotation(AnnotationInstance annotation, ClassInfo targetClass, Set<String> testClasses) {
        if (targetClass.isAnnotation()) {
            // meta-annotations have already been handled in collectMetaAnnotations
            return false;
        }
        AnnotationValue restrict = annotation.value("restrictToAnnotatedClass");
        if (restrict != null && restrict.asBoolean()) {
            return testClasses.contains(targetClass.name().toString('.'));
        }
        return true;
    }

    public static class TestResourceClassEntry {

        private Class<? extends QuarkusTestResourceLifecycleManager> clazz;
        private Map<String, String> args;
        private boolean parallel;
        private Annotation configAnnotation;

        public TestResourceClassEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz, Map<String, String> args,
                Annotation configAnnotation,
                boolean parallel) {
            this.clazz = clazz;
            this.args = args;
            this.configAnnotation = configAnnotation;
            this.parallel = parallel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestResourceClassEntry that = (TestResourceClassEntry) o;
            return clazz.equals(that.clazz) && args.equals(that.args) && Objects.equals(configAnnotation, that.configAnnotation)
                    && parallel == that.parallel;
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, args, configAnnotation, parallel);
        }

        public boolean isParallel() {
            return parallel;
        }
    }

    private static class TestResourceEntryRunnable implements Runnable {
        private final List<TestResourceEntry> entries;
        private final Map<String, String> allProps;

        public TestResourceEntryRunnable(TestResourceEntry entry,
                Map<String, String> allProps) {
            this(Collections.singletonList(entry), allProps);
        }

        public TestResourceEntryRunnable(List<TestResourceEntry> entries,
                Map<String, String> allProps) {
            this.entries = entries;
            this.allProps = allProps;
        }

        @Override
        public void run() {
            for (TestResourceEntry entry : entries) {
                try {
                    Map<String, String> start = entry.getTestResource().start();
                    if (start != null) {
                        allProps.putAll(start);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Unable to start Quarkus test resource " + entry.getTestResource().getClass().toString(), e);
                }
            }
        }
    }

    private static class TestResourceEntry {

        private final QuarkusTestResourceLifecycleManager testResource;
        private final Map<String, String> args;
        private final Annotation configAnnotation;

        public TestResourceEntry(QuarkusTestResourceLifecycleManager testResource) {
            this(testResource, Collections.emptyMap(), null);
        }

        public TestResourceEntry(QuarkusTestResourceLifecycleManager testResource, Map<String, String> args,
                Annotation configAnnotation) {
            this.testResource = testResource;
            this.args = args;
            this.configAnnotation = configAnnotation;
        }

        public QuarkusTestResourceLifecycleManager getTestResource() {
            return testResource;
        }

        public Map<String, String> getArgs() {
            return args;
        }

        public Annotation getConfigAnnotation() {
            return configAnnotation;
        }
    }

    // visible for testing
    static class DefaultTestInjector implements QuarkusTestResourceLifecycleManager.TestInjector {

        // visible for testing
        final Object testInstance;

        private DefaultTestInjector(Object testInstance) {
            this.testInstance = testInstance;
        }

        @Override
        public void injectIntoFields(Object fieldValue, Predicate<Field> predicate) {
            Class<?> c = testInstance.getClass();
            while (c != Object.class) {
                for (Field f : c.getDeclaredFields()) {
                    if (predicate.test(f)) {
                        f.setAccessible(true);
                        try {
                            f.set(testInstance, fieldValue);
                            return;
                        } catch (Exception e) {
                            throw new RuntimeException("Unable to set field '" + f.getName()
                                    + "' using 'QuarkusTestResourceLifecycleManager.TestInjector' ", e);
                        }
                    }
                }
                c = c.getSuperclass();
            }
            // no need to warn here because it's perfectly valid to have tests that don't use the injected fields
        }

    }

}
