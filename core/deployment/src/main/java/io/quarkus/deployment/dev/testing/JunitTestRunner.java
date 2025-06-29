package io.quarkus.deployment.dev.testing;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.commons.annotation.Testable;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.util.GlobUtil;

/**
 * This class is responsible for running a single run of JUnit tests.
 */
public class JunitTestRunner {

    private static final Logger log = Logger.getLogger(JunitTestRunner.class);
    public static final DotName QUARKUS_TEST = DotName.createSimple("io.quarkus.test.junit.QuarkusTest");
    public static final DotName QUARKUS_MAIN_TEST = DotName.createSimple("io.quarkus.test.junit.main.QuarkusMainTest");
    public static final DotName QUARKUS_INTEGRATION_TEST = DotName.createSimple("io.quarkus.test.junit.QuarkusIntegrationTest");
    public static final DotName TEST_PROFILE = DotName.createSimple("io.quarkus.test.junit.TestProfile");
    public static final DotName TEST = DotName.createSimple(Test.class.getName());
    public static final DotName REPEATED_TEST = DotName.createSimple(RepeatedTest.class.getName());
    public static final DotName PARAMETERIZED_TEST = DotName.createSimple(ParameterizedTest.class.getName());
    public static final DotName TEST_FACTORY = DotName.createSimple(TestFactory.class.getName());
    public static final DotName TEST_TEMPLATE = DotName.createSimple(TestTemplate.class.getName());
    public static final DotName TESTABLE = DotName.createSimple(Testable.class.getName());
    public static final DotName NESTED = DotName.createSimple(Nested.class.getName());
    private static final String ARCHUNIT_FIELDSOURCE_FQCN = "com.tngtech.archunit.junit.FieldSource";
    public static final String FACADE_CLASS_LOADER_NAME = "io.quarkus.test.junit.classloading.FacadeClassLoader";

    private final long runId;
    private final DevModeContext.ModuleInfo moduleInfo;
    private final CuratedApplication testApplication;
    private final ClassScanResult classScanResult;
    private final TestClassUsages testClassUsages;
    private final TestState testState;
    private final List<TestRunListener> listeners;
    List<PostDiscoveryFilter> additionalFilters;
    private final Set<String> includeTags;
    private final Set<String> excludeTags;
    private final Pattern include;
    private final Pattern exclude;
    private final String specificSelection;
    private final List<String> includeEngines;
    private final List<String> excludeEngines;
    private final boolean failingTestsOnly;
    private final TestType testType;

    private volatile boolean testsRunning = false;
    private volatile boolean aborted;
    private QuarkusClassLoader deploymentClassLoader;

    // A stable classloader for loading support classes, which can see more than the CL used to load this class
    private static QuarkusClassLoader firstDeploymentClassLoader;

    //    private static ClassLoader classLoaderForLoadingTests;

    public JunitTestRunner(Builder builder) {
        this.runId = builder.runId;
        this.moduleInfo = builder.moduleInfo;
        this.testApplication = builder.testApplication;
        this.classScanResult = builder.classScanResult;
        this.testClassUsages = builder.testClassUsages;
        this.listeners = builder.listeners;
        this.additionalFilters = builder.additionalFilters;
        this.testState = builder.testState;
        this.includeTags = new HashSet<>(builder.includeTags);
        this.excludeTags = new HashSet<>(builder.excludeTags);
        this.include = builder.include;
        this.exclude = builder.exclude;
        this.specificSelection = builder.specificSelection;
        this.includeEngines = builder.includeEngines;
        this.excludeEngines = builder.excludeEngines;
        this.failingTestsOnly = builder.failingTestsOnly;
        this.testType = builder.testType;
    }

    public Runnable prepare() {
        try {
            long start = System.currentTimeMillis();
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            QuarkusClassLoader tcl = testApplication.createDeploymentClassLoader();
            deploymentClassLoader = tcl;
            if (firstDeploymentClassLoader == null) {
                firstDeploymentClassLoader = deploymentClassLoader;
            }
            LogCapturingOutputFilter logHandler = new LogCapturingOutputFilter(testApplication, true, true,
                    TestSupport.instance()
                            .get()::isDisplayTestOutput);
            Thread.currentThread().setContextClassLoader(tcl);

            Set<UniqueId> allDiscoveredIds = new HashSet<>();
            Set<UniqueId> dynamicIds = new HashSet<>();
            DiscoveryResult quarkusTestClasses = discoverTestClasses();

            Launcher launcher = LauncherFactory.create(LauncherConfig.builder().build());
            LauncherDiscoveryRequestBuilder launchBuilder = LauncherDiscoveryRequestBuilder.request()
                    .selectors(quarkusTestClasses.testClasses.stream().map(DiscoverySelectors::selectClass)
                            .collect(Collectors.toList()));
            launchBuilder.filters(new PostDiscoveryFilter() {
                @Override
                public FilterResult apply(TestDescriptor testDescriptor) {
                    allDiscoveredIds.add(testDescriptor.getUniqueId());
                    return FilterResult.included(null);
                }
            });
            if (classScanResult != null) {
                launchBuilder.filters(testClassUsages.getTestsToRun(classScanResult.getChangedClassNames(), testState));
            }
            if (!includeTags.isEmpty()) {
                launchBuilder.filters(TagFilter.includeTags(new ArrayList<>(includeTags)));
            } else if (!excludeTags.isEmpty()) {
                launchBuilder.filters(TagFilter.excludeTags(new ArrayList<>(excludeTags)));
            }
            if (specificSelection != null) {
                if (specificSelection.startsWith("maven:")) {
                    launchBuilder.filters(new MavenSpecificSelectionFilter(specificSelection.substring("maven:".length())));
                } else if (specificSelection.startsWith("gradle:")) {
                    launchBuilder.filters(new GradleSpecificSelectionFilter(specificSelection.substring("gradle:".length())));
                } else {
                    log.error("Unknown specific selection, ignoring: " + specificSelection);
                }
            } else if (include != null) {
                launchBuilder.filters(new RegexFilter(false, include));
            } else if (exclude != null) {
                launchBuilder.filters(new RegexFilter(true, exclude));
            }
            if (!includeEngines.isEmpty()) {
                launchBuilder.filters(EngineFilter.includeEngines(includeEngines));
            } else if (!excludeEngines.isEmpty()) {
                launchBuilder.filters(EngineFilter.excludeEngines(excludeEngines));
            }
            if (!additionalFilters.isEmpty()) {
                launchBuilder.filters(additionalFilters.toArray(new PostDiscoveryFilter[0]));
            }
            if (failingTestsOnly) {
                launchBuilder.filters(new CurrentlyFailingFilter());
            }

            LauncherDiscoveryRequest request = launchBuilder
                    .build();
            TestPlan testPlan = launcher.discover(request);
            long toRun = testPlan.countTestIdentifiers(TestIdentifier::isTest);
            for (TestRunListener listener : listeners) {
                listener.runStarted(toRun);
            }
            return new Runnable() {
                @Override
                public void run() {
                    final ClassLoader origCl = Thread.currentThread().getContextClassLoader();
                    try {
                        synchronized (JunitTestRunner.this) {
                            testsRunning = true;
                        }
                        log.debug("Starting test run with " + testPlan.countTestIdentifiers((s) -> true) + " tests");
                        QuarkusConsole.addOutputFilter(logHandler);

                        final Deque<Set<String>> touchedClasses = new LinkedBlockingDeque<>();
                        Map<TestIdentifier, Long> startTimes = new HashMap<>();
                        final AtomicReference<Set<String>> startupClasses = new AtomicReference<>();
                        TracingHandler.setTracingHandler(new TracingHandler.TraceListener() {
                            @Override
                            public void touched(String className) {
                                Set<String> set = touchedClasses.peek();
                                if (set != null) {
                                    set.add(className);
                                }
                            }

                            @Override
                            public void quarkusStarting() {
                                startupClasses.set(touchedClasses.peek());
                            }
                        });

                        Map<String, Map<UniqueId, TestResult>> resultsByClass = new HashMap<>();
                        AtomicReference<TestIdentifier> currentNonDynamicTest = new AtomicReference<>();

                        Thread.currentThread().setContextClassLoader(tcl);
                        launcher.execute(testPlan, new TestExecutionListener() {

                            @Override
                            public void executionStarted(TestIdentifier testIdentifier) {
                                if (aborted) {
                                    return;
                                }
                                boolean dynamic = dynamicIds.contains(UniqueId.parse(testIdentifier.getUniqueId()));
                                if (!dynamic) {
                                    currentNonDynamicTest.set(testIdentifier);
                                }
                                startTimes.put(testIdentifier, System.currentTimeMillis());
                                String testClassName = "";
                                Class<?> testClass = getTestClassFromSource(testIdentifier.getSource());
                                if (testClass != null) {
                                    testClassName = testClass.getName();
                                }
                                for (TestRunListener listener : listeners) {
                                    listener.testStarted(testIdentifier, testClassName);
                                }
                                touchedClasses.push(Collections.synchronizedSet(new HashSet<>()));
                            }

                            @Override
                            public void executionSkipped(TestIdentifier testIdentifier, String reason) {
                                if (aborted) {
                                    return;
                                }
                                touchedClasses.pop();
                                Class<?> testClass = getTestClassFromSource(testIdentifier.getSource());
                                String displayName = getDisplayNameFromIdentifier(testIdentifier, testClass);
                                UniqueId id = UniqueId.parse(testIdentifier.getUniqueId());
                                if (testClass != null) {
                                    Map<UniqueId, TestResult> results = resultsByClass.computeIfAbsent(testClass.getName(),
                                            s -> new HashMap<>());
                                    TestResult result = new TestResult(displayName, testClass.getName(),
                                            toTagList(testIdentifier),
                                            id, TestExecutionResult.aborted(null),
                                            logHandler.captureOutput(), testIdentifier.isTest(), runId, 0, true);
                                    results.put(id, result);
                                    if (result.isTest()) {
                                        for (TestRunListener listener : listeners) {
                                            listener.testComplete(result);
                                        }
                                    }
                                }
                                touchedClasses.push(Collections.synchronizedSet(new HashSet<>()));
                            }

                            @Override
                            public void dynamicTestRegistered(TestIdentifier testIdentifier) {
                                dynamicIds.add(UniqueId.parse(testIdentifier.getUniqueId()));
                                for (TestRunListener listener : listeners) {
                                    listener.dynamicTestRegistered(testIdentifier);
                                }
                            }

                            @Override
                            public void executionFinished(TestIdentifier testIdentifier,
                                    TestExecutionResult testExecutionResult) {
                                if (aborted) {
                                    return;
                                }
                                boolean dynamic = dynamicIds.contains(UniqueId.parse(testIdentifier.getUniqueId()));
                                Set<String> touched = touchedClasses.pop();
                                Class<?> testClass = getTestClassFromSource(testIdentifier.getSource());
                                String displayName = getDisplayNameFromIdentifier(testIdentifier, testClass);
                                UniqueId id = UniqueId.parse(testIdentifier.getUniqueId());

                                if (testClass == null) {
                                    return;
                                }
                                String testClassName = testClass.getName();

                                if (testExecutionResult.getStatus() != TestExecutionResult.Status.ABORTED) {
                                    for (Set<String> i : touchedClasses) {
                                        //also add the parent touched classes
                                        touched.addAll(i);
                                    }
                                    if (startupClasses.get() != null) {
                                        touched.addAll(startupClasses.get());
                                    }
                                    if (testIdentifier.getSource().map(ClassSource.class::isInstance).orElse(false)) {
                                        testClassUsages.updateTestData(testClassName, touched);
                                    } else {
                                        testClassUsages.updateTestData(testClassName, id, touched);
                                    }
                                }
                                Map<UniqueId, TestResult> results = resultsByClass.computeIfAbsent(testClassName,
                                        s -> new HashMap<>());
                                TestResult result = new TestResult(displayName, testClassName,
                                        toTagList(testIdentifier),
                                        id, testExecutionResult,
                                        logHandler.captureOutput(), testIdentifier.isTest(), runId,
                                        System.currentTimeMillis() - startTimes.get(testIdentifier), true);
                                if (!results.containsKey(id)) {
                                    //if a child has failed we may have already marked the parent failed
                                    results.put(id, result);
                                }
                                if (result.isTest()) {
                                    for (TestRunListener listener : listeners) {
                                        listener.testComplete(result);
                                    }
                                    if (dynamic && testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                                        //if it is dynamic we fail the parent as well for re-runs

                                        RuntimeException failure = new RuntimeException("A child test failed");
                                        failure.setStackTrace(new StackTraceElement[0]);
                                        results.put(id,
                                                new TestResult(currentNonDynamicTest.get().getDisplayName(),
                                                        result.getTestClass(),
                                                        toTagList(testIdentifier),
                                                        currentNonDynamicTest.get().getUniqueIdObject(),
                                                        TestExecutionResult.failed(failure), List.of(), false, runId, 0,
                                                        false));
                                        results.put(UniqueId.parse(currentNonDynamicTest.get().getUniqueId()), result);
                                    } else if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                                        Throwable throwable = testExecutionResult.getThrowable().get();
                                        trimStackTrace(testClass, throwable);
                                        for (var i : throwable.getSuppressed()) {
                                            trimStackTrace(testClass, i);
                                        }
                                    }
                                } else if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                                    //if a parent fails we fail the children
                                    Set<TestIdentifier> children = testPlan.getChildren(testIdentifier);
                                    for (TestIdentifier child : children) {
                                        UniqueId childId = UniqueId.parse(child.getUniqueId());
                                        result = new TestResult(child.getDisplayName(), testClassName,
                                                toTagList(testIdentifier),
                                                childId,
                                                testExecutionResult,
                                                logHandler.captureOutput(), child.isTest(), runId,
                                                System.currentTimeMillis() - startTimes.get(testIdentifier), true);
                                        results.put(childId, result);
                                        if (child.isTest()) {
                                            for (TestRunListener listener : listeners) {
                                                listener.testStarted(child, testClassName);
                                                listener.testComplete(result);
                                            }
                                        }
                                    }

                                    Throwable throwable = testExecutionResult.getThrowable().get();
                                    trimStackTrace(testClass, throwable);
                                    for (var i : throwable.getSuppressed()) {
                                        trimStackTrace(testClass, i);
                                    }
                                }
                            }

                            @Override
                            public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {

                            }
                        });
                        if (aborted) {
                            return;
                        }
                        testState.updateResults(resultsByClass);
                        testState.pruneDeletedTests(allDiscoveredIds, dynamicIds);
                        if (classScanResult != null) {
                            testState.classesRemoved(classScanResult.getDeletedClassNames());
                        }

                        QuarkusConsole.removeOutputFilter(logHandler);
                        for (TestRunListener listener : listeners) {
                            listener.runComplete(new TestRunResults(runId, classScanResult, classScanResult == null, start,
                                    System.currentTimeMillis(), toResultsMap(testState.getCurrentResults())));
                        }
                    } finally {
                        try {
                            TracingHandler.setTracingHandler(null);
                            QuarkusConsole.removeOutputFilter(logHandler);
                            Thread.currentThread().setContextClassLoader(old);
                            tcl.close();
                            try {
                                quarkusTestClasses.close();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } finally {
                            Thread.currentThread().setContextClassLoader(origCl);
                            synchronized (JunitTestRunner.this) {
                                testsRunning = false;
                                if (aborted) {
                                    JunitTestRunner.this.notifyAll();
                                }
                            }
                        }

                    }
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<String> toTagList(TestIdentifier testIdentifier) {
        return testIdentifier
                .getTags()
                .stream()
                .map(TestTag::getName)
                .sorted()
                .toList();
    }

    private Class<?> getTestClassFromSource(Optional<TestSource> optionalTestSource) {
        if (optionalTestSource.isPresent()) {
            var testSource = optionalTestSource.get();
            if (testSource instanceof ClassSource classSource) {
                return classSource.getJavaClass();
            } else if (testSource instanceof MethodSource methodSource) {
                return methodSource.getJavaClass();
            } else if (testSource.getClass().getName().equals(ARCHUNIT_FIELDSOURCE_FQCN)) {
                try {
                    return (Class<?>) testSource.getClass().getMethod("getJavaClass").invoke(testSource);
                } catch (ReflectiveOperationException e) {
                    log.warnf(e, "Failed to read javaClass reflectively from %s. ArchUnit >= 0.23.0 is required.", testSource);
                }
            }
        }
        return null;
    }

    private String getDisplayNameFromIdentifier(TestIdentifier testIdentifier, Class<?> testClass) {
        if (testIdentifier.getSource().isPresent() && testClass != null) {
            var testSource = testIdentifier.getSource().get();
            if (testSource instanceof ClassSource) {
                return testIdentifier.getDisplayName();
            } else if (testSource instanceof MethodSource
                    || testSource.getClass().getName().equals(ARCHUNIT_FIELDSOURCE_FQCN)) {
                return testClass.getSimpleName() + "#" + testIdentifier.getDisplayName();
            }
        }
        return testIdentifier.getDisplayName();
    }

    private void trimStackTrace(Class<?> testClass, Throwable throwable) {
        if (testClass != null) {
            //first we cut all the platform stuff out of the stack trace
            Throwable cause = throwable;
            while (cause != null) {
                StackTraceElement[] st = cause.getStackTrace();
                for (int i = st.length - 1; i >= 0; --i) {
                    StackTraceElement elem = st[i];
                    if (elem.getClassName().equals(testClass.getName())) {
                        StackTraceElement[] newst = new StackTraceElement[i + 1];
                        System.arraycopy(st, 0, newst, 0, i + 1);
                        st = newst;
                        break;
                    }
                }

                //now cut out all the restassured internals
                //TODO: this should be pluggable
                for (int i = st.length - 1; i >= 0; --i) {
                    StackTraceElement elem = st[i];
                    if (elem.getClassName().startsWith("io.restassured")) {
                        StackTraceElement[] newst = new StackTraceElement[st.length - i];
                        System.arraycopy(st, i, newst, 0, st.length - i);
                        st = newst;
                        break;
                    }
                }
                cause.setStackTrace(st);
                cause = cause.getCause();
            }
        }
    }

    public synchronized void abort() {
        for (TestRunListener listener : listeners) {
            try {
                listener.runAborted();
            } catch (Throwable t) {
                log.error("Failed to invoke test listener", t);
            }
        }
        aborted = true;
        while (testsRunning) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Map<String, TestClassResult> toResultsMap(
            Map<String, Map<UniqueId, TestResult>> resultsByClass) {
        Map<String, TestClassResult> resultMap = new HashMap<>();
        Set<String> classes = new HashSet<>(resultsByClass.keySet());
        for (String clazz : classes) {
            List<TestResult> passing = new ArrayList<>();
            List<TestResult> failing = new ArrayList<>();
            List<TestResult> skipped = new ArrayList<>();
            long time = 0;
            for (TestResult i : Optional.ofNullable(resultsByClass.get(clazz)).orElse(Collections.emptyMap()).values()) {
                if (i.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failing.add(i);
                } else if (i.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED) {
                    skipped.add(i);
                } else {
                    passing.add(i);
                }
                if (i.getUniqueId().getLastSegment().getType().equals("class")) {
                    time = i.time;
                }
            }
            resultMap.put(clazz, new TestClassResult(clazz, passing, failing, skipped, time));
        }
        return resultMap;
    }

    private DiscoveryResult discoverTestClasses() {
        //maven has a lot of rules around this and is configurable
        //for now this is out of scope, we are just going to do annotation based discovery
        //we will need to fix this sooner rather than later though

        if (moduleInfo.getTest().isEmpty()) {
            return DiscoveryResult.EMPTY;
        }

        //we also only run tests from the current module, which we can also revisit later
        Indexer indexer = new Indexer();
        try (Stream<Path> files = Files.walk(Paths.get(moduleInfo.getTest().get().getClassesPath()))) {
            files.filter(s -> s.getFileName().toString().endsWith(".class")).forEach(s -> {
                try (InputStream in = Files.newInputStream(s)) {
                    indexer.index(in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Index index = indexer.complete();
        //we now have all the classes by name
        //these tests we never run
        Set<String> integrationTestClasses = new HashSet<>();
        for (AnnotationInstance i : index.getAnnotations(QUARKUS_INTEGRATION_TEST)) {
            DotName name = i.target().asClass().name();
            integrationTestClasses.add(name.toString());
            for (ClassInfo clazz : index.getAllKnownSubclasses(name)) {
                integrationTestClasses.add(clazz.name().toString());
            }
        }
        Set<String> quarkusTestClasses = new HashSet<>();
        for (var a : Arrays.asList(QUARKUS_TEST, QUARKUS_MAIN_TEST)) {
            for (AnnotationInstance i : index.getAnnotations(a)) {

                DotName name = i.target()
                        .asClass()
                        .name();
                quarkusTestClasses.add(name.toString());
                for (ClassInfo clazz : index.getAllKnownSubclasses(name)) {
                    if (!integrationTestClasses.contains(clazz.name().toString())) {
                        quarkusTestClasses.add(clazz.name().toString());
                    }
                }
            }
        }

        // The FacadeClassLoader approach of loading test classes with the classloader we will use to run them can only work for `@QuarkusTest` and not main or integration tests
        // Most logic in the JUnitRunner counts main tests as quarkus tests, so do a (mildly irritating) special pass to get the ones which are strictly @QuarkusTest

        Set<String> quarkusTestClassesForFacadeClassLoader = new HashSet<>();
        for (AnnotationInstance i : index.getAnnotations(QUARKUS_TEST)) {
            DotName name = i.target()
                    .asClass()
                    .name();
            quarkusTestClassesForFacadeClassLoader.add(name.toString());
            for (ClassInfo clazz : index.getAllKnownSubclasses(name)) {
                if (!integrationTestClasses.contains(clazz.name()
                        .toString())) {
                    quarkusTestClassesForFacadeClassLoader.add(clazz.name()
                            .toString());
                }
            }
        }

        Map<String, String> profiles = new HashMap<>();

        for (AnnotationInstance i : index.getAnnotations(TEST_PROFILE)) {

            DotName name = i.target()
                    .asClass()
                    .name();
            // We could do the value as a class, but it wouldn't be in the right classloader
            profiles.put(name.toString(), i.value().asString());
        }

        Set<DotName> allTestAnnotations = collectTestAnnotations(index);
        // Order matters here for nested tests
        // We assume we have evaluated the parent of a class before evaluating it
        Set<DotName> allTestClasses = new TreeSet<>();
        Map<DotName, DotName> enclosingClasses = new HashMap<>();
        for (DotName annotation : allTestAnnotations) {
            for (AnnotationInstance instance : index.getAnnotations(annotation)) {
                if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
                    ClassInfo classInfo = instance.target().asMethod().declaringClass();
                    allTestClasses.add(classInfo.name());
                    if (classInfo.declaredAnnotation(NESTED) != null) {
                        var enclosing = classInfo.enclosingClass();
                        if (enclosing != null) {
                            enclosingClasses.put(classInfo.name(), enclosing);
                        }
                    }
                } else if (instance.target().kind() == AnnotationTarget.Kind.FIELD) {
                    ClassInfo classInfo = instance.target().asField().declaringClass();
                    allTestClasses.add(classInfo.name());
                    if (classInfo.declaredAnnotation(NESTED) != null) {
                        var enclosing = classInfo.enclosingClass();
                        if (enclosing != null) {
                            enclosingClasses.put(classInfo.name(), enclosing);
                        }
                    }
                }
            }
        }
        //now we have all the classes with @Test
        //figure out which ones we want to actually run
        Set<String> unitTestClasses = new HashSet<>();
        for (DotName testClass : allTestClasses) {
            String name = testClass.toString();
            if (integrationTestClasses.contains(name)
                    || quarkusTestClasses.contains(name)) {
                continue;
            }
            var enclosing = enclosingClasses.get(testClass);
            if (enclosing != null) {
                final String enclosingString = enclosing.toString();
                if (quarkusTestClassesForFacadeClassLoader.contains(enclosingString)) {
                    quarkusTestClassesForFacadeClassLoader.add(name);
                }

                // No else here, this is an 'also do'
                if (integrationTestClasses.contains(enclosingString)) {
                    integrationTestClasses.add(name);
                    continue;
                } else if (quarkusTestClasses.contains(enclosingString)) {
                    quarkusTestClasses.add(name);
                    continue;
                }
            }
            ClassInfo clazz = index.getClassByName(testClass);
            if (Modifier.isAbstract(clazz.flags())) {
                continue;
            }
            unitTestClasses.add(name);
        }

        // if we didn't find any test classes, let's return early
        // Make sure you also update the logic for the non-empty case above if you adjust this part
        if (testType == TestType.ALL) {
            if (unitTestClasses.isEmpty() && quarkusTestClasses.isEmpty()) {
                return DiscoveryResult.EMPTY;
            }
        } else if (testType == TestType.UNIT) {
            if (unitTestClasses.isEmpty()) {
                return DiscoveryResult.EMPTY;
            }
        } else if (quarkusTestClasses.isEmpty()) {
            return DiscoveryResult.EMPTY;
        }

        List<Class<?>> itClasses = new ArrayList<>();
        List<Class<?>> utClasses = new ArrayList<>();

        ClassLoader classLoaderForLoadingTests;
        Closeable classLoaderToClose = null;
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            // JUnitTestRunner is loaded with an augmentation classloader which does not have visibility of FacadeClassLoader, but the deployment classloader can see it
            // We need a consistent classloader or we leak curated applications, so use a static classloader we stashed away
            Class fclClazz = firstDeploymentClassLoader.loadClass(FACADE_CLASS_LOADER_NAME);
            Constructor constructor = fclClazz.getConstructor(ClassLoader.class, boolean.class, CuratedApplication.class,
                    Map.class,
                    Set.class,
                    String.class);

            // Passing in the test classes is necessary because in dev mode getAnnotations() on the class returns an empty array, for some reason (plus it saves rediscovery effort)
            String classPath = moduleInfo.getMain()
                    .getClassesPath() + File.pathSeparator + moduleInfo.getTest().get().getClassesPath();
            classLoaderForLoadingTests = (ClassLoader) constructor.newInstance(Thread.currentThread()
                    .getContextClassLoader(), true, testApplication, profiles, quarkusTestClassesForFacadeClassLoader,
                    classPath);
            // We only want to close classloaders if they're facade loaders we made, so squirrel away an instance to close on this path
            classLoaderToClose = (Closeable) classLoaderForLoadingTests;

            Thread.currentThread()
                    .setContextClassLoader(classLoaderForLoadingTests);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException
                | InvocationTargetException e) {
            // This is fine, and usually just means that test-framework/junit5 isn't one of the project dependencies
            // In that case, fallback to loading classes as we normally would, using a TCCL
            log.debug(
                    "Could not load class for FacadeClassLoader. This might be because quarkus-junit5 is not on the project classpath: "
                            + e);
            log.debug(e);
            classLoaderForLoadingTests = Thread.currentThread()
                    .getContextClassLoader();
        }

        for (String i : quarkusTestClasses) {
            try {
                // We could load these classes directly, since we know the profile and we have a handy interception point;
                // but we need to signal to the downstream interceptor that it shouldn't interfere with the classloading
                // While we're doing that, we may as well share the classloading logic
                itClasses.add(classLoaderForLoadingTests.loadClass(i));
            } catch (Exception e) {
                log.debug(e);
                log.warnf(
                        "Failed to load test class %s (possibly as it was added after the test run started), it will not be executed this run.",
                        i);
            }
        }
        itClasses.sort(Comparator.comparing(new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> aClass) {
                ClassInfo def = index.getClassByName(DotName.createSimple(aClass.getName()));
                AnnotationInstance testProfile = def.declaredAnnotation(TEST_PROFILE);
                if (testProfile == null) {
                    return "$$" + aClass.getName();
                }
                return testProfile.value().asClass().name().toString() + "$$" + aClass.getName();
            }
        }));
        QuarkusClassLoader cl = null;
        if (!unitTestClasses.isEmpty()) {
            //we need to work the unit test magic
            //this is a lot more complex
            //we need to transform the classes to make the tracing magic work

            Set<String> classesToTransform = new HashSet<>(deploymentClassLoader.getReloadableClassNames());
            // this won't be the right classloader for some profiles, but that is ok because it's only for vanilla tests
            Map<String, byte[]> transformedClasses = new HashMap<>();
            for (String i : classesToTransform) {
                try {
                    String resourceName = fromClassNameToResourceName(i);
                    byte[] classData = IoUtil
                            .readBytes(deploymentClassLoader.getResourceAsStream(resourceName));
                    ClassReader cr = new ClassReader(classData);
                    ClassWriter writer = new QuarkusClassWriter(cr,
                            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    cr.accept(new TestTracingProcessor.TracingClassVisitor(writer, i), 0);
                    transformedClasses.put(resourceName, writer.toByteArray());
                } catch (Exception e) {
                    log.error("Failed to instrument " + i + " for usage tracking", e);
                }
            }
            cl = testApplication.createDeploymentClassLoader();
            deploymentClassLoader = cl;
            cl.reset(Collections.emptyMap(), transformedClasses);
            for (String i : unitTestClasses) {
                try {
                    utClasses.add(cl.loadClass(i));
                } catch (ClassNotFoundException exception) {
                    log.warnf(
                            "Failed to load test class %s (possibly as it was added after the test run started), it will not be executed this run.",
                            i);
                }
            }

        }

        if (classLoaderToClose != null) {
            try {
                classLoaderToClose.close();
                // Don't leave a closed classloader as the TCCL
                Thread.currentThread().setContextClassLoader(orig);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Make sure you also update the logic for the empty case above if you adjust this part
        if (testType == TestType.ALL) {
            //run unit style tests first
            //before the quarkus tests have started
            //which stops quarkus interfering with WireMock
            List<Class<?>> ret = new ArrayList<>(utClasses.size() + itClasses.size());
            ret.addAll(utClasses);
            ret.addAll(itClasses);
            return new DiscoveryResult(cl, ret);
        } else if (testType == TestType.UNIT) {
            return new DiscoveryResult(cl, utClasses);
        } else {
            return new DiscoveryResult(cl, itClasses);
        }
    }

    private static Set<DotName> collectTestAnnotations(Index index) {
        //todo: read from the full index
        //TODO: this is not 100% correct, discovery needs to be based on class name
        //we can fix it when someone complains
        Set<DotName> ret = new HashSet<>();
        ret.add(TEST);
        ret.add(REPEATED_TEST);
        ret.add(PARAMETERIZED_TEST);
        ret.add(TEST_FACTORY);
        ret.add(TEST_TEMPLATE);
        ret.add(TESTABLE);
        Set<DotName> metaAnnotations = new HashSet<>(ret);
        metaAnnotations.add(TESTABLE);
        //these annotations can also be used as meta annotations
        //so we take this into account
        for (DotName an : metaAnnotations) {
            for (AnnotationInstance instance : index.getAnnotations(an)) {
                if (instance.target().kind() == AnnotationTarget.Kind.CLASS) {
                    ret.add(instance.target().asClass().name());
                }
            }
        }
        Set<DotName> processed = new HashSet<>();
        processed.addAll(ret);
        for (ClassInfo clazz : index.getKnownClasses()) {
            for (DotName annotation : clazz.annotationsMap().keySet()) {
                if (processed.contains(annotation)) {
                    continue;
                }
                processed.add(annotation);
                try {
                    Class<?> loadedAnnotation = Thread.currentThread().getContextClassLoader().loadClass(annotation.toString());
                    if (loadedAnnotation.isAnnotationPresent(Testable.class)) {
                        ret.add(annotation);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Unable to load annotation type " + annotation + " cannot determine if it is @Testable");
                }
            }
        }
        return ret;
    }

    public TestState getResults() {
        return testState;
    }

    public boolean isRunning() {
        return testsRunning;
    }

    static class Builder {
        private DevModeContext.ModuleInfo moduleInfo;
        private TestType testType = TestType.ALL;
        private TestState testState;
        private long runId = -1;
        private CuratedApplication testApplication;
        private ClassScanResult classScanResult;
        private TestClassUsages testClassUsages;
        private final List<TestRunListener> listeners = new ArrayList<>();
        private final List<PostDiscoveryFilter> additionalFilters = new ArrayList<>();
        private List<String> includeTags = Collections.emptyList();
        private List<String> excludeTags = Collections.emptyList();
        private Pattern include;
        private Pattern exclude;
        private String specificSelection;
        private List<String> includeEngines = Collections.emptyList();
        private List<String> excludeEngines = Collections.emptyList();
        private boolean failingTestsOnly;

        public Builder setRunId(long runId) {
            this.runId = runId;
            return this;
        }

        public Builder setModuleInfo(DevModeContext.ModuleInfo moduleInfo) {
            this.moduleInfo = moduleInfo;
            return this;
        }

        public Builder setTestType(TestType testType) {
            this.testType = testType;
            return this;
        }

        // TODO we now ignore what gets set here and make our own, how to handle that?
        public Builder setTestApplication(CuratedApplication testApplication) {
            this.testApplication = testApplication;
            return this;
        }

        public Builder setClassScanResult(ClassScanResult classScanResult) {
            this.classScanResult = classScanResult;
            return this;
        }

        public Builder setIncludeTags(List<String> includeTags) {
            this.includeTags = includeTags;
            return this;
        }

        public Builder setExcludeTags(List<String> excludeTags) {
            this.excludeTags = excludeTags;
            return this;
        }

        public Builder setTestClassUsages(TestClassUsages testClassUsages) {
            this.testClassUsages = testClassUsages;
            return this;
        }

        public Builder addListener(TestRunListener listener) {
            this.listeners.add(listener);
            return this;
        }

        public Builder addAdditionalFilter(PostDiscoveryFilter filter) {
            this.additionalFilters.add(filter);
            return this;
        }

        public Builder setTestState(TestState testState) {
            this.testState = testState;
            return this;
        }

        public Builder setInclude(Pattern include) {
            this.include = include;
            return this;
        }

        public Builder setExclude(Pattern exclude) {
            this.exclude = exclude;
            return this;
        }

        public Builder setSpecificSelection(String specificSelection) {
            this.specificSelection = specificSelection;
            return this;
        }

        public Builder setIncludeEngines(List<String> includeEngines) {
            this.includeEngines = includeEngines;
            return this;
        }

        public Builder setExcludeEngines(List<String> excludeEngines) {
            this.excludeEngines = excludeEngines;
            return this;
        }

        public JunitTestRunner build() {
            Objects.requireNonNull(testClassUsages, "testClassUsages");
            Objects.requireNonNull(testApplication, "testApplication");
            Objects.requireNonNull(testState, "testState");
            return new JunitTestRunner(this);
        }

        public Builder setFailingTestsOnly(boolean failingTestsOnly) {
            this.failingTestsOnly = failingTestsOnly;
            return this;
        }
    }

    private static class RegexFilter implements PostDiscoveryFilter {

        final boolean exclude;
        final Pattern pattern;

        private RegexFilter(boolean exclude, Pattern pattern) {
            this.exclude = exclude;
            this.pattern = pattern;
        }

        @Override
        public FilterResult apply(TestDescriptor testDescriptor) {
            if (testDescriptor.getSource().isPresent()) {
                if (testDescriptor.getSource().get() instanceof MethodSource methodSource) {
                    String name = methodSource.getClassName();
                    if (pattern.matcher(name).matches()) {
                        return FilterResult.includedIf(!exclude);
                    }
                    return FilterResult.includedIf(exclude);
                }
            }
            return FilterResult.included("not a method");
        }
    }

    // https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html#test
    // org.apache.maven.surefire.api.testset.TestListResolver
    // org.apache.maven.surefire.api.testset.ResolvedTest
    private static class MavenSpecificSelectionFilter implements PostDiscoveryFilter {
        private final Matcher[] excludes;
        private final Matcher[] includes;

        MavenSpecificSelectionFilter(String selection) {
            List<Matcher> excludes = new ArrayList<>();
            List<Matcher> includes = new ArrayList<>();

            if (selection != null) {
                for (String item : selection.split(",")) {
                    item = item.trim();
                    if (item.isEmpty() || "!".equals(item) || "#".equals(item)) {
                        continue;
                    }
                    List<Matcher> list;
                    if (item.startsWith("!")) {
                        list = excludes;
                        item = item.substring(1);
                    } else {
                        list = includes;
                    }

                    int hashIndex = item.indexOf('#');
                    if (hashIndex == 0) {
                        List<Pattern> methods = extractMethodPatterns(item.substring(hashIndex + 1));
                        list.add(new MethodMatcher(methods.toArray(new Pattern[0])));
                    } else if (hashIndex > 0) {
                        String classPattern = adjustClassGlob(item.substring(0, hashIndex));
                        if (hashIndex == item.length() - 1) {
                            list.add(new ClassMatcher(globToPattern(classPattern)));
                        } else {
                            List<Pattern> methods = extractMethodPatterns(item.substring(hashIndex + 1));
                            list.add(new ClassAndMethodMatcher(globToPattern(classPattern), methods.toArray(new Pattern[0])));
                        }
                    } else {
                        String classPattern = adjustClassGlob(item);
                        list.add(new ClassMatcher(globToPattern(classPattern)));
                    }
                }
            }

            this.excludes = excludes.toArray(new Matcher[0]);
            this.includes = includes.toArray(new Matcher[0]);
        }

        private static List<Pattern> extractMethodPatterns(String methodGlobs) {
            List<Pattern> result = new ArrayList<>();
            for (String methodGlob : methodGlobs.split("\\+")) {
                methodGlob = methodGlob.trim();
                if (!methodGlob.isEmpty()) {
                    result.add(globToPattern(methodGlob));
                }
            }
            return result;
        }

        private static String adjustClassGlob(String classGlob) {
            if (classGlob.startsWith("**/")) {
                classGlob = classGlob.substring("**/".length());
            }
            if (classGlob.endsWith(".java")) {
                classGlob = classGlob.substring(0, classGlob.length() - ".java".length());
            } else if (classGlob.endsWith(".class")) {
                classGlob = classGlob.substring(0, classGlob.length() - ".class".length());
            } else if (classGlob.endsWith(".*")) {
                classGlob = classGlob.substring(0, classGlob.length() - ".*".length());
            }
            return "**/" + classGlob.replace('.', '/');
        }

        private static Pattern globToPattern(String glob) {
            return Pattern.compile(GlobUtil.toRegexPattern(glob));
        }

        @Override
        public FilterResult apply(TestDescriptor testDescriptor) {
            if (testDescriptor.getSource().isPresent()
                    && testDescriptor.getSource().get() instanceof MethodSource methodSource) {
                String className = methodSource.getClassName().replace('.', '/');
                String methodName = methodSource.getMethodName();
                for (Matcher exclude : excludes) {
                    if (exclude.matches(className, methodName)) {
                        return FilterResult.excluded(null);
                    }
                }
                for (Matcher include : includes) {
                    if (include.matches(className, methodName)) {
                        return FilterResult.included(null);
                    }
                }
                return FilterResult.excluded(null);
            }
            return FilterResult.included("not a method");
        }

        private interface Matcher {
            boolean matches(String className, String methodName);
        }

        private record ClassMatcher(Pattern classPattern) implements Matcher {
            @Override
            public boolean matches(String className, String methodName) {
                return classPattern.matcher(className).matches();
            }
        }

        private record MethodMatcher(Pattern[] methodPatterns) implements Matcher {
            @Override
            public boolean matches(String className, String methodName) {
                for (Pattern methodPattern : methodPatterns) {
                    if (methodPattern.matcher(methodName).matches()) {
                        return true;
                    }
                }
                return false;
            }
        }

        private record ClassAndMethodMatcher(Pattern classPattern, Pattern[] methodPatterns) implements Matcher {
            @Override
            public boolean matches(String className, String methodName) {
                if (classPattern.matcher(className).matches()) {
                    for (Pattern methodPattern : methodPatterns) {
                        if (methodPattern.matcher(methodName).matches()) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
    }

    // https://docs.gradle.org/current/userguide/java_testing.html#test_filtering
    // org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher
    // org.gradle.api.internal.tasks.testing.filter.TestSelectionMatcher.TestPattern
    private static class GradleSpecificSelectionFilter implements PostDiscoveryFilter {
        // these 2 arrays always have the same length
        private final Pattern[] includes;
        private final boolean[] simpleNames;

        GradleSpecificSelectionFilter(String selection) {
            List<Pattern> includes = new ArrayList<>();
            List<Boolean> simpleNames = new ArrayList<>();

            if (selection != null) {
                for (String item : selection.split(",")) {
                    item = item.trim();
                    if (item.isEmpty()) {
                        continue;
                    }

                    includes.add(parsePattern(item));
                    simpleNames.add(Character.isUpperCase(item.charAt(0)));
                }
            }

            this.includes = includes.toArray(new Pattern[0]);
            this.simpleNames = new boolean[simpleNames.size()];
            for (int i = 0; i < simpleNames.size(); i++) {
                this.simpleNames[i] = simpleNames.get(i);
            }
        }

        private static Pattern parsePattern(String item) {
            StringBuilder result = new StringBuilder();
            int start = 0;
            int current = 0;
            while (current < item.length()) {
                if (item.charAt(current) == '*') {
                    if (current > start) {
                        String part = item.substring(start, current);
                        result.append(Pattern.quote(part));
                    }
                    result.append(".*");
                    start = current + 1;
                }
                current++;
            }
            if (current > start) {
                String part = item.substring(start, current);
                result.append(Pattern.quote(part));
            }
            return Pattern.compile(result.toString());
        }

        @Override
        public FilterResult apply(TestDescriptor testDescriptor) {
            if (testDescriptor.getSource().isPresent()
                    && testDescriptor.getSource().get() instanceof MethodSource methodSource) {
                String className = methodSource.getClassName();
                String methodName = methodSource.getMethodName();
                String classAndMethodName = className + "." + methodName;

                String simpleClassName = className;
                String simpleClassAndMethodName = classAndMethodName;

                // using simple names is common, so let's just precompute that unconditionally
                int lastDot = className.lastIndexOf('.');
                if (lastDot >= 0 && lastDot < className.length() - 1) {
                    simpleClassName = className.substring(lastDot + 1);
                    simpleClassAndMethodName = simpleClassName + "." + methodName;
                }

                for (int i = 0; i < includes.length; i++) {
                    String testedClassName = className;
                    String testedClassAndMethodName = classAndMethodName;
                    if (simpleNames[i]) {
                        testedClassName = simpleClassName;
                        testedClassAndMethodName = simpleClassAndMethodName;
                    }

                    Pattern include = includes[i];
                    if (include.matcher(testedClassAndMethodName).matches() || include.matcher(testedClassName).matches()) {
                        return FilterResult.included(null);
                    }
                }
                return FilterResult.excluded(null);
            }
            return FilterResult.included("not a method");
        }
    }

    /**
     * filter for tests that are currently failing.
     * <p>
     * Note that this also includes newly written tests, as we don't know if they
     * will fail or not yet.
     */
    private class CurrentlyFailingFilter implements PostDiscoveryFilter {

        @Override
        public FilterResult apply(TestDescriptor testDescriptor) {
            if (testDescriptor.getSource().isPresent()) {
                if (testDescriptor.getSource().get() instanceof MethodSource methodSource) {
                    String name = methodSource.getClassName();
                    Map<UniqueId, TestResult> results = testState.getCurrentResults().get(name);
                    if (results == null) {
                        return FilterResult.included("new test");
                    }
                    TestResult testResult = results.get(testDescriptor.getUniqueId());
                    if (testResult == null) {
                        return FilterResult.included("new test");
                    }
                    return FilterResult
                            .includedIf(testResult.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED);
                }
            }
            return FilterResult.included("not a method");
        }

    }

    static class DiscoveryResult implements AutoCloseable {

        private final static DiscoveryResult EMPTY = new DiscoveryResult(null, List.of());

        final QuarkusClassLoader classLoader;
        final List<Class<?>> testClasses;

        DiscoveryResult(QuarkusClassLoader classLoader, List<Class<?>> testClasses) {
            this.classLoader = classLoader;
            this.testClasses = testClasses;
        }

        @Override
        public void close() throws Exception {
            if (classLoader != null) {
                classLoader.close();
            }
        }
    }

}
