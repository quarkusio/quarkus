package io.quarkus.devui.deployment.logstream;

import java.util.Optional;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.StreamingLogHandlerBuildItem;
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
    JsonRPCProvidersBuildItem createJsonRPCService() {
        return new JsonRPCProvidersBuildItem("devui-logstream", LogStreamJsonRPCService.class);
    }

}
