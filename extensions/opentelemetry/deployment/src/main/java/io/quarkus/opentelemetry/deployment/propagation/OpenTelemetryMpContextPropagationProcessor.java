package io.quarkus.opentelemetry.deployment.propagation;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;
import io.quarkus.opentelemetry.runtime.propagation.OpenTelemetryMpContextPropagationProvider;
import io.quarkus.smallrye.context.deployment.spi.ThreadContextProviderBuildItem;

@BuildSteps(onlyIf = OpenTelemetryEnabled.class)
public class OpenTelemetryMpContextPropagationProcessor {

    @BuildStep
    void registerOpenTelemetryThreadProvider(BuildProducer<ThreadContextProviderBuildItem> threadContextProvider) {
        threadContextProvider
                .produce(new ThreadContextProviderBuildItem(OpenTelemetryMpContextPropagationProvider.class));
    }
}
