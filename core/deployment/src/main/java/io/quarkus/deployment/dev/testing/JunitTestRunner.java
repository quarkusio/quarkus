package io.quarkus.deployment.dev.testing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.commons.annotation.Testable;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.testing.TracingHandler;

/**
 * This class is responsible for running a single run of JUnit tests.
 */
public class JunitTestRunner {

    private static final Logger log = Logger.getLogger(JunitTestRunner.class);
    public static final DotName QUARKUS_TEST = DotName.createSimple("io.quarkus.test.junit.QuarkusTest");
    public static final DotName QUARKUS_INTEGRATION_TEST = DotName.createSimple("io.quarkus.test.junit.QuarkusIntegrationTest");
    public static final DotName NATIVE_IMAGE_TEST = DotName.createSimple("io.quarkus.test.junit.NativeImageTest");
    public static final DotName TEST_PROFILE = DotName.createSimple("io.quarkus.test.junit.TestProfile");
    public static final DotName TEST = DotName.createSimple(Test.class.getName());
    public static final DotName REPEATED_TEST = DotName.createSimple(RepeatedTest.class.getName());
    public static final DotName PARAMETERIZED_TEST = DotName.createSimple(ParameterizedTest.class.getName());
    public static final DotName TEST_FACTORY = DotName.createSimple(TestFactory.class.getName());
    public static final DotName TEST_TEMPLATE = DotName.createSimple(TestTemplate.class.getName());
    public static final DotName TESTABLE = DotName.createSimple(Testable.class.getName());
    private final long runId;
    private final DevModeContext devModeContext;
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
    private final boolean failingTestsOnly;
    private final TestType testType;

    private volatile boolean testsRunning = false;
    private volatile boolean aborted;
    private volatile boolean paused;

    public JunitTestRunner(Builder builder) {
        this.runId = builder.runId;
        this.devModeContext = builder.devModeContext;
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
        this.failingTestsOnly = builder.failingTestsOnly;
        this.testType = builder.testType;
    }

    public void runTests() {
        long start = System.currentTimeMillis();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try (QuarkusClassLoader tcl = testApplication.createDeploymentClassLoader()) {
            Thread.currentThread().setContextClassLoader(tcl);
            Consumer currentTestAppConsumer = (Consumer) tcl.loadClass(CurrentTestApplication.class.getName()).newInstance();
            currentTestAppConsumer.accept(testApplication);

            Set<UniqueId> allDiscoveredIds = new HashSet<>();
            Set<UniqueId> dynamicIds = new HashSet<>();
            try (DiscoveryResult quarkusTestClasses = discoverTestClasses(devModeContext)) {

                Launcher launcher = LauncherFactory.create(LauncherConfig.builder().build());
                LauncherDiscoveryRequestBuilder launchBuilder = new LauncherDiscoveryRequestBuilder()
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
                    launchBuilder.filters(new TagFilter(false, includeTags));
                } else if (!excludeTags.isEmpty()) {
                    launchBuilder.filters(new TagFilter(true, excludeTags));
                }
                if (include != null) {
                    launchBuilder.filters(new RegexFilter(false, include));
                } else if (exclude != null) {
                    launchBuilder.filters(new RegexFilter(true, exclude));
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
                if (!testPlan.containsTests()) {
                    testState.pruneDeletedTests(allDiscoveredIds, dynamicIds);
                    //nothing to see here
                    for (TestRunListener i : listeners) {
                        i.noTests(new TestRunResults(runId, classScanResult, classScanResult == null, start,
                                System.currentTimeMillis(), toResultsMap(testState.getCurrentResults())));
                    }
                    return;
                }
                long toRun = testPlan.countTestIdentifiers(TestIdentifier::isTest);
                for (TestRunListener listener : listeners) {
                    listener.runStarted(toRun);
                }
                log.debug("Starting test run with " + testPlan.countTestIdentifiers((s) -> true) + " tests");
                TestLogCapturingHandler logHandler = new TestLogCapturingHandler();
                QuarkusConsole.INSTANCE.setOutputFilter(logHandler);

                final Deque<Set<String>> touchedClasses = new LinkedBlockingDeque<>();
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
                launcher.execute(testPlan, new TestExecutionListener() {

                    @Override
                    public void executionStarted(TestIdentifier testIdentifier) {
                        if (aborted) {
                            return;
                        }
                        String className = "";
                        Class<?> clazz = null;
                        if (testIdentifier.getSource().isPresent()) {
                            if (testIdentifier.getSource().get() instanceof MethodSource) {
                                clazz = ((MethodSource) testIdentifier.getSource().get()).getJavaClass();
                            } else if (testIdentifier.getSource().get() instanceof ClassSource) {
                                clazz = ((ClassSource) testIdentifier.getSource().get()).getJavaClass();
                            }
                        }
                        if (clazz != null) {
                            className = clazz.getName();
                            Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
                        }
                        for (TestRunListener listener : listeners) {
                            listener.testStarted(testIdentifier, className);
                        }
                        waitTillResumed();
                        touchedClasses.push(Collections.synchronizedSet(new HashSet<>()));
                    }

                    @Override
                    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
                        waitTillResumed();
                        if (aborted) {
                            return;
                        }
                        Class<?> testClass = null;
                        String displayName = testIdentifier.getDisplayName();
                        TestSource testSource = testIdentifier.getSource().orElse(null);
                        touchedClasses.pop();
                        UniqueId id = UniqueId.parse(testIdentifier.getUniqueId());
                        if (testSource instanceof ClassSource) {
                            testClass = ((ClassSource) testSource).getJavaClass();
                        } else if (testSource instanceof MethodSource) {
                            testClass = ((MethodSource) testSource).getJavaClass();
                            displayName = testClass.getSimpleName() + "#" + displayName;
                        }
                        if (testClass != null) {
                            Map<UniqueId, TestResult> results = resultsByClass.computeIfAbsent(testClass.getName(),
                                    s -> new HashMap<>());
                            TestResult result = new TestResult(displayName, testClass.getName(), id,
                                    TestExecutionResult.aborted(null),
                                    logHandler.captureOutput(), testIdentifier.isTest(), runId);
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
                    }

                    @Override
                    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {

                        if (aborted) {
                            return;
                        }
                        Class<?> testClass = null;
                        String displayName = testIdentifier.getDisplayName();
                        TestSource testSource = testIdentifier.getSource().orElse(null);
                        Set<String> touched = touchedClasses.pop();
                        UniqueId id = UniqueId.parse(testIdentifier.getUniqueId());
                        if (testSource instanceof ClassSource) {
                            testClass = ((ClassSource) testSource).getJavaClass();
                            if (testExecutionResult.getStatus() != TestExecutionResult.Status.ABORTED) {
                                for (Set<String> i : touchedClasses) {
                                    //also add the parent touched classes
                                    touched.addAll(i);
                                }
                                if (startupClasses.get() != null) {
                                    touched.addAll(startupClasses.get());
                                }
                                testClassUsages.updateTestData(testClass.getName(), touched);
                            }
                        } else if (testSource instanceof MethodSource) {
                            testClass = ((MethodSource) testSource).getJavaClass();
                            displayName = testClass.getSimpleName() + "#" + displayName;

                            if (testExecutionResult.getStatus() != TestExecutionResult.Status.ABORTED) {
                                for (Set<String> i : touchedClasses) {
                                    //also add the parent touched classes
                                    touched.addAll(i);
                                }
                                if (startupClasses.get() != null) {
                                    touched.addAll(startupClasses.get());
                                }
                                testClassUsages.updateTestData(testClass.getName(), id,
                                        touched);
                            }
                        }
                        if (testClass != null) {
                            Map<UniqueId, TestResult> results = resultsByClass.computeIfAbsent(testClass.getName(),
                                    s -> new HashMap<>());
                            TestResult result = new TestResult(displayName, testClass.getName(), id, testExecutionResult,
                                    logHandler.captureOutput(), testIdentifier.isTest(), runId);
                            results.put(id, result);
                            if (result.isTest()) {
                                for (TestRunListener listener : listeners) {
                                    listener.testComplete(result);
                                }
                            }
                        }
                        if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                            Throwable throwable = testExecutionResult.getThrowable().get();
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

                QuarkusConsole.INSTANCE.setOutputFilter(null);

                for (TestRunListener listener : listeners) {
                    listener.runComplete(new TestRunResults(runId, classScanResult, classScanResult == null, start,
                            System.currentTimeMillis(), toResultsMap(testState.getCurrentResults())));
                }
            } finally {
                currentTestAppConsumer.accept(null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            TracingHandler.setTracingHandler(null);
            QuarkusConsole.INSTANCE.setOutputFilter(null);
            Thread.currentThread().setContextClassLoader(old);
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
        notifyAll();
    }

    public synchronized void pause() {
        //todo
        paused = true;
    }

    public synchronized void resume() {
        paused = false;
        notifyAll();
    }

    private Map<String, TestClassResult> toResultsMap(
            Map<String, Map<UniqueId, TestResult>> resultsByClass) {
        Map<String, TestClassResult> resultMap = new HashMap<>();
        Set<String> classes = new HashSet<>(resultsByClass.keySet());
        for (String clazz : classes) {
            List<TestResult> passing = new ArrayList<>();
            List<TestResult> failing = new ArrayList<>();
            List<TestResult> skipped = new ArrayList<>();
            for (TestResult i : Optional.ofNullable(resultsByClass.get(clazz)).orElse(Collections.emptyMap()).values()) {
                if (i.getTestExecutionResult().getStatus() == TestExecutionResult.Status.FAILED) {
                    failing.add(i);
                } else if (i.getTestExecutionResult().getStatus() == TestExecutionResult.Status.ABORTED) {
                    skipped.add(i);
                } else {
                    passing.add(i);
                }
            }
            resultMap.put(clazz, new TestClassResult(clazz, passing, failing, skipped));
        }
        return resultMap;
    }

    public void waitTillResumed() {
        synchronized (JunitTestRunner.this) {
            while (paused && !aborted) {
                try {
                    JunitTestRunner.this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (aborted) {
                throw new TestAbortedException("Tests are disabled");
            }
        }
    }

    private DiscoveryResult discoverTestClasses(DevModeContext devModeContext) {
        //maven has a lot of rules around this and is configurable
        //for now this is out of scope, we are just going to do annotation based discovery
        //we will need to fix this sooner rather than later though

        //we also only run tests from the current module, which we can also revisit later
        Indexer indexer = new Indexer();
        try (Stream<Path> files = Files.walk(Paths.get(devModeContext.getApplicationRoot().getTest().get().getClassesPath()))) {
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
        for (DotName intAnno : Arrays.asList(QUARKUS_INTEGRATION_TEST, NATIVE_IMAGE_TEST)) {
            for (AnnotationInstance i : index.getAnnotations(intAnno)) {
                DotName name = i.target().asClass().name();
                integrationTestClasses.add(name.toString());
                for (ClassInfo clazz : index.getAllKnownSubclasses(name)) {
                    integrationTestClasses.add(clazz.name().toString());
                }
            }
        }
        Set<String> quarkusTestClasses = new HashSet<>();
        for (AnnotationInstance i : index.getAnnotations(QUARKUS_TEST)) {
            DotName name = i.target().asClass().name();
            quarkusTestClasses.add(name.toString());
            for (ClassInfo clazz : index.getAllKnownSubclasses(name)) {
                if (!integrationTestClasses.contains(clazz.name().toString())) {
                    quarkusTestClasses.add(clazz.name().toString());
                }
            }
        }
        Set<DotName> allTestAnnotations = collectTestAnnotations(index);
        Set<DotName> allTestClasses = new HashSet<>();
        for (DotName annotation : allTestAnnotations) {
            for (AnnotationInstance instance : index.getAnnotations(annotation)) {
                if (instance.target().kind() == AnnotationTarget.Kind.METHOD) {
                    allTestClasses.add(instance.target().asMethod().declaringClass().name());
                }
            }
        }
        //now we have all the classes with @Test
        //figure out which ones we want to actually run
        Set<String> unitTestClasses = new HashSet<>();
        for (DotName testClass : allTestClasses) {
            String name = testClass.toString();
            if (integrationTestClasses.contains(name) || quarkusTestClasses.contains(name)) {
                continue;
            }
            ClassInfo clazz = index.getClassByName(testClass);
            if (Modifier.isAbstract(clazz.flags())) {
                continue;
            }
            unitTestClasses.add(name);
        }

        List<Class<?>> itClasses = new ArrayList<>();
        List<Class<?>> utClasses = new ArrayList<>();
        for (String i : quarkusTestClasses) {
            try {
                itClasses.add(Thread.currentThread().getContextClassLoader().loadClass(i));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        itClasses.sort(Comparator.comparing(new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> aClass) {
                ClassInfo def = index.getClassByName(DotName.createSimple(aClass.getName()));
                AnnotationInstance testProfile = def.classAnnotation(TEST_PROFILE);
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
            QuarkusClassLoader deploymentClassLoader = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            Set<String> classesToTransform = new HashSet<>(deploymentClassLoader.getLocalClassNames());
            Map<String, byte[]> transformedClasses = new HashMap<>();
            for (String i : classesToTransform) {
                try {
                    byte[] classData = IoUtil
                            .readBytes(deploymentClassLoader.getResourceAsStream(i.replace(".", "/") + ".class"));
                    ClassReader cr = new ClassReader(classData);
                    ClassWriter writer = new QuarkusClassWriter(cr,
                            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    cr.accept(new TestTracingProcessor.TracingClassVisitor(writer, i), 0);
                    transformedClasses.put(i.replace(".", "/") + ".class", writer.toByteArray());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
            cl = testApplication.createRuntimeClassLoader(testApplication.getAugmentClassLoader(), Collections.emptyMap(),
                    transformedClasses);
            for (String i : unitTestClasses) {
                try {
                    utClasses.add(cl.loadClass(i));
                } catch (ClassNotFoundException exception) {
                    throw new RuntimeException(exception);
                }
            }

        }
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
        return ret;
    }

    public TestState getResults() {
        return testState;
    }

    public boolean isRunning() {
        return testsRunning;
    }

    private class TestLogCapturingHandler implements Predicate<String> {

        private final List<String> logOutput;

        public TestLogCapturingHandler() {
            this.logOutput = new ArrayList<>();
        }

        public List<String> captureOutput() {
            List<String> ret = new ArrayList<>(logOutput);
            logOutput.clear();
            return ret;
        }

        @Override
        public boolean test(String logRecord) {
            Thread thread = Thread.currentThread();
            ClassLoader cl = thread.getContextClassLoader();
            while (cl.getParent() != null) {
                if (cl == testApplication.getAugmentClassLoader()
                        || cl == testApplication.getBaseRuntimeClassLoader()) {
                    //TODO: for convenience we save the log records as HTML rather than ANSI here
                    synchronized (logOutput) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        HtmlAnsiOutputStream outputStream = new HtmlAnsiOutputStream(out) {
                        };
                        try {
                            outputStream.write(logRecord.getBytes(StandardCharsets.UTF_8));
                            logOutput.add(new String(out.toByteArray(), StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            log.error("Failed to capture log record", e);
                            logOutput.add(logRecord);
                        }
                    }
                    return TestSupport.instance().get().isDisplayTestOutput();
                }
                cl = cl.getParent();
            }
            return true;
        }
    }

    static class Builder {
        private TestType testType = TestType.ALL;
        private TestState testState;
        private long runId = -1;
        private DevModeContext devModeContext;
        private CuratedApplication testApplication;
        private ClassScanResult classScanResult;
        private TestClassUsages testClassUsages;
        private final List<TestRunListener> listeners = new ArrayList<>();
        private final List<PostDiscoveryFilter> additionalFilters = new ArrayList<>();
        private List<String> includeTags = Collections.emptyList();
        private List<String> excludeTags = Collections.emptyList();
        private Pattern include;
        private Pattern exclude;
        private boolean failingTestsOnly;

        public Builder setRunId(long runId) {
            this.runId = runId;
            return this;
        }

        public Builder setTestType(TestType testType) {
            this.testType = testType;
            return this;
        }

        public Builder setDevModeContext(DevModeContext devModeContext) {
            this.devModeContext = devModeContext;
            return this;
        }

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

        public JunitTestRunner build() {
            Objects.requireNonNull(devModeContext, "devModeContext");
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

    private static class TagFilter implements PostDiscoveryFilter {

        final boolean exclude;
        final Set<String> tags;

        private TagFilter(boolean exclude, Set<String> tags) {
            this.exclude = exclude;
            this.tags = tags;
        }

        @Override
        public FilterResult apply(TestDescriptor testDescriptor) {
            if (testDescriptor.getSource().isPresent()) {
                if (testDescriptor.getSource().get() instanceof MethodSource) {
                    MethodSource methodSource = (MethodSource) testDescriptor.getSource().get();
                    Method m = methodSource.getJavaMethod();
                    FilterResult res = filterTags(m);
                    if (res != null) {
                        return res;
                    }
                    res = filterTags(methodSource.getJavaClass());
                    if (res != null) {
                        return res;
                    }
                    return FilterResult.includedIf(exclude);
                }
            }
            return FilterResult.included("not a method");
        }

        public FilterResult filterTags(AnnotatedElement clz) {
            Tag tag = clz.getAnnotation(Tag.class);
            Tags tagsAnn = clz.getAnnotation(Tags.class);
            List<Tag> all = null;
            if (tag != null) {
                all = Collections.singletonList(tag);
            } else if (tagsAnn != null) {
                all = Arrays.asList(tagsAnn.value());
            } else {
                return null;
            }
            for (Tag i : all) {
                if (tags.contains(i.value())) {
                    return FilterResult.includedIf(!exclude);
                }
            }
            return FilterResult.includedIf(exclude);
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
                if (testDescriptor.getSource().get() instanceof MethodSource) {
                    MethodSource methodSource = (MethodSource) testDescriptor.getSource().get();
                    String name = methodSource.getJavaClass().getName();
                    if (pattern.matcher(name).matches()) {
                        return FilterResult.includedIf(!exclude);
                    }
                    return FilterResult.includedIf(exclude);
                }
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
                if (testDescriptor.getSource().get() instanceof MethodSource) {
                    MethodSource methodSource = (MethodSource) testDescriptor.getSource().get();

                    String name = methodSource.getJavaClass().getName();
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
