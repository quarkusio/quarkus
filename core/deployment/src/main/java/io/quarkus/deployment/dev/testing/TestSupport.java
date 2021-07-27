package io.quarkus.deployment.dev.testing;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.deployment.dev.CompilationProvider;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.QuarkusCompiler;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.dev.spi.DevModeType;

public class TestSupport implements TestController {

    private static final Logger log = Logger.getLogger(TestSupport.class);

    final CuratedApplication curatedApplication;
    final List<CompilationProvider> compilationProviders;
    final DevModeContext context;
    final List<TestListener> testListeners = new CopyOnWriteArrayList<>();
    final TestState testState = new TestState();
    final DevModeType devModeType;

    volatile CuratedApplication testCuratedApplication;
    volatile QuarkusCompiler compiler;
    volatile TestRunner testRunner;
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

    public boolean isRunning() {
        if (testRunner == null) {
            return false;
        }
        return testRunner.isRunning();
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
        if (testRunner == null) {
            return new RunStatus(-1, -1);
        }
        long last = -1;
        //get the running test id before the current status
        //otherwise there is a race where they both could be -1 even though it has started
        long runningTestRunId = testRunner.getRunningTestRunId();
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
                        if (context.getApplicationRoot().getTest().isPresent()) {
                            started = true;
                            init();
                            testRunner.enable();
                            for (TestListener i : testListeners) {
                                i.testsEnabled();
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to create compiler, runtime compilation will be unavailable", e);
                    }

                }
            }
        }
    }

    public void init() {
        if (!context.getApplicationRoot().getTest().isPresent()) {
            return;
        }
        if (testCuratedApplication == null) {
            try {
                List<Path> paths = new ArrayList<>();
                paths.add(Paths.get(context.getApplicationRoot().getTest().get().getClassesPath()));
                paths.addAll(curatedApplication.getQuarkusBootstrap().getApplicationRoot().toList());
                testCuratedApplication = curatedApplication.getQuarkusBootstrap().clonedBuilder()
                        .setMode(QuarkusBootstrap.Mode.TEST)
                        .setAssertionsEnabled(true)
                        .setDisableClasspathCache(false)
                        .setIsolateDeployment(true)
                        .setBaseClassLoader(getClass().getClassLoader())
                        .setTest(true)
                        .setAuxiliaryApplication(true)
                        .setHostApplicationIsTestOnly(devModeType == DevModeType.TEST_ONLY)
                        .setApplicationRoot(PathsCollection.from(paths))
                        .build()
                        .bootstrap();
                compiler = new QuarkusCompiler(testCuratedApplication, compilationProviders, context);
                testRunner = new TestRunner(this, context, testCuratedApplication);
                QuarkusClassLoader cl = (QuarkusClassLoader) getClass().getClassLoader();
                cl.addCloseTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            stop();
                        } finally {
                            testCuratedApplication.close();
                        }
                    }
                });

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void stop() {
        if (started) {
            started = false;
            for (TestListener i : testListeners) {
                i.testsDisabled();
            }
        }
        if (testRunner != null) {
            testRunner.disable();
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

    public boolean isStarted() {
        return started;
    }

    public TestRunner getTestRunner() {
        return testRunner;
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

    public void pause() {
        TestRunner tr = this.testRunner;
        if (tr != null) {
            tr.pause();
        }
    }

    public void resume() {
        TestRunner tr = this.testRunner;
        if (tr != null) {
            tr.resume();
        }
    }

    public TestRunResults getResults() {
        return testRunResults;
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
        return testState;
    }

    @Override
    public void runAllTests() {
        getTestRunner().runTests();
    }

    @Override
    public void setDisplayTestOutput(boolean displayTestOutput) {
        this.explicitDisplayTestOutput = displayTestOutput;
        this.displayTestOutput = displayTestOutput;
    }

    @Override
    public void runFailedTests() {
        getTestRunner().runFailedTests();
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
