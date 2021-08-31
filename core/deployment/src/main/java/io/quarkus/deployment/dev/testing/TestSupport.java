package io.quarkus.deployment.dev.testing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.junit.platform.launcher.TestIdentifier;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.CompilationProvider;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.QuarkusCompiler;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.testing.TestWatchedFiles;
import io.quarkus.runtime.configuration.HyphenateEnumConverter;

public class TestSupport implements TestController {

    private static final Logger log = Logger.getLogger("io.quarkus.test");
    private static final AtomicLong COUNTER = new AtomicLong();

    final CuratedApplication curatedApplication;
    final List<CompilationProvider> compilationProviders;
    final DevModeContext context;
    final List<ModuleTestRunner> moduleRunners = new ArrayList<>();
    final List<TestListener> testListeners = new CopyOnWriteArrayList<>();
    final DevModeType devModeType;

    volatile QuarkusCompiler compiler;
    volatile boolean started;
    volatile TestRunResults testRunResults;
    volatile List<String> includeTags = Collections.emptyList();
    volatile List<String> excludeTags = Collections.emptyList();
    volatile Pattern include = null;
    volatile Pattern exclude = null;
    volatile boolean displayTestOutput;
    volatile Boolean explicitDisplayTestOutput;
    volatile boolean brokenOnlyMode;
    volatile TestType testType = TestType.ALL;

    private boolean testsRunning = false;
    private boolean testsQueued = false;
    private ClassScanResult queuedChanges = null;
    private Throwable compileProblem;
    private volatile boolean firstRun = true;

    String appPropertiesIncludeTags;
    String appPropertiesExcludeTags;
    String appPropertiesIncludePattern;
    String appPropertiesExcludePattern;
    String appPropertiesTestType;
    private TestConfig config;
    private volatile boolean closed;

    public TestSupport(CuratedApplication curatedApplication, List<CompilationProvider> compilationProviders,
            DevModeContext context, DevModeType devModeType) {
        this.curatedApplication = curatedApplication;
        this.compilationProviders = compilationProviders;
        this.context = context;
        this.devModeType = devModeType;
    }

    public static Optional<TestSupport> instance() {
        if (RuntimeUpdatesProcessor.INSTANCE == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(RuntimeUpdatesProcessor.INSTANCE.getTestSupport());
    }

    public synchronized boolean isRunning() {
        return testsRunning;
    }

    public List<TestListener> getTestListeners() {
        return testListeners;
    }

    /**
     * returns the current status of the test runner.
     * <p>
     * This is expressed in terms of test run ids, where -1 signifies
     * no result.
     */
    public RunStatus getStatus() {
        long last = -1;
        //get the running test id before the current status
        //otherwise there is a race where they both could be -1 even though it has started
        long runningTestRunId = getRunningTestRunId();
        TestRunResults tr = testRunResults;
        if (tr != null) {
            last = tr.getId();
        }
        return new RunStatus(last, runningTestRunId);
    }

    public void start() {
        if (!started) {
            synchronized (this) {
                if (!started) {
                    try {
                        started = true;
                        init();
                        for (TestListener i : testListeners) {
                            i.testsEnabled();
                        }
                        if (firstRun) {
                            runTests();
                        }
                        firstRun = false;
                    } catch (Exception e) {
                        log.error("Failed to create compiler, runtime compilation will be unavailable", e);
                    }

                }
            }
        }
    }

    public void init() {
        if (moduleRunners.isEmpty()) {
            TestWatchedFiles.setWatchedFilesListener((s) -> RuntimeUpdatesProcessor.INSTANCE.setWatchedFilePaths(s, true));
            for (var module : context.getAllModules()) {
                boolean mainModule = module == context.getApplicationRoot();
                if (config.onlyTestApplicationModule && !mainModule) {
                    continue;
                } else if (config.includeModulePattern.isPresent()) {
                    Pattern p = Pattern.compile(config.includeModulePattern.get());
                    if (!p.matcher(module.getAppArtifactKey().getGroupId() + ":" + module.getAppArtifactKey().getArtifactId())
                            .matches()) {
                        continue;
                    }
                } else if (config.excludeModulePattern.isPresent()) {
                    Pattern p = Pattern.compile(config.excludeModulePattern.get());
                    if (p.matcher(module.getAppArtifactKey().getGroupId() + ":" + module.getAppArtifactKey().getArtifactId())
                            .matches()) {
                        continue;
                    }
                }

                try {
                    Set<Path> paths = new LinkedHashSet<>();
                    paths.add(Paths.get(module.getTest().get().getClassesPath()));
                    if (module.getTest().get().getResourcesOutputPath() != null) {
                        paths.add(Paths.get(module.getTest().get().getResourcesOutputPath()));
                    }
                    if (mainModule) {
                        paths.addAll(curatedApplication.getQuarkusBootstrap().getApplicationRoot().toList());
                    } else {
                        paths.add(Paths.get(module.getMain().getClassesPath()));
                    }
                    for (var i : paths) {
                        if (!Files.exists(i)) {
                            Files.createDirectories(i);
                        }
                    }
                    var testCuratedApplication = curatedApplication.getQuarkusBootstrap().clonedBuilder()
                            .setMode(QuarkusBootstrap.Mode.TEST)
                            .setAssertionsEnabled(true)
                            .setDisableClasspathCache(false)
                            .setIsolateDeployment(true)
                            .setExistingModel(null)
                            .setBaseClassLoader(getClass().getClassLoader().getParent())
                            .setTest(true)
                            .setAuxiliaryApplication(true)
                            .setHostApplicationIsTestOnly(devModeType == DevModeType.TEST_ONLY)
                            .setProjectRoot(Paths.get(module.getProjectDirectory()))
                            .setApplicationRoot(PathsCollection.from(paths))
                            .build()
                            .bootstrap();
                    if (mainModule) {
                        //horrible hack
                        //we really need a compiler per module but we are not setup for this yet
                        //if a module has test scoped dependencies that are not in the application then
                        //compilation can fail
                        //note that we already have a similar issue with provided scoped deps, and so far nobody
                        //has complained much
                        compiler = new QuarkusCompiler(testCuratedApplication, compilationProviders, context);
                    }
                    var testRunner = new ModuleTestRunner(this, context, testCuratedApplication, module);
                    QuarkusClassLoader cl = (QuarkusClassLoader) getClass().getClassLoader();
                    cl.addCloseTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                close();
                            } finally {
                                testCuratedApplication.close();
                            }
                        }
                    });
                    moduleRunners.add(testRunner);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public synchronized void close() {
        closed = true;
        stop();
    }

    public synchronized void stop() {
        if (started) {
            started = false;
            for (TestListener i : testListeners) {
                i.testsDisabled();
            }
        }
        for (var runner : moduleRunners) {
            runner.abort();
        }
    }

    public void runTests() {
        runTests(null);
    }

    public void runFailedTests() {
        runTests(null, true, false);
    }

    public void runTests(ClassScanResult classScanResult) {
        runTests(classScanResult, false, false);
    }

    /**
     * @param classScanResult The changed classes
     * @param reRunFailures If failures should be re-run
     * @param runningQueued If this is running queued up changes, so we expect 'testsRunning' to be true
     */
    private void runTests(ClassScanResult classScanResult, boolean reRunFailures, boolean runningQueued) {
        if (compileProblem != null) {
            return;
        }
        if (!started) {
            return;
        }
        if (reRunFailures && testRunResults == null) {
            return;
        }
        if (reRunFailures && testRunResults.getCurrentFailing().isEmpty()) {
            log.error("Not re-running failed tests, as all tests passed");
            return;
        }
        synchronized (TestSupport.this) {
            if (testsRunning && !runningQueued) {
                if (reRunFailures) {
                    log.error("Not re-running failed tests, as tests are already in progress.");
                    return;
                }
                if (testsQueued) {
                    if (queuedChanges != null) { //if this is null a full run is scheduled
                        this.queuedChanges = ClassScanResult.merge(this.queuedChanges, classScanResult);
                    }
                } else {
                    testsQueued = true;
                    this.queuedChanges = classScanResult;
                }
                return;
            } else {
                testsRunning = true;
            }
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        runInternal(classScanResult, reRunFailures);
                    } finally {
                        boolean run = false;
                        ClassScanResult current = null;
                        synchronized (TestSupport.this) {
                            if (started) {
                                if (testsQueued) {
                                    testsQueued = false;
                                    run = true;
                                } else {
                                    testsRunning = false;
                                }
                                current = queuedChanges;
                                queuedChanges = null;
                            }
                        }
                        if (run) {
                            runTests(current, false, true);
                        }
                    }
                } catch (Throwable t) {
                    log.error("Internal error running tests", t);
                }
            }
        }, "Test runner thread");
        t.setDaemon(true);
        t.start();
    }

    void runInternal(ClassScanResult classScanResult, boolean reRunFailures) {
        final long runId = COUNTER.incrementAndGet();
        handleApplicationPropertiesChange();
        List<Runnable> runnables = new ArrayList<>();
        List<TestRunListener> testRunListeners = new ArrayList<>();
        for (var i : testListeners) {
            i.testRunStarted(testRunListeners::add);
        }
        long start = System.currentTimeMillis();
        final AtomicLong testCount = new AtomicLong();
        List<TestRunResults> allResults = new ArrayList<>();
        for (var module : moduleRunners) {
            runnables.add(module.prepare(classScanResult, reRunFailures, runId, new TestRunListener() {
                @Override
                public void runStarted(long toRun) {
                    testCount.addAndGet(toRun);
                }

                @Override
                public void testComplete(TestResult result) {
                    for (var i : testRunListeners) {
                        i.testComplete(result);
                    }
                }

                @Override
                public void runComplete(TestRunResults results) {
                    allResults.add(results);
                }

                @Override
                public void runAborted() {
                    for (var i : testRunListeners) {
                        i.runAborted();
                    }
                }

                @Override
                public void testStarted(TestIdentifier testIdentifier, String className) {
                    for (var i : testRunListeners) {
                        i.testStarted(testIdentifier, className);
                    }
                }

                @Override
                public void noTests(TestRunResults results) {
                    allResults.add(results);
                    runStarted(0);
                }
            }));
        }
        if (testCount.get() == 0) {
            TestRunResults results = new TestRunResults(runId, classScanResult, classScanResult == null, start,
                    System.currentTimeMillis(), Collections.emptyMap());
            for (var i : testRunListeners) {
                i.noTests(results);
            }
        }
        for (var i : testRunListeners) {
            i.runStarted(testCount.get());
        }
        for (var i : runnables) {
            i.run();
        }
        Map<String, TestClassResult> aggregate = new HashMap<>();
        for (var i : allResults) {
            aggregate.putAll(i.getResults());
        }
        TestRunResults results = new TestRunResults(runId, classScanResult, classScanResult == null, start,
                System.currentTimeMillis(), aggregate);
        testRunResults = results;
        if (!closed) {
            for (var i : testRunListeners) {
                i.runComplete(results);
            }
        }

    }

    public void addListener(TestListener listener) {
        boolean run = false;
        synchronized (this) {
            testListeners.add(listener);
            if (started) {
                run = true;
            }
        }
        listener.listenerRegistered(this);
        if (run) {
            //run outside lock
            listener.testsEnabled();
        }
    }

    /**
     * HUGE HACK
     * <p>
     * config is driven from the outer dev mode startup, if the user modified test
     * related config in application.properties it will cause a re-test, but the
     * values will not be applied until a dev mode restart happens.
     * <p>
     * We also can't apply this as part of the test startup, as by then it is too
     * late, and the filters have already been resloved.
     * <p>
     * We manually check for application.properties changes and apply them.
     */
    private void handleApplicationPropertiesChange() {
        for (Path rootPath : curatedApplication.getQuarkusBootstrap().getApplicationRoot()) {
            Path appProps = rootPath.resolve("application.properties");
            if (Files.exists(appProps)) {
                Properties p = new Properties();
                try (InputStream in = Files.newInputStream(appProps)) {
                    p.load(in);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String includeTags = p.getProperty("quarkus.test.include-tags");
                String excludeTags = p.getProperty("quarkus.test.exclude-tags");
                String includePattern = p.getProperty("quarkus.test.include-pattern");
                String excludePattern = p.getProperty("quarkus.test.exclude-pattern");
                String testType = p.getProperty("quarkus.test.type");
                if (!firstRun) {
                    if (!Objects.equals(includeTags, appPropertiesIncludeTags)) {
                        if (includeTags == null) {
                            this.includeTags = Collections.emptyList();
                        } else {
                            this.includeTags = Arrays.stream(includeTags.split(",")).map(String::trim)
                                    .collect(Collectors.toList());
                        }
                    }
                    if (!Objects.equals(excludeTags, appPropertiesExcludeTags)) {
                        if (excludeTags == null) {
                            this.excludeTags = Collections.emptyList();
                        } else {
                            this.excludeTags = Arrays.stream(excludeTags.split(",")).map(String::trim)
                                    .collect(Collectors.toList());
                        }
                    }
                    if (!Objects.equals(includePattern, appPropertiesIncludePattern)) {
                        if (includePattern == null) {
                            include = null;
                        } else {
                            include = Pattern.compile(includePattern);
                        }
                    }
                    if (!Objects.equals(excludePattern, appPropertiesExcludePattern)) {
                        if (excludePattern == null) {
                            exclude = null;
                        } else {
                            exclude = Pattern.compile(excludePattern);
                        }
                    }
                    if (!Objects.equals(testType, appPropertiesTestType)) {
                        if (testType == null) {
                            this.testType = TestType.ALL;
                        } else {
                            this.testType = new HyphenateEnumConverter<>(TestType.class).convert(testType);
                        }
                    }
                }
                appPropertiesIncludeTags = includeTags;
                appPropertiesExcludeTags = excludeTags;
                appPropertiesIncludePattern = includePattern;
                appPropertiesExcludePattern = excludePattern;
                appPropertiesTestType = testType;
                break;
            }
        }
    }

    public boolean isStarted() {
        return started;
    }

    public CuratedApplication getCuratedApplication() {
        return curatedApplication;
    }

    public QuarkusCompiler getCompiler() {
        return compiler;
    }

    public TestRunResults getTestRunResults() {
        return testRunResults;
    }

    public TestRunResults getResults() {
        return testRunResults;
    }

    public synchronized long getRunningTestRunId() {
        if (testsRunning) {
            return COUNTER.get();
        }
        return -1;
    }

    public void setTags(List<String> includeTags, List<String> excludeTags) {
        this.includeTags = includeTags;
        this.excludeTags = excludeTags;
    }

    public void setPatterns(String include, String exclude) {
        this.include = include == null ? null : Pattern.compile(include);
        this.exclude = exclude == null ? null : Pattern.compile(exclude);
    }

    public TestSupport setConfiguredDisplayTestOutput(boolean displayTestOutput) {
        if (explicitDisplayTestOutput != null) {
            this.displayTestOutput = displayTestOutput;
        }
        this.displayTestOutput = displayTestOutput;
        return this;
    }

    public TestSupport setTestType(TestType testType) {
        this.testType = testType;
        return this;
    }

    @Override
    public TestState currentState() {
        return TestState.merge(moduleRunners.stream().map(ModuleTestRunner::getTestState).collect(Collectors.toList()));
    }

    @Override
    public void runAllTests() {
        runTests();
    }

    @Override
    public void setDisplayTestOutput(boolean displayTestOutput) {
        this.explicitDisplayTestOutput = displayTestOutput;
        this.displayTestOutput = displayTestOutput;
    }

    @Override
    public boolean toggleBrokenOnlyMode() {

        brokenOnlyMode = !brokenOnlyMode;

        if (brokenOnlyMode) {
            log.info("Broken only mode enabled");
        } else {
            log.info("Broken only mode disabled");
        }

        for (TestListener i : testListeners) {
            i.setBrokenOnly(brokenOnlyMode);
        }

        return brokenOnlyMode;
    }

    @Override
    public boolean toggleTestOutput() {

        setDisplayTestOutput(!displayTestOutput);
        if (displayTestOutput) {
            log.info("Test output enabled");
        } else {
            log.info("Test output disabled");
        }

        for (TestListener i : testListeners) {
            i.setTestOutput(displayTestOutput);
        }

        return displayTestOutput;
    }

    @Override
    public boolean toggleInstrumentation() {

        boolean ibr = RuntimeUpdatesProcessor.INSTANCE.toggleInstrumentation();

        for (TestListener i : testListeners) {
            i.setInstrumentationBasedReload(ibr);
        }

        return ibr;
    }

    @Override
    public boolean toggleLiveReloadEnabled() {

        boolean lr = RuntimeUpdatesProcessor.INSTANCE.toggleLiveReloadEnabled();

        for (TestListener i : testListeners) {
            i.setLiveReloadEnabled(lr);
        }

        return lr;
    }

    @Override
    public void printFullResults() {
        if (currentState().getFailingClasses().isEmpty()) {
            log.info("All tests passed, no output to display");
        }
        for (TestClassResult i : currentState().getFailingClasses()) {
            for (TestResult failed : i.getFailing()) {
                log.error(
                        "Test " + failed.getDisplayName() + " failed "
                                + failed.getTestExecutionResult().getStatus()
                                + "\n",
                        failed.getTestExecutionResult().getThrowable().get());
            }
        }
    }

    @Override
    public boolean isBrokenOnlyMode() {
        return brokenOnlyMode;
    }

    @Override
    public boolean isDisplayTestOutput() {
        return displayTestOutput;
    }

    @Override
    public boolean isInstrumentationEnabled() {
        return RuntimeUpdatesProcessor.INSTANCE.instrumentationEnabled();
    }

    @Override
    public boolean isLiveReloadEnabled() {
        return RuntimeUpdatesProcessor.INSTANCE.isLiveReloadEnabled();
    }

    public void testCompileFailed(Throwable e) {
        synchronized (this) {
            compileProblem = e;
        }

        for (TestListener i : testListeners) {
            i.testCompileFailed(e.getMessage());
        }
    }

    public synchronized void testCompileSucceeded() {
        compileProblem = null;
        for (TestListener i : testListeners) {
            i.testCompileSucceeded();
        }
    }

    public void setConfig(TestConfig config) {
        this.config = config;
    }

    public TestConfig getConfig() {
        return config;
    }

    public static class RunStatus {

        final long lastRun;
        final long running;

        public RunStatus(long lastRun, long running) {
            this.lastRun = lastRun;
            this.running = running;
        }

        public long getLastRun() {
            return lastRun;
        }

        public long getRunning() {
            return running;
        }
    }

}
