package io.quarkus.jfr.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jfr.runtime.RequestIdProducerImpl;
import io.quarkus.jfr.runtime.TracingRequestIdProducer;
import io.quarkus.jfr.runtime.http.rest.JfrRestReactiveFilter;
import io.quarkus.jfr.runtime.http.rest.RestEventFactory;
import io.quarkus.jfr.runtime.http.rest.RestReactiveRecorder;
import io.quarkus.jfr.runtime.http.rest.tracing.TracingRestEventFactory;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;

@BuildSteps
class JfrProcessor {

    private static final String FEATURE = "jfr";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerRequestIdProducer(Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (capabilities.isPresent(Capability.OPENTELEMETRY_TRACER)) {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(TracingRequestIdProducer.class, TracingRestEventFactory.class)
                    .build());

        } else {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(RequestIdProducerImpl.class, RestEventFactory.class)
                    .build());
        }
    }

    @BuildStep
    void registerReactiveResteasyIntegration(Capabilities capabilities,
            BuildProducer<CustomContainerRequestFilterBuildItem> filterBeans,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(RestReactiveRecorder.class)
                    .build());

            filterBeans
                    .produce(new CustomContainerRequestFilterBuildItem(JfrRestReactiveFilter.class.getName()));
        }
    }
}
