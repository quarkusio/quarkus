package io.quarkus.deployment.dev.testing;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.junit.platform.launcher.TestIdentifier;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.QuarkusBootstrap.Mode;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.CompilationProvider;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeContext.ModuleInfo;
import io.quarkus.deployment.dev.QuarkusCompiler;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.testing.TestWatchedFiles;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathList;
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
    volatile List<String> includeEngines = Collections.emptyList();
    volatile List<String> excludeEngines = Collections.emptyList();
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
    String appPropertiesIncludeEngines;
    String appPropertiesExcludeEngines;
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

    private static Pattern getCompiledPatternOrNull(Optional<String> patternStr) {
        return patternStr.isPresent() ? Pattern.compile(patternStr.get()) : null;
    }

    public void init() {
        if (moduleRunners.isEmpty()) {
            TestWatchedFiles.setWatchedFilesListener(
                    (paths, predicates) -> RuntimeUpdatesProcessor.INSTANCE.setWatchedFilePaths(paths, predicates, true));
            final Pattern includeModulePattern = getCompiledPatternOrNull(config.includeModulePattern);
            final Pattern excludeModulePattern = getCompiledPatternOrNull(config.excludeModulePattern);
            for (var module : context.getAllModules()) {
                final boolean mainModule = module == context.getApplicationRoot();
                if (config.onlyTestApplicationModule && !mainModule) {
                    continue;
                } else if (includeModulePattern != null) {
                    if (!includeModulePattern
                            .matcher(module.getArtifactKey().getGroupId() + ":" + module.getArtifactKey().getArtifactId())
                            .matches()) {
                        continue;
                    }
                } else if (excludeModulePattern != null) {
                    if (excludeModulePattern
                            .matcher(module.getArtifactKey().getGroupId() + ":" + module.getArtifactKey().getArtifactId())
                            .matches()) {
                        continue;
                    }
                }

                try {
                    final Path projectDir = Path.of(module.getProjectDirectory());
                    final QuarkusBootstrap.Builder bootstrapConfig = curatedApplication.getQuarkusBootstrap().clonedBuilder()
                            .setMode(QuarkusBootstrap.Mode.TEST)
                            .setAssertionsEnabled(true)
                            .setDisableClasspathCache(false)
                            .setIsolateDeployment(true)
                            .setExistingModel(null)
                            .setBaseClassLoader(getClass().getClassLoader().getParent())
                            .setTest(true)
                            .setAuxiliaryApplication(true)
                            .setHostApplicationIsTestOnly(devModeType == DevModeType.TEST_ONLY)
                            .setProjectRoot(projectDir)
                            .setApplicationRoot(getRootPaths(module, mainModule))
                            .clearLocalArtifacts();

                    final QuarkusClassLoader ctParentFirstCl;
                    final Mode currentMode = curatedApplication.getQuarkusBootstrap().getMode();
                    // in case of quarkus:test the application model will already include test dependencies
                    if (Mode.CONTINUOUS_TEST != currentMode && Mode.TEST != currentMode) {
                        // In this case the current application model does not include test dependencies.
                        // 1) we resolve an application model for test mode;
                        // 2) we create a new CT base classloader that includes parent-first test scoped dependencies
                        // so that they are not loaded by augment and base runtime classloaders.
                        var appModelFactory = curatedApplication.getQuarkusBootstrap().newAppModelFactory();
                        appModelFactory.setBootstrapAppModelResolver(null);
                        appModelFactory.setTest(true);
                        appModelFactory.setLocalArtifacts(Set.of());
                        if (!mainModule) {
                            appModelFactory.setAppArtifact(null);
                            appModelFactory.setProjectRoot(projectDir);
                        }
                        final ApplicationModel testModel = appModelFactory.resolveAppModel().getApplicationModel();
                        bootstrapConfig.setExistingModel(testModel);

                        QuarkusClassLoader.Builder clBuilder = null;
                        var currentParentFirst = curatedApplication.getApplicationModel().getParentFirst();
                        for (ResolvedDependency d : testModel.getDependencies()) {
                            if (d.isClassLoaderParentFirst() && !currentParentFirst.contains(d.getKey())) {
                                if (clBuilder == null) {
                                    clBuilder = QuarkusClassLoader.builder("Continuous Testing Parent-First",
                                            getClass().getClassLoader().getParent(), false);
                                }
                                clBuilder.addElement(ClassPathElement.fromDependency(d));
                            }
                        }

                        ctParentFirstCl = clBuilder == null ? null : clBuilder.build();
                        if (ctParentFirstCl != null) {
                            bootstrapConfig.setBaseClassLoader(ctParentFirstCl);
                        }
                    } else {
                        ctParentFirstCl = null;
                        if (mainModule) {
                            // the model and the app classloader already include test scoped dependencies
                            bootstrapConfig.setExistingModel(curatedApplication.getApplicationModel());
                        }
                    }

                    //we always want to propagate parent first
                    //so it is consistent. Some modules may not have quarkus dependencies
                    //so they won't load junit parent first without this
                    for (var i : curatedApplication.getApplicationModel().getDependencies()) {
                        if (i.isClassLoaderParentFirst()) {
                            bootstrapConfig.addParentFirstArtifact(i.getKey());
                        }
                    }
                    var testCuratedApplication = bootstrapConfig.build().bootstrap();
                    if (mainModule) {
                        //horrible hack
                        //we really need a compiler per module but we are not setup for this yet
                        //if a module has test scoped dependencies that are not in the application then
                        //compilation can fail
                        //note that we already have a similar issue with provided scoped deps, and so far nobody
                        //has complained much
                        compiler = new QuarkusCompiler(testCuratedApplication, compilationProviders, context);
                    }
                    var testRunner = new ModuleTestRunner(this, testCuratedApplication, module);
                    QuarkusClassLoader cl = (QuarkusClassLoader) getClass().getClassLoader();
                    cl.addCloseTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                close();
                            } finally {
                                testCuratedApplication.close();
                                if (ctParentFirstCl != null) {
                                    ctParentFirstCl.close();
                                }
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

    private PathList getRootPaths(ModuleInfo module, final boolean mainModule) {
        final PathList.Builder pathBuilder = PathList.builder();
        final Consumer<Path> paths = new Consumer<>() {
            @Override
            public void accept(Path t) {
                if (!pathBuilder.contains(t)) {
                    if (!Files.exists(t)) {
                        try {
                            Files.createDirectories(t);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    pathBuilder.add(t);
                }
            }
        };
        module.getTest().ifPresent(test -> {
            paths.accept(Path.of(test.getClassesPath()));
            if (test.getResourcesOutputPath() != null) {
                paths.accept(Path.of(test.getResourcesOutputPath()));
            }
        });
        if (mainModule) {
            curatedApplication.getQuarkusBootstrap().getApplicationRoot().forEach(paths::accept);
        } else {
            paths.accept(Path.of(module.getMain().getClassesPath()));
        }
        return pathBuilder.build();
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

            }));
        }
        for (var i : testRunListeners) {
            i.runStarted(testCount.get());
        }
        for (var i : runnables) {
            try {
                i.run();
            } catch (Exception e) {
                log.error("Failed to run test module", e);
            }
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
     * We also can't apply this as part of the test startup, as it is too
     * late and the filters have already been resolved.
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
                String includeEngines = p.getProperty("quarkus.test.include-engines");
                String excludeEngines = p.getProperty("quarkus.test.exclude-engines");
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
                    if (!Objects.equals(includeEngines, appPropertiesIncludeEngines)) {
                        if (includeEngines == null) {
                            this.includeEngines = Collections.emptyList();
                        } else {
                            this.includeEngines = Arrays.stream(includeEngines.split(",")).map(String::trim)
                                    .collect(Collectors.toList());
                        }
                    }
                    if (!Objects.equals(excludeEngines, appPropertiesExcludeEngines)) {
                        if (excludeEngines == null) {
                            this.excludeEngines = Collections.emptyList();
                        } else {
                            this.excludeEngines = Arrays.stream(excludeEngines.split(",")).map(String::trim)
                                    .collect(Collectors.toList());
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
                appPropertiesIncludeEngines = includeEngines;
                appPropertiesExcludeEngines = excludeEngines;
                appPropertiesTestType = testType;
                break;
            }
        }
    }

    public boolean isStarted() {
        return started;
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

    public void setEngines(List<String> includeEngines, List<String> excludeEngines) {
        this.includeEngines = includeEngines;
        this.excludeEngines = excludeEngines;
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
