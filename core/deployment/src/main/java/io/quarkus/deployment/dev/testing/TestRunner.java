package io.quarkus.deployment.dev.testing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.PostDiscoveryFilter;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.dev.testing.TestWatchedFiles;
import io.quarkus.runtime.configuration.HyphenateEnumConverter;

public class TestRunner {

    private static final Logger log = Logger.getLogger("io.quarkus.test");
    private static final AtomicLong COUNTER = new AtomicLong();

    private final TestSupport testSupport;
    private final DevModeContext devModeContext;
    private final CuratedApplication testApplication;

    private boolean testsRunning = false;
    private boolean testsQueued = false;
    private ClassScanResult queuedChanges = null;
    private boolean queuedFailureRun;

    private Throwable compileProblem;

    private final TestClassUsages testClassUsages = new TestClassUsages();
    private boolean paused;
    /**
     * disabled is different to paused, when the runner is disabled we abort all runs rather than pausing them.
     */
    private volatile boolean disabled = true;
    private volatile boolean firstRun = true;
    private JunitTestRunner runner;

    String appPropertiesIncludeTags;
    String appPropertiesExcludeTags;
    String appPropertiesIncludePattern;
    String appPropertiesExcludePattern;
    String appPropertiesTestType;

    public TestRunner(TestSupport testSupport, DevModeContext devModeContext, CuratedApplication testApplication) {
        this.testSupport = testSupport;
        this.devModeContext = devModeContext;
        this.testApplication = testApplication;
    }

    public void runTests() {
        runTests(null);
    }

    public synchronized long getRunningTestRunId() {
        if (testsRunning) {
            return COUNTER.get();
        }
        return -1;
    }

    public void runFailedTests() {
        runTests(null, true);
    }

    public void runTests(ClassScanResult classScanResult) {
        runTests(classScanResult, false);
    }

    private void runTests(ClassScanResult classScanResult, boolean reRunFailures) {
        if (compileProblem != null) {
            return;
        }
        if (disabled) {
            return;
        }
        if (reRunFailures && testSupport.testRunResults == null) {
            return;
        }
        if (reRunFailures && testSupport.testRunResults.getCurrentFailing().isEmpty()) {
            log.error("Not re-running failed tests, as all tests passed");
            return;
        }
        synchronized (TestRunner.this) {
            if (testsRunning) {
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
                Thread.currentThread().setContextClassLoader(testApplication.getAugmentClassLoader());
                try {
                    try {
                        runInternal(classScanResult, reRunFailures);
                    } finally {
                        waitTillResumed();
                        boolean run = false;
                        ClassScanResult current = null;
                        synchronized (TestRunner.this) {
                            if (!disabled) {
                                if (testsQueued) {
                                    testsQueued = false;
                                    run = true;
                                }
                                current = queuedChanges;
                                queuedChanges = null;
                            }
                            testsRunning = false;
                        }
                        if (run) {
                            runTests(current);
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

    public synchronized void pause() {
        paused = true;
        if (runner != null) {
            runner.pause();
        }
    }

    public synchronized void resume() {
        paused = false;
        notifyAll();
        if (runner != null) {
            runner.resume();
        }
    }

    public synchronized void disable() {
        disabled = true;
        notifyAll();
        if (runner != null) {
            runner.abort();
        }
    }

    public synchronized void enable() {
        if (!disabled) {
            return;
        }
        disabled = false;
        if (firstRun) {
            runTests();
        }
    }

    private void runInternal(ClassScanResult classScanResult, boolean reRunFailures) {
        final long runId = COUNTER.incrementAndGet();

        synchronized (this) {
            if (runner != null) {
                throw new IllegalStateException("Tests already in progress");
            }
            if (disabled) {
                return;
            }
            handleApplicationPropertiesChange();
            JunitTestRunner.Builder builder = new JunitTestRunner.Builder()
                    .setClassScanResult(classScanResult)
                    .setDevModeContext(devModeContext)
                    .setRunId(runId)
                    .setTestState(testSupport.testState)
                    .setTestClassUsages(testClassUsages)
                    .setTestApplication(testApplication)
                    .setIncludeTags(testSupport.includeTags)
                    .setExcludeTags(testSupport.excludeTags)
                    .setInclude(testSupport.include)
                    .setExclude(testSupport.exclude)
                    .setTestType(testSupport.testType)
                    .setFailingTestsOnly(classScanResult != null && testSupport.brokenOnlyMode); //broken only mode is only when changes are made, not for forced runs
            if (reRunFailures) {
                Set<UniqueId> ids = new HashSet<>();
                for (Map.Entry<String, TestClassResult> e : testSupport.testRunResults.getCurrentFailing().entrySet()) {
                    for (TestResult test : e.getValue().getFailing()) {
                        ids.add(test.uniqueId);
                    }
                }
                builder.addAdditionalFilter(new PostDiscoveryFilter() {
                    @Override
                    public FilterResult apply(TestDescriptor testDescriptor) {
                        return FilterResult.includedIf(ids.contains(testDescriptor.getUniqueId()));
                    }
                });
            }
            for (TestListener i : testSupport.testListeners) {
                i.testRunStarted(builder::addListener);
            }
            builder.addListener(new TestRunListener() {
                @Override
                public void runComplete(TestRunResults results) {
                    testSupport.testRunResults = results;
                }

                @Override
                public void noTests(TestRunResults results) {
                    testSupport.testRunResults = results;
                }
            });
            runner = builder
                    .build();
            if (paused) {
                runner.pause();
            }
        }
        runner.runTests();
        synchronized (this) {
            runner = null;
        }
        Map<String, Boolean> watched = TestWatchedFiles.retrieveWatchedFilePaths();
        if (watched != null) {
            RuntimeUpdatesProcessor.INSTANCE.setWatchedFilePaths(watched, true);
        }
        if (disabled) {
            return;
        }
        if (firstRun) {
            firstRun = false;
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
        for (Path rootPath : testApplication.getQuarkusBootstrap().getApplicationRoot()) {
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
                            testSupport.includeTags = Collections.emptyList();
                        } else {
                            testSupport.includeTags = Arrays.stream(includeTags.split(",")).map(String::trim)
                                    .collect(Collectors.toList());
                        }
                    }
                    if (!Objects.equals(excludeTags, appPropertiesExcludeTags)) {
                        if (excludeTags == null) {
                            testSupport.excludeTags = Collections.emptyList();
                        } else {
                            testSupport.excludeTags = Arrays.stream(excludeTags.split(",")).map(String::trim)
                                    .collect(Collectors.toList());
                        }
                    }
                    if (!Objects.equals(includePattern, appPropertiesIncludePattern)) {
                        if (includePattern == null) {
                            testSupport.include = null;
                        } else {
                            testSupport.include = Pattern.compile(includePattern);
                        }
                    }
                    if (!Objects.equals(excludePattern, appPropertiesExcludePattern)) {
                        if (excludePattern == null) {
                            testSupport.exclude = null;
                        } else {
                            testSupport.exclude = Pattern.compile(excludePattern);
                        }
                    }
                    if (!Objects.equals(testType, appPropertiesTestType)) {
                        if (testType == null) {
                            testSupport.testType = TestType.ALL;
                        } else {
                            testSupport.testType = new HyphenateEnumConverter<>(TestType.class).convert(testType);
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

    public void waitTillResumed() {
        synchronized (TestRunner.this) {
            while (paused && !disabled) {
                try {
                    TestRunner.this.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void testCompileFailed(Throwable e) {
        synchronized (this) {
            compileProblem = e;
        }

        for (TestListener i : testSupport.testListeners) {
            i.testCompileFailed(e.getMessage());
        }
    }

    public synchronized void testCompileSucceeded() {
        compileProblem = null;
        for (TestListener i : testSupport.testListeners) {
            i.testCompileSucceeded();
        }
    }

    public boolean isRunning() {
        return testsRunning;
    }

}
