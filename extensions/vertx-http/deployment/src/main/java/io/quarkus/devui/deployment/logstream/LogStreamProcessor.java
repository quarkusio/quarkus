package io.quarkus.devui.deployment.logstream;

import java.util.Map;
import java.util.Optional;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.StreamingLogHandlerBuildItem;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.runtime.logstream.LogStreamBroadcaster;
import io.quarkus.devui.runtime.logstream.LogStreamJsonRPCService;
import io.quarkus.devui.runtime.logstream.LogStreamRecorder;
import io.quarkus.devui.runtime.logstream.MutinyLogHandler;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Processor for Log stream in Dev UI
 */
public class LogStreamProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(LogStreamBroadcaster.class)
                .setUnremovable().build());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    @SuppressWarnings("unchecked")
    public void handler(BuildProducer<StreamingLogHandlerBuildItem> streamingLogHandlerBuildItem,
            LogStreamRecorder recorder) {
        RuntimeValue<Optional<MutinyLogHandler>> mutinyLogHandler = recorder.mutinyLogHandler();
        streamingLogHandlerBuildItem.produce(new StreamingLogHandlerBuildItem((RuntimeValue) mutinyLogHandler));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCService(LaunchModeBuildItem launchModeBuildItem) {
        Optional<TestSupport> ts = TestSupport.instance();

        DevConsoleManager.register("logstream-force-restart", ignored -> {
            RuntimeUpdatesProcessor.INSTANCE.doScan(true, true);
            return Map.of();
        });

        DevConsoleManager.register("logstream-rerun-all-tests", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            if (ts.get().isStarted()) {
                ts.get().runAllTests();
                return Map.of();
            } else {
                ts.get().start();
                return Map.of("running", ts.get().isRunning());
            }
        });

        DevConsoleManager.register("logstream-rerun-failed-tests", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            ts.get().runFailedTests();
            return Map.of();
        });

        DevConsoleManager.register("logstream-toggle-broken-only", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            boolean brokenOnlyMode = ts.get().toggleBrokenOnlyMode();
            return Map.of("brokenOnlyMode", brokenOnlyMode);
        });

        DevConsoleManager.register("logstream-print-failures", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            ts.get().printFullResults();
            return Map.of();
        });

        DevConsoleManager.register("logstream-toggle-test-output", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            boolean isTestOutput = ts.get().toggleTestOutput();
            return Map.of("isTestOutput", isTestOutput);
        });

        DevConsoleManager.register("logstream-toggle-instrumentation-reload", ignored -> {
            boolean instrumentationEnabled = RuntimeUpdatesProcessor.INSTANCE.toggleInstrumentation();
            return Map.of("instrumentationEnabled", instrumentationEnabled);
        });

        DevConsoleManager.register("logstream-pause-tests", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            if (ts.get().isStarted()) {
                ts.get().stop();
                return Map.of("running", ts.get().isRunning());
            }
            return Map.of();
        });

        DevConsoleManager.register("logstream-toggle-live-reload", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            boolean liveReloadEnabled = ts.get().toggleLiveReloadEnabled();
            return Map.of("liveReloadEnabled", liveReloadEnabled);
        });

        DevConsoleManager.register("logstream-toggle-live-reload", ignored -> {

            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            boolean liveReloadEnabled = ts.get().toggleLiveReloadEnabled();
            return Map.of("liveReloadEnabled", liveReloadEnabled);
        });

        return new JsonRPCProvidersBuildItem("devui-logstream", LogStreamJsonRPCService.class);
    }

    private boolean testsDisabled(LaunchModeBuildItem launchModeBuildItem, Optional<TestSupport> ts) {
        return ts.isEmpty() || launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL;
    }

}
