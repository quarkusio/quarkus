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
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.observability.ObservabilityCustomizer;
import io.quarkus.runtime.metrics.MetricsFactory;

public class ObservabilityProcessor {

    @BuildStep
    MethodScannerBuildItem integrateObservability(Capabilities capabilities,
            Optional<MetricsCapabilityBuildItem> metricsCapability) {
        boolean integrationNeeded = (capabilities.isPresent(Capability.OPENTELEMETRY_TRACER) ||
                (metricsCapability.isPresent()
                        && metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)));
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

}
