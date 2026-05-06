package io.quarkus.devui.deployment.menu;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.ClassScanResult;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.buildtime.DevMcpBuildTimeTool;
import io.quarkus.devui.spi.buildtime.DevMcpParam;

/**
 * Registers one-shot synchronous testing tools for AI coding agents.
 * These tools run tests on demand and return results directly,
 * without requiring continuous testing to be started.
 */
@DevMcpBuildTimeTool(name = OneShotTestingProcessor.RUN_TESTS_NAME, description = OneShotTestingProcessor.RUN_TESTS_DESC)
@DevMcpBuildTimeTool(name = OneShotTestingProcessor.RUN_AFFECTED_TESTS_NAME, description = OneShotTestingProcessor.RUN_AFFECTED_TESTS_DESC)
@DevMcpBuildTimeTool(name = OneShotTestingProcessor.RUN_TEST_NAME, description = OneShotTestingProcessor.RUN_TEST_DESC, params = {
        @DevMcpParam(name = "className", description = "The fully qualified test class name, e.g. com.example.MyTest"),
        @DevMcpParam(name = "methodName", description = "The test method name to run. If not provided, all tests in the class are run.", required = false)
})
@DevMcpBuildTimeTool(name = OneShotTestingProcessor.CANCEL_TESTS_NAME, description = OneShotTestingProcessor.CANCEL_TESTS_DESC)
@DevMcpBuildTimeTool(name = OneShotTestingProcessor.RESET_TESTS_NAME, description = OneShotTestingProcessor.RESET_TESTS_DESC)
public class OneShotTestingProcessor {

    private static final String NAMESPACE = "devui-testing";
    private static final long TEST_TIMEOUT_MINUTES = 5;

    static final String RUN_TESTS_NAME = "runTests";
    static final String RUN_TESTS_DESC = "Run all tests synchronously and return the results. "
            + "Does not require continuous testing to be started.";
    static final String RUN_AFFECTED_TESTS_NAME = "runAffectedTests";
    static final String RUN_AFFECTED_TESTS_DESC = "Run tests affected by recent code changes synchronously and return the results. "
            + "On first call (no prior test data), runs all tests to populate tracing data. "
            + "Subsequent calls intelligently filter to only affected tests.";
    static final String RUN_TEST_NAME = "runTest";
    static final String RUN_TEST_DESC = "Run a specific test synchronously and return the results. "
            + "Accepts a test class name (e.g. com.example.MyTest) "
            + "or class and method (e.g. com.example.MyTest#myMethod).";
    static final String CANCEL_TESTS_NAME = "cancelTests";
    static final String CANCEL_TESTS_DESC = "Cancel a currently running test execution. "
            + "Use when tests are taking too long or you need to run different tests.";
    static final String RESET_TESTS_NAME = "resetTests";
    static final String RESET_TESTS_DESC = "Reset the test runner state. "
            + "Use when the test runner is stuck reporting 'tests already in progress' after a failure.";
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "oneshot-test-runner");
        t.setDaemon(true);
        return t;
    });

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void registerOneShotTestingActions(LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer) {

        BuildTimeActionBuildItem actions = new BuildTimeActionBuildItem(NAMESPACE);
        registerRunTestsMethod(launchModeBuildItem, actions);
        registerRunAffectedTestsMethod(launchModeBuildItem, actions);
        registerRunTestMethod(launchModeBuildItem, actions);
        registerCancelTestsMethod(launchModeBuildItem, actions);
        registerResetTestsMethod(launchModeBuildItem, actions);
        buildTimeActionProducer.produce(actions);
    }

    private boolean testsDisabled(LaunchModeBuildItem launchModeBuildItem, Optional<TestSupport> ts) {
        return ts.isEmpty() || launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL;
    }

    /**
     * Compile any changed main and test source files before running tests.
     * This is necessary because the one-shot test tools do not use continuous testing,
     * so test sources written after the app started would not be compiled otherwise.
     * Returns the ClassScanResult so the test runner knows which classes changed.
     */
    private static ClassScanResult compileTestSources() {
        if (RuntimeUpdatesProcessor.INSTANCE != null) {
            return RuntimeUpdatesProcessor.INSTANCE.checkForChangedClassesForTests();
        }
        return null;
    }

    private void registerRunTestsMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName(RUN_TESTS_NAME)
                .description(RUN_TESTS_DESC)
                .function(ignored -> {
                    Optional<TestSupport> ts = TestSupport.instance();
                    if (testsDisabled(launchModeBuildItem, ts)) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return CompletableFuture.supplyAsync(() -> {
                        compileTestSources();
                        TestRunResults results = ts.get().runAllTestsSynchronously();
                        return results != null ? new TrimmedTestRunResult(results) : null;
                    }, executor).orTimeout(TEST_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                })
                .enableMcpFunctionByDefault()
                .build();
    }

    private void registerRunAffectedTestsMethod(LaunchModeBuildItem launchModeBuildItem,
            BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName(RUN_AFFECTED_TESTS_NAME)
                .description(RUN_AFFECTED_TESTS_DESC)
                .function(ignored -> {
                    Optional<TestSupport> ts = TestSupport.instance();
                    if (testsDisabled(launchModeBuildItem, ts)) {
                        return CompletableFuture.completedFuture(null);
                    }
                    return CompletableFuture.supplyAsync(() -> {
                        compileTestSources();
                        TestSupport testSupport = ts.get();
                        boolean usageDataAvailable = testSupport.hasTestClassUsageData();
                        TestRunResults results = testSupport.runAffectedTestsSynchronously();
                        return results != null ? new TrimmedTestRunResult(results, usageDataAvailable) : null;
                    }, executor).orTimeout(TEST_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                })
                .enableMcpFunctionByDefault()
                .build();
    }

    private void registerRunTestMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName(RUN_TEST_NAME)
                .description(RUN_TEST_DESC)
                .parameter("className", "The fully qualified test class name, e.g. com.example.MyTest")
                .parameter("methodName",
                        "The test method name to run. If not provided, all tests in the class are run.",
                        false)
                .function(params -> {
                    Optional<TestSupport> ts = TestSupport.instance();
                    if (testsDisabled(launchModeBuildItem, ts)) {
                        return CompletableFuture.completedFuture(null);
                    }
                    String className = params.get("className");
                    if (className == null || className.isBlank()) {
                        throw new IllegalArgumentException("className parameter is required");
                    }
                    String methodName = params.get("methodName");
                    String testSelection = className;
                    if (methodName != null && !methodName.isBlank()) {
                        testSelection = className + "#" + methodName;
                    }
                    final String selection = testSelection;
                    return CompletableFuture.supplyAsync(() -> {
                        compileTestSources();
                        TestRunResults results = ts.get().runSpecificTestSynchronously(selection);
                        return results != null ? new TrimmedTestRunResult(results) : null;
                    }, executor).orTimeout(TEST_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                })
                .enableMcpFunctionByDefault()
                .build();
    }

    private void registerCancelTestsMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName(CANCEL_TESTS_NAME)
                .description(CANCEL_TESTS_DESC)
                .function(ignored -> {
                    Optional<TestSupport> ts = TestSupport.instance();
                    if (testsDisabled(launchModeBuildItem, ts)) {
                        return CompletableFuture.completedFuture("Test support is not available");
                    }
                    ts.get().cancelRunningTests();
                    return CompletableFuture.completedFuture("Tests cancelled");
                })
                .enableMcpFunctionByDefault()
                .build();
    }

    private void registerResetTestsMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName(RESET_TESTS_NAME)
                .description(RESET_TESTS_DESC)
                .function(ignored -> {
                    Optional<TestSupport> ts = TestSupport.instance();
                    if (testsDisabled(launchModeBuildItem, ts)) {
                        return CompletableFuture.completedFuture("Test support is not available");
                    }
                    ts.get().resetTestState();
                    return CompletableFuture.completedFuture("Test runner state reset");
                })
                .enableMcpFunctionByDefault()
                .build();
    }
}
