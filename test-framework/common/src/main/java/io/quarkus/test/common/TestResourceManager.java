package io.quarkus.test.common;

import static io.quarkus.test.common.TestResourceScope.GLOBAL;
import static io.quarkus.test.common.TestResourceScope.MATCHING_RESOURCES;
import static io.quarkus.test.common.TestResourceScope.RESTRICTED_TO_CLASS;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

/**
 * Manages {@link QuarkusTestResourceLifecycleManager}
 */
public class TestResourceManager implements Closeable {
    private static final DotName QUARKUS_TEST_RESOURCE = DotName.createSimple(QuarkusTestResource.class);
    private static final DotName WITH_TEST_RESOURCE = DotName.createSimple(WithTestResource.class);
    public static final String CLOSEABLE_NAME = TestResourceManager.class.getName() + ".closeable";

    private final List<TestResourceStartInfo> sequentialTestResources;
    private final List<TestResourceStartInfo> parallelTestResources;
    private final List<TestResourceStartInfo> allTestResources;
    private final Map<String, String> configProperties = new ConcurrentHashMap<>();
    private final Set<TestResourceComparisonInfo> testResourceComparisonInfo;
    private final DevServicesContext devServicesContext;

    private boolean started = false;

    private TestStatus testStatus = new TestStatus(null);

    public TestResourceManager(Class<?> testClass) {
        this(testClass, null, Collections.emptyList(), false);
    }

    public TestResourceManager(Class<?> testClass,
            Class<?> profileClass,
            List<TestResourceClassEntry> additionalTestResources,
            boolean disableGlobalTestResources) {
        this(testClass, profileClass, additionalTestResources, disableGlobalTestResources, Collections.emptyMap(),
                Optional.empty());
    }

    public TestResourceManager(Class<?> testClass,
            Class<?> profileClass,
            List<TestResourceClassEntry> additionalTestResources,
            boolean disableGlobalTestResources,
            Map<String, String> devServicesProperties,
            Optional<String> containerNetworkId) {
        this(testClass, profileClass, additionalTestResources, disableGlobalTestResources, devServicesProperties,
                containerNetworkId, PathTestHelper.getTestClassesLocation(testClass));
    }

    /**
     * This is safe to call as it doesn't do anything other than create state - no {@link QuarkusTestResourceLifecycleManager}
     * is ever touched
     * at this stage.
     */
    public TestResourceManager(Class<?> testClass,
            Class<?> profileClass,
            List<TestResourceClassEntry> additionalTestResources,
            boolean disableGlobalTestResources,
            Map<String, String> devServicesProperties,
            Optional<String> containerNetworkId,
            Path testClassLocation) {
        this.parallelTestResources = new ArrayList<>();
        this.sequentialTestResources = new ArrayList<>();

        // we need to keep track of duplicate entries to make sure we don't start the same resource
        // multiple times even if there are multiple same @WithTestResource annotations
        Set<TestResourceClassEntry> uniqueEntries;
        if (disableGlobalTestResources) {
            uniqueEntries = new HashSet<>(additionalTestResources);
        } else {
            uniqueEntries = uniqueTestResourceClassEntries(testClassLocation, testClass, profileClass,
                    additionalTestResources);
        }

        this.testResourceComparisonInfo = new HashSet<>();
        for (TestResourceClassEntry uniqueEntry : uniqueEntries) {
            testResourceComparisonInfo.add(prepareTestResourceComparisonInfo(uniqueEntry));
        }

        Set<TestResourceClassEntry> remainingUniqueEntries = initParallelTestResources(uniqueEntries);
        initSequentialTestResources(remainingUniqueEntries);

        this.allTestResources = new ArrayList<>(sequentialTestResources);
        this.allTestResources.addAll(parallelTestResources);

        this.devServicesContext = new DevServicesContext() {
            @Override
            public Map<String, String> devServicesProperties() {
                return devServicesProperties;
            }

            @Override
            public Optional<String> containerNetworkId() {
                return containerNetworkId;
            }
        };
        for (var i : allTestResources) {
            if (i.getTestResource() instanceof DevServicesContext.ContextAware) {
                ((DevServicesContext.ContextAware) i.getTestResource()).setIntegrationTestContext(devServicesContext);
            }
        }
    }

    public void setTestErrorCause(Throwable testErrorCause) {
        this.testStatus = new TestStatus(testErrorCause);
    }

    public void init(String testProfileName) {
        for (TestResourceStartInfo entry : allTestResources) {
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
        int taskSize = parallelTestResources.size() + 1;
        ExecutorService executor = Executors.newFixedThreadPool(taskSize);
        List<Runnable> tasks = new ArrayList<>(taskSize);
        for (TestResourceStartInfo entry : parallelTestResources) {
            tasks.add(new TestResourceRunnable(entry, allProps));
        }
        tasks.add(new TestResourceRunnable(sequentialTestResources, allProps));

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
        injectTestContext(testInstance, devServicesContext);
        for (TestResourceStartInfo entry : allTestResources) {
            QuarkusTestResourceLifecycleManager quarkusTestResourceLifecycleManager = entry.getTestResource();
            quarkusTestResourceLifecycleManager.inject(testInstance);
            quarkusTestResourceLifecycleManager.inject(new DefaultTestInjector(testInstance));
        }
    }

    private static void injectTestContext(Object testInstance, DevServicesContext context) {
        Class<?> c = testInstance.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType().equals(DevServicesContext.class)) {
                    try {
                        f.setAccessible(true);
                        f.set(testInstance, context);
                        return;
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to set field '" + f.getName()
                                + "' with the proper test context", e);
                    }
                } else if (DevServicesContext.ContextAware.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    try {
                        DevServicesContext.ContextAware val = (DevServicesContext.ContextAware) f.get(testInstance);
                        val.setIntegrationTestContext(context);
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to inject context into field " + f.getName(), e);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    public void close() {
        if (!started) {
            return;
        }
        started = false;
        for (TestResourceStartInfo entry : allTestResources) {
            try {
                entry.getTestResource().stop();
            } catch (Exception e) {
                throw new RuntimeException("Unable to stop Quarkus test resource " + entry.getTestResource(), e);
            }
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
                TestResourceStartInfo testResourceStartInfo = buildTestResourceEntry(entry);
                this.parallelTestResources.add(testResourceStartInfo);
                remainingUniqueEntries.remove(entry);
            }
        }
        parallelTestResources.sort(new Comparator<TestResourceStartInfo>() {

            private final QuarkusTestResourceLifecycleManagerComparator lifecycleManagerComparator = new QuarkusTestResourceLifecycleManagerComparator();

            @Override
            public int compare(TestResourceStartInfo o1, TestResourceStartInfo o2) {
                return lifecycleManagerComparator.compare(o1.getTestResource(), o2.getTestResource());
            }
        });

        return remainingUniqueEntries;
    }

    private Set<TestResourceClassEntry> initSequentialTestResources(Set<TestResourceClassEntry> uniqueEntries) {
        Set<TestResourceClassEntry> remainingUniqueEntries = new LinkedHashSet<>(uniqueEntries);
        for (TestResourceClassEntry entry : uniqueEntries) {
            if (!entry.isParallel()) {
                TestResourceStartInfo testResourceStartInfo = buildTestResourceEntry(entry);
                this.sequentialTestResources.add(testResourceStartInfo);
                remainingUniqueEntries.remove(entry);
            }
        }

        for (QuarkusTestResourceLifecycleManager quarkusTestResourceLifecycleManager : ServiceLoader.load(
                QuarkusTestResourceLifecycleManager.class,
                Thread.currentThread().getContextClassLoader())) {
            TestResourceStartInfo testResourceStartInfo = new TestResourceStartInfo(quarkusTestResourceLifecycleManager);
            this.sequentialTestResources.add(testResourceStartInfo);
        }

        this.sequentialTestResources.sort(new Comparator<>() {

            private final QuarkusTestResourceLifecycleManagerComparator lifecycleManagerComparator = new QuarkusTestResourceLifecycleManagerComparator();

            @Override
            public int compare(TestResourceStartInfo o1, TestResourceStartInfo o2) {
                return lifecycleManagerComparator.compare(o1.getTestResource(), o2.getTestResource());
            }
        });

        return remainingUniqueEntries;
    }

    private TestResourceStartInfo buildTestResourceEntry(TestResourceClassEntry entry) {
        Class<? extends QuarkusTestResourceLifecycleManager> testResourceClass = (Class<? extends QuarkusTestResourceLifecycleManager>) entry.clazz;
        try {
            return new TestResourceStartInfo(testResourceClass.getConstructor().newInstance(), entry.args,
                    entry.configAnnotation);
        } catch (InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            throw new RuntimeException("Unable to instantiate the test resource " + testResourceClass.getName(), e);
        }
    }

    private Set<TestResourceClassEntry> uniqueTestResourceClassEntries(Path testClassLocation, Class<?> testClass,
            Class<?> profileClass,
            List<TestResourceClassEntry> additionalTestResources) {
        Class<?> profileClassFromTCCL = profileClass != null ? alwaysFromTccl(profileClass) : null;
        Consumer<Set<TestResourceClassEntry>> profileMetaAnnotationsConsumer = null;
        if (profileClassFromTCCL != null) {
            profileMetaAnnotationsConsumer = new ProfileMetaAnnotationsUniqueEntriesConsumer(profileClassFromTCCL);
        }

        Set<TestResourceClassEntry> uniqueEntries = getUniqueTestResourceClassEntries(testClass, testClassLocation,
                profileMetaAnnotationsConsumer);
        uniqueEntries.addAll(additionalTestResources);
        return uniqueEntries;
    }

    /**
     * Allows Quarkus to extract basic information about which test resources a test class will require
     */
    public static Set<TestResourceManager.TestResourceComparisonInfo> testResourceComparisonInfo(Class<?> testClass,
            Path testClassLocation, List<TestResourceClassEntry> entriesFromProfile) {
        Set<TestResourceClassEntry> uniqueEntries = getUniqueTestResourceClassEntries(testClass, testClassLocation, null);
        if (uniqueEntries.isEmpty() && entriesFromProfile.isEmpty()) {
            return Collections.emptySet();
        }
        Set<TestResourceClassEntry> allEntries = new HashSet<>(uniqueEntries);
        allEntries.addAll(entriesFromProfile);
        Set<TestResourceManager.TestResourceComparisonInfo> result = new HashSet<>(allEntries.size());
        for (TestResourceClassEntry entry : allEntries) {
            result.add(prepareTestResourceComparisonInfo(entry));
        }
        return result;
    }

    private static TestResourceComparisonInfo prepareTestResourceComparisonInfo(TestResourceClassEntry entry) {
        Map<String, Object> args;
        if (entry.configAnnotation != null) {
            args = new HashMap<>(entry.args);
            args.put("configAnnotation", entry.configAnnotation.annotationType().getName());
            Method[] annotationAttributes = entry.configAnnotation.annotationType().getDeclaredMethods();
            for (Method annotationAttribute : annotationAttributes) {
                try {
                    args.put(annotationAttribute.getName(), annotationAttribute.invoke(entry.configAnnotation));
                } catch (Exception e) {
                    throw new RuntimeException("Unable to extract configuration values for annotation "
                            + entry.configAnnotation.annotationType().getName(), e);
                }
            }
        } else {
            args = new HashMap<>(entry.args);
        }

        return new TestResourceComparisonInfo(entry.testResourceLifecycleManagerClass().getName(), entry.getScope(),
                args);
    }

    private static Set<TestResourceClassEntry> getUniqueTestResourceClassEntries(Class<?> testClass,
            Path testClassLocation,
            Consumer<Set<TestResourceClassEntry>> afterMetaAnnotationAction) {
        Class<?> testClassFromTCCL = alwaysFromTccl(testClass); // TODO this extra classload is annoying, but sort of necessary because we do lots of class == checks and also casting. It is possible to get rid of it, with some rewrite.

        Set<TestResourceClassEntry> uniqueEntries = new LinkedHashSet<>();

        // handle meta-annotations: in this case we must rely on reflection because meta-annotations are not indexed
        // because they are not in the user's test folder but come from test extensions
        collectMetaAnnotations(testClassFromTCCL, Class::getSuperclass, uniqueEntries);
        collectMetaAnnotations(testClassFromTCCL, Class::getEnclosingClass, uniqueEntries);
        if (afterMetaAnnotationAction != null) {
            afterMetaAnnotationAction.accept(uniqueEntries);
        }
        for (AnnotationInstance annotation : findTestResourceInstancesOfClass(testClass,
                TestClassIndexer.readIndex(testClassLocation, testClass))) {
            uniqueEntries.add(TestResourceClassEntryHandler.produceEntry(annotation));
        }
        return uniqueEntries;
    }

    private static Class<?> alwaysFromTccl(Class<?> testClass) {
        if (testClass.getClassLoader().equals(Thread.currentThread().getContextClassLoader())) {
            return testClass;
        }
        try {
            return Class.forName(testClass.getName(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void collectMetaAnnotations(Class<?> testClassFromTCCL, Function<Class<?>, Class<?>> next,
            Set<TestResourceClassEntry> uniqueEntries) {
        while (testClassFromTCCL != null && !testClassFromTCCL.getName().equals("java.lang.Object")) {
            for (Annotation metaAnnotation : testClassFromTCCL.getAnnotations()) {
                for (Annotation ann : metaAnnotation.annotationType().getAnnotations()) {
                    if (ann.annotationType() == WithTestResource.class) {
                        addTestResourceEntry((WithTestResource) ann, metaAnnotation, uniqueEntries);
                        break;
                    } else if (ann.annotationType() == WithTestResource.List.class) {
                        Arrays.stream(((WithTestResource.List) ann).value())
                                .forEach(res -> addTestResourceEntry(res, metaAnnotation, uniqueEntries));
                        break;
                    } else if (ann.annotationType() == WithTestResourceRepeatable.class) {
                        for (Annotation repeatableMetaAnn : testClassFromTCCL
                                .getAnnotationsByType(((WithTestResourceRepeatable) ann).value())) {
                            WithTestResource quarkusTestResource = repeatableMetaAnn.annotationType()
                                    .getAnnotation(WithTestResource.class);
                            if (quarkusTestResource != null) {
                                addTestResourceEntry(quarkusTestResource, repeatableMetaAnn, uniqueEntries);
                            }
                        }
                    } else if (ann.annotationType() == QuarkusTestResource.class) {
                        addTestResourceEntry((QuarkusTestResource) ann, metaAnnotation, uniqueEntries);
                        break;
                    } else if (ann.annotationType() == QuarkusTestResource.List.class) {
                        for (QuarkusTestResource quarkusTestResource : ((QuarkusTestResource.List) ann).value()) {
                            addTestResourceEntry(quarkusTestResource, metaAnnotation, uniqueEntries);
                        }
                        break;
                    } else if (ann.annotationType() == QuarkusTestResourceRepeatable.class) {
                        for (Annotation repeatableMetaAnn : testClassFromTCCL
                                .getAnnotationsByType(((QuarkusTestResourceRepeatable) ann).value())) {
                            QuarkusTestResource quarkusTestResource = repeatableMetaAnn.annotationType()
                                    .getAnnotation(QuarkusTestResource.class);
                            if (quarkusTestResource != null) {
                                addTestResourceEntry(quarkusTestResource, repeatableMetaAnn, uniqueEntries);
                            }
                        }
                    }
                }
            }
            testClassFromTCCL = next.apply(testClassFromTCCL);
        }
    }

    private static void addTestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> testResourceClass,
            ResourceArg[] argsAnnotationValue, Annotation originalAnnotation,
            boolean parallel, TestResourceScope scope, Set<TestResourceClassEntry> uniqueEntries) {

        // NOTE: we don't need to check restrictToAnnotatedClass because by design config-based annotations
        // are not discovered outside the test class, so they're restricted
        var args = Arrays.stream(argsAnnotationValue)
                .collect(Collectors.toMap(ResourceArg::name, ResourceArg::value));

        uniqueEntries
                .add(new TestResourceClassEntry(testResourceClass, args, originalAnnotation, parallel, scope));
    }

    private static void addTestResourceEntry(WithTestResource quarkusTestResource, Annotation originalAnnotation,
            Set<TestResourceClassEntry> uniqueEntries) {

        addTestResourceEntry(quarkusTestResource.value(), quarkusTestResource.initArgs(), originalAnnotation,
                quarkusTestResource.parallel(), quarkusTestResource.scope(),
                uniqueEntries);
    }

    private static void addTestResourceEntry(QuarkusTestResource quarkusTestResource, Annotation originalAnnotation,
            Set<TestResourceClassEntry> uniqueEntries) {

        addTestResourceEntry(quarkusTestResource.value(), quarkusTestResource.initArgs(), originalAnnotation,
                quarkusTestResource.parallel(), quarkusTestResource.restrictToAnnotatedClass() ? RESTRICTED_TO_CLASS : GLOBAL,
                uniqueEntries);
    }

    private static Collection<AnnotationInstance> findTestResourceInstancesOfClass(Class<?> testClass, IndexView index) {
        // collect all test supertypes for matching per-test targets
        Set<String> currentTestClassHierarchy = new HashSet<>();
        Class<?> current = testClass;
        // If this gets called for an @interface, the superclass will be null.
        while (current != Object.class && current != null) {
            currentTestClassHierarchy.add(current.getName());
            // @interface objects may not have a superclass
            current = current.getSuperclass();
            if (current == null) {
                throw new RuntimeException("Internal error: The class " + testClass
                        + " is not a descendant of Object.class, so cannot be a Quarkus test.");
            }
        }
        current = testClass.getEnclosingClass();
        while (current != null) {
            currentTestClassHierarchy.add(current.getName());
            current = current.getEnclosingClass();
        }

        Set<AnnotationInstance> testResourceAnnotations = new LinkedHashSet<>();

        for (DotName testResourceClasses : List.of(WITH_TEST_RESOURCE, QUARKUS_TEST_RESOURCE)) {
            for (AnnotationInstance annotation : index.getAnnotations(testResourceClasses)) {
                if (keepTestResourceAnnotation(annotation, annotation.target().asClass(), currentTestClassHierarchy)) {
                    testResourceAnnotations.add(annotation);
                }
            }
        }

        for (DotName testResourceListClasses : List.of(DotName.createSimple(WithTestResource.List.class.getName()),
                DotName.createSimple(QuarkusTestResource.List.class.getName()))) {
            for (AnnotationInstance annotation : index.getAnnotations(testResourceListClasses)) {
                for (AnnotationInstance nestedAnnotation : annotation.value().asNestedArray()) {
                    // keep the list target
                    if (keepTestResourceAnnotation(nestedAnnotation, annotation.target().asClass(),
                            currentTestClassHierarchy)) {
                        testResourceAnnotations.add(nestedAnnotation);
                    }
                }
            }
        }

        return testResourceAnnotations;
    }

    private static boolean keepTestResourceAnnotation(AnnotationInstance annotation, ClassInfo targetClass,
            Set<String> currentTestClassHierarchy) {
        if (targetClass.isAnnotation()) {
            // meta-annotations have already been handled in collectMetaAnnotations
            return false;
        }

        if (restrictToAnnotatedClass(annotation)) {
            return currentTestClassHierarchy.contains(targetClass.name().toString('.'));
        }

        return true;
    }

    private static boolean restrictToAnnotatedClass(AnnotationInstance annotation) {
        return TestResourceClassEntryHandler.determineScope(annotation) == RESTRICTED_TO_CLASS
                || TestResourceClassEntryHandler.determineScope(annotation) == MATCHING_RESOURCES;
    }

    /**
     * Provides the basic information needed for comparing the test resources currently in use
     */
    @SuppressWarnings("unused") // called with reflection in TestResourceManagerReflections
    public Set<TestResourceComparisonInfo> testResourceComparisonInfo() {
        return testResourceComparisonInfo;
    }

    /**
     * Quarkus needs to restart if one of the following is true:
     * <ul>
     * <li>at least one the existing test resources is restricted to the test class</li>
     * <li>at least one the next test resources is restricted to the test class</li>
     * <li>different {@code MATCHING_RESOURCE} scoped test resources are being used</li>
     * </ul>
     */
    public static boolean testResourcesRequireReload(Set<TestResourceComparisonInfo> existing,
            Set<TestResourceComparisonInfo> next) {
        if (existing.isEmpty() && next.isEmpty()) {
            return false;
        }

        if (anyResourceRestrictedToClass(existing) || anyResourceRestrictedToClass(next)) {
            return true;
        }

        // now we need to check whether the sets contain the exact same MATCHING_RESOURCE test resources

        Set<TestResourceComparisonInfo> inExistingAndNotNext = new HashSet<>(existing);
        inExistingAndNotNext.removeAll(next);
        if (!inExistingAndNotNext.isEmpty()) {
            return true;
        }

        Set<TestResourceComparisonInfo> inNextAndNotExisting = new HashSet<>(next);
        inNextAndNotExisting.removeAll(existing);
        if (!inNextAndNotExisting.isEmpty()) {
            return true;
        }

        // the sets contain the same objects, so no need to reload
        return false;
    }

    public static String getReloadGroupIdentifier(Set<TestResourceComparisonInfo> existing) {
        // For now, we reload if it's restricted to class scope, and don't otherwise
        String uniquenessModifier = anyResourceRestrictedToClass(existing) ? UUID.randomUUID().toString() : "";
        return existing.stream().map(Object::toString).sorted().collect(Collectors.joining()) + uniquenessModifier;
    }

    private static boolean anyResourceRestrictedToClass(Set<TestResourceComparisonInfo> testResources) {
        for (TestResourceComparisonInfo info : testResources) {
            if (info.scope == RESTRICTED_TO_CLASS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Contains all the metadata that is needed to handle the lifecycle and perform all the bookkeeping associated
     * with a Test Resource.
     * When this information is produced by {@link TestResourceManager}, nothing has yet been started, so interrogating
     * it is perfectly fine.
     */
    public static class TestResourceClassEntry {

        private final Class<? extends QuarkusTestResourceLifecycleManager> clazz;
        private final Map<String, String> args;
        private final boolean parallel;
        private final Annotation configAnnotation;
        private final TestResourceScope scope;

        public TestResourceClassEntry(Class<?> clazz, Map<String, String> args,
                Annotation configAnnotation,
                boolean parallel,
                TestResourceScope scope) {
            this.clazz = (Class<? extends QuarkusTestResourceLifecycleManager>) clazz;
            this.args = args;
            this.configAnnotation = configAnnotation;
            this.parallel = parallel;
            this.scope = scope;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            TestResourceClassEntry entry = (TestResourceClassEntry) object;
            return parallel == entry.parallel && Objects.equals(clazz, entry.clazz) && Objects.equals(args,
                    entry.args) && Objects.equals(configAnnotation, entry.configAnnotation) && scope == entry.scope;
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, args, parallel, configAnnotation, scope);
        }

        public boolean isParallel() {
            return parallel;
        }

        public Class<?> testResourceLifecycleManagerClass() {
            return clazz;
        }

        public TestResourceScope getScope() {
            return scope;
        }
    }

    public record TestResourceComparisonInfo(String testResourceLifecycleManagerClass, TestResourceScope scope,
            Map<String, Object> args) {

    }

    private static class TestResourceRunnable implements Runnable {
        private final List<TestResourceStartInfo> entries;
        private final Map<String, String> allProps;

        public TestResourceRunnable(TestResourceStartInfo entry,
                Map<String, String> allProps) {
            this(Collections.singletonList(entry), allProps);
        }

        public TestResourceRunnable(List<TestResourceStartInfo> entries,
                Map<String, String> allProps) {
            this.entries = entries;
            this.allProps = allProps;
        }

        @Override
        public void run() {
            for (TestResourceStartInfo entry : entries) {
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

    /**
     * Contains all the information necessary to start a {@link QuarkusTestResourceLifecycleManager}
     */
    private static class TestResourceStartInfo {

        private final QuarkusTestResourceLifecycleManager testResource;
        private final Map<String, String> args;
        private final Annotation configAnnotation;

        public TestResourceStartInfo(QuarkusTestResourceLifecycleManager testResource) {
            this(testResource, Collections.emptyMap(), null);
        }

        public TestResourceStartInfo(QuarkusTestResourceLifecycleManager testResource, Map<String, String> args,
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

    /**
     * The entry point to handling the differences between {@link QuarkusTestResource} and {@link WithTestResource}
     * (and whatever else we potentially come up with in the future).
     */
    private sealed interface TestResourceClassEntryHandler
            permits QuarkusTestResourceTestResourceClassEntryHandler, WithTestResourceTestResourceClassEntryHandler {

        List<TestResourceClassEntryHandler> HANDLERS = List
                .of(new QuarkusTestResourceTestResourceClassEntryHandler(),
                        new WithTestResourceTestResourceClassEntryHandler());

        /**
         * Find the {@link TestResourceScope} of the provided annotation
         */
        static TestResourceScope determineScope(AnnotationInstance annotation) {
            for (TestResourceClassEntryHandler handler : HANDLERS) {
                if (handler.appliesTo(annotation)) {
                    return handler.scope(annotation);
                }
            }
            throw new IllegalStateException("Annotation '" + annotation.name() + "' is not supported");
        }

        /**
         * Extract all the metadata needed from the provided annotation
         */
        static TestResourceClassEntry produceEntry(AnnotationInstance annotation) {
            for (TestResourceClassEntryHandler producer : TestResourceClassEntryHandler.HANDLERS) {
                if (producer.appliesTo(annotation)) {
                    return producer.produce(annotation);
                }
            }
            throw new IllegalStateException("Annotation '" + annotation.name() + "' is not supported");
        }

        /**
         * Whether the strategy applies to the current annotation
         */
        boolean appliesTo(AnnotationInstance annotation);

        TestResourceScope scope(AnnotationInstance annotationInstance);

        TestResourceClassEntry produce(AnnotationInstance annotation);
    }

    /**
     * Hold code that is common for handling both {@link QuarkusTestResource} and {@link WithTestResource}
     */
    private static abstract class AbstractTestResourceClassEntryHandler {

        Class<? extends QuarkusTestResourceLifecycleManager> lifecycleManager(AnnotationInstance annotation) {
            return loadTestResourceClassFromTCCL(annotation.value().asString());
        }

        @SuppressWarnings("unchecked")
        Class<? extends QuarkusTestResourceLifecycleManager> loadTestResourceClassFromTCCL(String className) {
            try {
                return (Class<? extends QuarkusTestResourceLifecycleManager>) Class.forName(className, true,
                        Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        Map<String, String> args(AnnotationInstance annotation) {
            AnnotationValue argsAnnotationValue = annotation.value("initArgs");
            Map<String, String> args;
            if (argsAnnotationValue == null) {
                return Collections.emptyMap();
            } else {
                args = new HashMap<>();
                AnnotationInstance[] resourceArgsInstances = argsAnnotationValue.asNestedArray();
                for (AnnotationInstance resourceArgsInstance : resourceArgsInstances) {
                    args.put(resourceArgsInstance.value("name").asString(), resourceArgsInstance.value().asString());
                }
                return args;
            }
        }

        boolean isParallel(AnnotationInstance annotation) {
            AnnotationValue parallelAnnotationValue = annotation.value("parallel");
            if (parallelAnnotationValue != null) {
                return parallelAnnotationValue.asBoolean();
            }
            return false;
        }
    }

    /**
     * Handles {@link QuarkusTestResource}
     */
    private static final class QuarkusTestResourceTestResourceClassEntryHandler extends
            AbstractTestResourceClassEntryHandler
            implements TestResourceClassEntryHandler {

        @Override
        public boolean appliesTo(AnnotationInstance annotation) {
            return QUARKUS_TEST_RESOURCE.equals(annotation.name());
        }

        @Override
        public TestResourceClassEntry produce(AnnotationInstance annotation) {
            return new TestResourceClassEntry(lifecycleManager(annotation), args(annotation), null, isParallel(annotation),
                    scope(annotation));
        }

        @Override
        public TestResourceScope scope(AnnotationInstance annotation) {
            TestResourceScope scope = GLOBAL;
            AnnotationValue restrict = annotation.value("restrictToAnnotatedClass");
            if (restrict != null) {
                if (restrict.asBoolean()) {
                    scope = RESTRICTED_TO_CLASS;
                }
            }
            return scope;
        }
    }

    /**
     * Handles {@link WithTestResource}
     */
    private static final class WithTestResourceTestResourceClassEntryHandler extends
            AbstractTestResourceClassEntryHandler
            implements TestResourceClassEntryHandler {

        @Override
        public boolean appliesTo(AnnotationInstance annotation) {
            return WITH_TEST_RESOURCE.equals(annotation.name());
        }

        @Override
        public TestResourceClassEntry produce(AnnotationInstance annotation) {
            return new TestResourceClassEntry(lifecycleManager(annotation), args(annotation), null, isParallel(annotation),
                    scope(annotation));
        }

        @Override
        public TestResourceScope scope(AnnotationInstance annotation) {
            TestResourceScope scope = MATCHING_RESOURCES;
            AnnotationValue restrict = annotation.value("scope");
            if (restrict != null) {
                scope = TestResourceScope.valueOf(restrict.asEnum());
            }
            return scope;
        }
    }

    private static class ProfileMetaAnnotationsUniqueEntriesConsumer implements Consumer<Set<TestResourceClassEntry>> {
        private final Class<?> profileClassFromTCCL;

        public ProfileMetaAnnotationsUniqueEntriesConsumer(Class<?> profileClassFromTCCL) {
            this.profileClassFromTCCL = profileClassFromTCCL;
        }

        @Override
        public void accept(Set<TestResourceClassEntry> entries) {
            collectMetaAnnotations(profileClassFromTCCL, Class::getSuperclass, entries);
        }
    }
}
