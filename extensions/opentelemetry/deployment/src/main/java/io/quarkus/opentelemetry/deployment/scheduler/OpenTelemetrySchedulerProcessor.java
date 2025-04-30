package io.quarkus.opentelemetry.deployment.scheduler;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.scheduler.OpenTelemetryJobInstrumenter;

public class OpenTelemetrySchedulerProcessor {

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    void registerJobInstrumenter(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans) {
        if (capabilities.isPresent(Capability.SCHEDULER)) {
            beans.produce(new AdditionalBeanBuildItem(OpenTelemetryJobInstrumenter.class));
        }
    }

}
