package io.quarkus.devui.deployment.menu;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.dev.testing.TestRunResults;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.continuoustesting.ContinuousTestingJsonRPCService;
import io.quarkus.devui.runtime.continuoustesting.ContinuousTestingRecorder;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Continuous Testing Page
 */
public class ContinuousTestingProcessor {

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
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

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createContinuousTestingPages() {

        InternalPageBuildItem continuousTestingPages = new InternalPageBuildItem("Continuous Testing", 30);

        continuousTestingPages.addPage(Page.webComponentPageBuilder()
                .namespace(NAMESPACE)
                .title("Continuous Testing")
                .icon("font-awesome-solid:flask-vial")
                .componentLink("qwc-continuous-testing.js"));

        return continuousTestingPages;

    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCService(LaunchModeBuildItem launchModeBuildItem) {

        registerStartMethod(launchModeBuildItem);
        registerStopMethod(launchModeBuildItem);
        registerRunAllMethod(launchModeBuildItem);
        registerRunFailedMethod(launchModeBuildItem);
        registerToggleBrokenOnlyMethod(launchModeBuildItem);
        registerGetResultsMethod(launchModeBuildItem);

        return new JsonRPCProvidersBuildItem(NAMESPACE, ContinuousTestingJsonRPCService.class);
    }

    private boolean testsDisabled(LaunchModeBuildItem launchModeBuildItem, Optional<TestSupport> ts) {
        return ts.isEmpty() || launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL;
    }

    private void registerStartMethod(LaunchModeBuildItem launchModeBuildItem) {
        DevConsoleManager.register(NAMESPACE + DASH + "start", ignored -> {

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
        });
    }

    private void registerStopMethod(LaunchModeBuildItem launchModeBuildItem) {
        DevConsoleManager.register(NAMESPACE + DASH + "stop", ignored -> {

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
        });
    }

    private void registerRunAllMethod(LaunchModeBuildItem launchModeBuildItem) {
        DevConsoleManager.register(NAMESPACE + DASH + "runAll", ignored -> {

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
        });
    }

    private void registerRunFailedMethod(LaunchModeBuildItem launchModeBuildItem) {
        DevConsoleManager.register(NAMESPACE + DASH + "runFailed", ignored -> {

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
        });
    }

    private void registerToggleBrokenOnlyMethod(LaunchModeBuildItem launchModeBuildItem) {
        DevConsoleManager.register(NAMESPACE + DASH + "toggleBrokenOnly", ignored -> {

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
        });
    }

    private void registerGetResultsMethod(LaunchModeBuildItem launchModeBuildItem) {
        DevConsoleManager.register(NAMESPACE + DASH + "getResults", ignored -> {
            ObjectMapper objectMapper = new ObjectMapper(); // Remove in favior of build in.
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

                try (StringWriter sw = new StringWriter()) {
                    objectMapper.writeValue(sw, testRunResults);
                    return sw.toString();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static final String NAMESPACE = "devui-continuous-testing";
    private static final String DASH = "-";

}
