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
@DevMcpBuildTimeTool(name = "runTests", description = "Run all tests synchronously and return the results. Does not require continuous testing to be started.")
@DevMcpBuildTimeTool(name = "runAffectedTests", description = "Run tests affected by recent code changes synchronously and return the results. On first call, runs all tests to populate tracing data. Subsequent calls intelligently filter to only affected tests.")
@DevMcpBuildTimeTool(name = "runTest", description = "Run a specific test synchronously and return the results. Accepts a test class name or class and method.", params = {
        @DevMcpParam(name = "className", description = "The fully qualified test class name, e.g. com.example.MyTest"),
        @DevMcpParam(name = "methodName", description = "The test method name to run. If not provided, all tests in the class are run.", required = false)
})
public class OneShotTestingProcessor {

    private static final String NAMESPACE = "devui-testing";
    private static final long TEST_TIMEOUT_MINUTES = 5;
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
                .methodName("runTests")
                .description("Run all tests synchronously and return the results. "
                        + "Does not require continuous testing to be started.")
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
                .methodName("runAffectedTests")
                .description("Run tests affected by recent code changes synchronously and return the results. "
                        + "On first call (no prior test data), runs all tests to populate tracing data. "
                        + "Subsequent calls intelligently filter to only affected tests.")
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
                .methodName("runTest")
                .description("Run a specific test synchronously and return the results. "
                        + "Accepts a test class name (e.g. com.example.MyTest) "
                        + "or class and method (e.g. com.example.MyTest#myMethod).")
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
}
