package io.quarkus.devui.deployment.logstream;

import java.util.Map;
import java.util.Optional;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.StreamingLogHandlerBuildItem;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.deployment.logging.LoggingDecorateBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.devui.runtime.logstream.LogStreamBroadcaster;
import io.quarkus.devui.runtime.logstream.LogStreamJsonRPCService;
import io.quarkus.devui.runtime.logstream.LogStreamRecorder;
import io.quarkus.devui.runtime.logstream.MutinyLogHandler;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Processor for Log stream in Dev UI
 */
public class LogStreamProcessor {

    private final String namespace = "devui-logstream";

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void additionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(LogStreamBroadcaster.class)
                .setUnremovable().build());
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    @SuppressWarnings("unchecked")
    public void handler(BuildProducer<StreamingLogHandlerBuildItem> streamingLogHandlerBuildItem,
            LoggingDecorateBuildItem loggingDecorateBuildItem, LogStreamRecorder recorder) {
        RuntimeValue<Optional<MutinyLogHandler>> mutinyLogHandler = recorder.mutinyLogHandler(
                loggingDecorateBuildItem.getSrcMainJava().toString(), loggingDecorateBuildItem.getKnowClasses());
        streamingLogHandlerBuildItem.produce(new StreamingLogHandlerBuildItem((RuntimeValue) mutinyLogHandler));
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void registerBuildTimeActions(BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            LaunchModeBuildItem launchModeBuildItem) {

        Optional<TestSupport> ts = TestSupport.instance();

        BuildTimeActionBuildItem keyStrokeActions = new BuildTimeActionBuildItem(namespace);

        keyStrokeActions.addAction("forceRestart", ignored -> {
            RuntimeUpdatesProcessor.INSTANCE.doScan(true, true);
            return Map.of();
        });

        keyStrokeActions.addAction("rerunAllTests", ignored -> {
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

        keyStrokeActions.addAction("rerunFailedTests", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            ts.get().runFailedTests();
            return Map.of();
        });

        keyStrokeActions.addAction("toggleBrokenOnly", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            boolean brokenOnlyMode = ts.get().toggleBrokenOnlyMode();
            return Map.of("brokenOnlyMode", brokenOnlyMode);
        });

        keyStrokeActions.addAction("printFailures", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            ts.get().printFullResults();
            return Map.of();
        });

        keyStrokeActions.addAction("toggleTestOutput", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            boolean isTestOutput = ts.get().toggleTestOutput();
            return Map.of("isTestOutput", isTestOutput);
        });

        keyStrokeActions.addAction("toggleInstrumentationReload", ignored -> {
            boolean instrumentationEnabled = RuntimeUpdatesProcessor.INSTANCE.toggleInstrumentation();
            return Map.of("instrumentationEnabled", instrumentationEnabled);
        });

        keyStrokeActions.addAction("pauseTests", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            if (ts.get().isStarted()) {
                ts.get().stop();
                return Map.of("running", ts.get().isRunning());
            }
            return Map.of();
        });

        keyStrokeActions.addAction("toggleLiveReload", ignored -> {
            if (testsDisabled(launchModeBuildItem, ts)) {
                return Map.of();
            }
            boolean liveReloadEnabled = ts.get().toggleLiveReloadEnabled();
            return Map.of("liveReloadEnabled", liveReloadEnabled);
        });

        buildTimeActionProducer.produce(keyStrokeActions);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem(namespace, LogStreamJsonRPCService.class);
    }

    private boolean testsDisabled(LaunchModeBuildItem launchModeBuildItem, Optional<TestSupport> ts) {
        return ts.isEmpty() || launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL;
    }

}
