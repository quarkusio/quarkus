package io.quarkus.devui.deployment.menu;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.continuoustesting.ContinuousTestingJsonRPCService;
import io.quarkus.devui.runtime.continuoustesting.ContinuousTestingRecorder;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Continuous Testing Page
 */
public class ContinuousTestingProcessor {

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public void continuousTestingState(
            ContinuousTestingRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            ShutdownContextBuildItem shutdownContextBuildItem,
            BeanContainerBuildItem beanContainer) throws IOException {

        DevModeType devModeType = launchModeBuildItem.getDevModeType().orElse(null);
        if (devModeType == null || !devModeType.isContinuousTestingSupported()) {
            return;
        }

        if (TestSupport.instance().isPresent()) {
            // Add continuous testing
            recorder.createContinuousTestingSharedStateManager(beanContainer.getValue(), shutdownContextBuildItem);
        }

    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    InternalPageBuildItem createContinuousTestingPages() {

        InternalPageBuildItem continuousTestingPages = new InternalPageBuildItem("Continuous Testing", 30,
                "qwc-continuous-testing-menu-action");

        continuousTestingPages.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .title("Continuous Testing")
                .icon("font-awesome-solid:flask-vial")
                .componentLink("qwc-continuous-testing.js"));

        return continuousTestingPages;

    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void registerBuildTimeActions(LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer) {

        BuildTimeActionBuildItem actions = new BuildTimeActionBuildItem(NAMESPACE);

        registerStartMethod(launchModeBuildItem, actions);
        registerStopMethod(launchModeBuildItem, actions);
        registerRunAllMethod(launchModeBuildItem, actions);
        registerRunFailedMethod(launchModeBuildItem, actions);
        registerToggleBrokenOnlyMethod(launchModeBuildItem, actions);
        registerToggleInstrumentationMethod(launchModeBuildItem, actions);
        registerGetResultsMethod(launchModeBuildItem, actions);
        registerGetResultsMCPMethod(launchModeBuildItem, actions);
        registerGetStatusMethod(launchModeBuildItem, actions);
        buildTimeActionProducer.produce(actions);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(NAMESPACE, ContinuousTestingJsonRPCService.class);
    }

    private boolean testsDisabled(LaunchModeBuildItem launchModeBuildItem, Optional<TestSupport> ts) {
        return ts.isEmpty() || launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL;
    }

    private void registerStartMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName("start")
                .description("Start the Continuous Testing")
                .function(ignored -> {
                    try {
                        Optional<TestSupport> ts = TestSupport.instance();
                        if (testsDisabled(launchModeBuildItem, ts)) {
                            return false;
                        }
                        TestSupport testSupport = ts.get();

                        if (testSupport.isStarted()) {
                            return false; // Already running
                        } else {
                            testSupport.start();
                            return true;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .enableMcpFuctionByDefault()
                .build();
    }

    private void registerStopMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName("stop")
                .description("Stop the Continuous Testing")
                .function(ignored -> {
                    try {
                        Optional<TestSupport> ts = TestSupport.instance();
                        if (testsDisabled(launchModeBuildItem, ts)) {
                            return false;
                        }
                        TestSupport testSupport = ts.get();

                        if (testSupport.isStarted()) {
                            testSupport.stop();
                            return true;
                        } else {
                            return false; // Already running
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .enableMcpFuctionByDefault()
                .build();
    }

    private void registerRunAllMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName("runAll")
                .description("Run all tests in Continuous Testing if it's started")
                .function(ignored -> {
                    try {
                        Optional<TestSupport> ts = TestSupport.instance();
                        if (testsDisabled(launchModeBuildItem, ts)) {
                            return false;
                        }
                        TestSupport testSupport = ts.get();
                        testSupport.runAllTests();
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .enableMcpFuctionByDefault()
                .build();
    }

    private void registerRunFailedMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName("runFailed")
                .description("Run all failed tests in Continuous Testing if it's started")
                .function(ignored -> {
                    try {
                        Optional<TestSupport> ts = TestSupport.instance();
                        if (testsDisabled(launchModeBuildItem, ts)) {
                            return false;
                        }
                        TestSupport testSupport = ts.get();
                        testSupport.runFailedTests();
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();
    }

    private void registerToggleBrokenOnlyMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName("toggleBrokenOnly")
                .description("Toggle broken only in Continuous Testing")
                .function(ignored -> {
                    try {
                        Optional<TestSupport> ts = TestSupport.instance();
                        if (testsDisabled(launchModeBuildItem, ts)) {
                            return false;
                        }
                        TestSupport testSupport = ts.get();
                        return testSupport.toggleBrokenOnlyMode();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();
    }

    private void registerToggleInstrumentationMethod(LaunchModeBuildItem launchModeBuildItem,
            BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName("toggleInstrumentation")
                .description("Toggle instrumentation in Continuous Testing")
                .function(ignored -> {
                    try {
                        Optional<TestSupport> ts = TestSupport.instance();
                        if (testsDisabled(launchModeBuildItem, ts)) {
                            return false;
                        }
                        TestSupport testSupport = ts.get();
                        return testSupport.toggleInstrumentation();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();
    }

    private void registerGetStatusMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName("getStatus")
                .description("Get the status of Continuous Testing")
                .function(ignored -> {
                    try {
                        Optional<TestSupport> ts = TestSupport.instance();
                        if (testsDisabled(launchModeBuildItem, ts)) {
                            return null;
                        }
                        TestSupport testSupport = ts.get();
                        TestSupport.RunStatus status = testSupport.getStatus();

                        if (status == null) {
                            return null;
                        }

                        Map<String, Long> testStatus = new HashMap<>();

                        long lastRun = status.getLastRun();
                        testStatus.put("lastRun", lastRun);
                        if (lastRun > 0) {
                            TestRunResults result = testSupport.getResults();
                            testStatus.put("testsFailed", result.getCurrentFailedCount());
                            testStatus.put("testsPassed", result.getCurrentPassedCount());
                            testStatus.put("testsSkipped", result.getCurrentSkippedCount());
                            testStatus.put("testsRun", result.getFailedCount() + result.getPassedCount());
                            testStatus.put("totalTestsFailed", result.getFailedCount());
                            testStatus.put("totalTestsPassed", result.getPassedCount());
                            testStatus.put("totalTestsSkipped", result.getSkippedCount());
                        }
                        //get running last, as otherwise if the test completes in the meantime you could see
                        //both running and last run being the same number
                        testStatus.put("running", status.getRunning());
                        return testStatus;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .enableMcpFuctionByDefault()
                .build();
    }

    private void registerGetResultsMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName("getResults")
                .description("Get the full results of a Continuous testing test run")
                .function(ignored -> {
                    try {
                        Optional<TestSupport> ts = TestSupport.instance();
                        if (testsDisabled(launchModeBuildItem, ts)) {
                            return null;
                        }
                        TestSupport testSupport = ts.get();
                        TestRunResults testRunResults = testSupport.getResults();

                        if (testRunResults == null) {
                            return null;
                        }

                        return testRunResults;

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();
    }

    /**
     * Duplicate of getResults but using a MCP friendly version of TestRunResults.
     * TODO to consolidate with getResults after we evaluate other usage
     */
    private void registerGetResultsMCPMethod(LaunchModeBuildItem launchModeBuildItem, BuildTimeActionBuildItem actions) {
        actions.actionBuilder()
                .methodName("getResultsMCP")
                .description("Get the results of a Continuous testing test run")
                .enableMcpFuctionByDefault()
                .function(ignored -> {
                    try {
                        Optional<TestSupport> ts = TestSupport.instance();
                        if (testsDisabled(launchModeBuildItem, ts)) {
                            return null;
                        }
                        TestSupport testSupport = ts.get();
                        TestRunResults testRunResults = testSupport.getResults();

                        if (testRunResults == null) {
                            return null;
                        }

                        return new TrimmedTestRunResult(testRunResults);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();
    }

    private static final String NAMESPACE = "devui-continuous-testing";
}
