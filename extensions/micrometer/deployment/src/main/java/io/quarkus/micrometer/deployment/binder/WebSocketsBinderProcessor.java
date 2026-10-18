package io.quarkus.micrometer.deployment.binder;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.micrometer.runtime.binder.websockets.WebSocketMetricsInterceptorProducerImpl;

/**
 * Add support for WebSockets Next instrumentation.
 */
@BuildSteps(onlyIf = MicrometerProcessor.MicrometerEnabled.class)
public class WebSocketsBinderProcessor {

    @BuildStep
    void registerWebSocketMetricsInterceptor(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer,
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.WEBSOCKETS_NEXT)) {
            additionalBeanProducer
                    .produce(AdditionalBeanBuildItem.unremovableOf(WebSocketMetricsInterceptorProducerImpl.class));
        }
    }

}
