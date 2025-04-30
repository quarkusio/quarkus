package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.observability.ObservabilityCustomizer;
import io.quarkus.resteasy.reactive.server.runtime.observability.ObservabilityIntegrationRecorder;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.vertx.http.deployment.FilterBuildItem;

public class ObservabilityProcessor {

    @BuildStep
    MethodScannerBuildItem methodScanner(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        boolean integrationNeeded = integrationNeeded(capabilities, metricsCapability);
        if (!integrationNeeded) {
            return null;
        }
        return new MethodScannerBuildItem(new MethodScanner() {
            @Override
            public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                    Map<String, Object> methodContext) {
                return Collections.singletonList(new ObservabilityCustomizer());
            }
        });
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    void preAuthFailureFilter(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability,
            ObservabilityIntegrationRecorder recorder,
            ResteasyReactiveDeploymentBuildItem deployment,
            BuildProducer<FilterBuildItem> filterProducer,
            BuildProducer<ObservabilityIntegrationBuildItem> observabilityIntegrationProducer) {
        boolean integrationNeeded = integrationNeeded(capabilities, metricsCapability);
        if (!integrationNeeded) {
            return;
        }

        filterProducer.produce(FilterBuildItem.ofPreAuthenticationFailureHandler(
                recorder.preAuthFailureHandler(deployment.getDeployment())));
        observabilityIntegrationProducer.produce(new ObservabilityIntegrationBuildItem());
    }

    private boolean integrationNeeded(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        return capabilities.isPresent(Capability.OPENTELEMETRY_TRACER) ||
                (metricsCapability.isPresent()
                        && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER));
    }

}
