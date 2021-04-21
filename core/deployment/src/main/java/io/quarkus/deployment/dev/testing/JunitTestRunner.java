package io.quarkus.deployment.dev.testing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
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
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.testing.TracingHandler;

/**
 * This class is responsible for running a single run of JUnit tests.
 */
public class JunitTestRunner {

    private static final Logger log = Logger.getLogger(JunitTestRunner.class);
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
    private final boolean displayInConsole;
    private final boolean failingTestsOnly;

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
        this.displayInConsole = builder.displayInConsole;
        this.failingTestsOnly = builder.failingTestsOnly;
    }

    public void runTests() {
        long start = System.currentTimeMillis();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {

            ClassLoader tcl = testApplication.createDeploymentClassLoader();
            Thread.currentThread().setContextClassLoader(tcl);
            ((Consumer) tcl.loadClass(CurrentTestApplication.class.getName()).newInstance()).accept(testApplication);

            List<Class<?>> quarkusTestClasses = discoverTestClasses(devModeContext);

            Launcher launcher = LauncherFactory.create(LauncherConfig.builder().build());
            LauncherDiscoveryRequestBuilder launchBuilder = new LauncherDiscoveryRequestBuilder()
                    .selectors(quarkusTestClasses.stream().map(DiscoverySelectors::selectClass).collect(Collectors.toList()));
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
                //nothing to see here
                for (TestRunListener i : listeners) {
                    i.noTests();
                }
                return;
            }
            long toRun = testPlan.countTestIdentifiers(TestIdentifier::isTest);
            for (TestRunListener listener : listeners) {
                listener.runStarted(toRun);
            }
            log.debug("Starting test run with " + quarkusTestClasses.size() + " test cases");
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
                    String className = "";
                    if (testIdentifier.getSource().isPresent()) {
                        if (testIdentifier.getSource().get() instanceof MethodSource) {
                            className = ((MethodSource) testIdentifier.getSource().get()).getClassName();
                        } else if (testIdentifier.getSource().get() instanceof ClassSource) {
                            className = ((ClassSource) testIdentifier.getSource().get()).getClassName();
                        }
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
                            StackTraceElement[] st = throwable.getStackTrace();
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
                            throwable.setStackTrace(st);
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
            if (classScanResult != null) {
                testState.classesRemoved(classScanResult.getDeletedClassNames());
            }

            QuarkusConsole.INSTANCE.setOutputFilter(null);
            List<TestResult> historicFailures = testState.getHistoricFailures(resultsByClass);

            for (TestRunListener listener : listeners) {
                listener.runComplete(new TestRunResults(runId, classScanResult, classScanResult == null, start,
                        System.currentTimeMillis(), toResultsMap(historicFailures, resultsByClass)));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
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

    private Map<String, TestClassResult> toResultsMap(List<TestResult> historicFailures,
            Map<String, Map<UniqueId, TestResult>> resultsByClass) {
        Map<String, TestClassResult> resultMap = new HashMap<>();
        Map<String, List<TestResult>> historicMap = new HashMap<>();
        for (TestResult i : historicFailures) {
            historicMap.computeIfAbsent(i.getTestClass(), s -> new ArrayList<>()).add(i);
        }
        Set<String> classes = new HashSet<>(resultsByClass.keySet());
        classes.addAll(historicMap.keySet());
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
            for (TestResult i : Optional.ofNullable(historicMap.get(clazz)).orElse(Collections.emptyList())) {
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

    private static List<Class<?>> discoverTestClasses(DevModeContext devModeContext) {
        //maven has a lot of rules around this and is configurable
        //for now this is out of scope, we are just going to consider all @QuarkusTest classes
        //we can revisit this later

        //simple class loading
        List<URL> classRoots = new ArrayList<>();
        try {
            for (DevModeContext.ModuleInfo i : devModeContext.getAllModules()) {
                classRoots.add(Paths.get(i.getMain().getClassesPath()).toFile().toURL());
            }
            //we know test is not empty, otherwise we would not be runnning
            classRoots.add(Paths.get(devModeContext.getApplicationRoot().getTest().get().getClassesPath()).toFile().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        URLClassLoader ucl = new URLClassLoader(classRoots.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

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

        //todo: sort by profile, account for modules
        Index index = indexer.complete();
        List<Class<?>> ret = new ArrayList<>();
        for (AnnotationInstance i : index.getAnnotations(DotName.createSimple("io.quarkus.test.junit.QuarkusTest"))) {
            try {
                ret.add(ucl.loadClass(i.target().asClass().name().toString()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        ret.sort(Comparator.comparing(new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> aClass) {
                ClassInfo def = index.getClassByName(DotName.createSimple(aClass.getName()));
                AnnotationInstance testProfile = def.classAnnotation(DotName.createSimple("io.quarkus.test.junit.TestProfile"));
                if (testProfile == null) {
                    return "$$" + aClass.getName();
                }
                return testProfile.value().asClass().name().toString() + "$$" + aClass.getName();
            }
        }));
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
                    return displayInConsole;
                }
                cl = cl.getParent();
            }
            return true;
        }
    }

    static class Builder {
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
        private boolean displayInConsole;
        private boolean failingTestsOnly;

        public Builder setRunId(long runId) {
            this.runId = runId;
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

        public Builder setDisplayInConsole(boolean displayInConsole) {
            this.displayInConsole = displayInConsole;
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
     *
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

}
