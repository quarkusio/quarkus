package io.quarkus.jfr.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.*;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.jfr.runtime.JfrRecorder;
import io.quarkus.jfr.runtime.OTelIdProducer;
import io.quarkus.jfr.runtime.QuarkusIdProducer;
import io.quarkus.jfr.runtime.config.JfrRuntimeConfig;
import io.quarkus.jfr.runtime.http.rest.ClassicServerRecorderProducer;
import io.quarkus.jfr.runtime.http.rest.JfrClassicServerFilter;
import io.quarkus.jfr.runtime.http.rest.JfrReactiveServerFilter;
import io.quarkus.jfr.runtime.http.rest.ReactiveServerRecorderProducer;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;

@BuildSteps
public class JfrProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JFR);
    }

    @BuildStep
    void registerRequestIdProducer(Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClassBuildItem) {

        if (capabilities.isPresent(Capability.OPENTELEMETRY_TRACER)) {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(OTelIdProducer.class)
                    .build());

        } else {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(QuarkusIdProducer.class)
                    .build());

            runtimeInitializedClassBuildItem
                    .produce(new RuntimeInitializedClassBuildItem(QuarkusIdProducer.class.getCanonicalName()));
        }
    }

    @BuildStep
    void registerRestIntegration(Capabilities capabilities,
            BuildProducer<CustomContainerRequestFilterBuildItem> filterBeans,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(ReactiveServerRecorderProducer.class)
                    .build());

            filterBeans
                    .produce(new CustomContainerRequestFilterBuildItem(JfrReactiveServerFilter.class.getName()));
        }
    }

    @BuildStep
    void registerResteasyClassicIntegration(Capabilities capabilities,
            BuildProducer<ResteasyJaxrsProviderBuildItem> resteasyJaxrsProviderBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (capabilities.isPresent(Capability.RESTEASY)) {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(ClassicServerRecorderProducer.class)
                    .build());

            resteasyJaxrsProviderBuildItemBuildProducer
                    .produce(new ResteasyJaxrsProviderBuildItem(JfrClassicServerFilter.class.getName()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void runtimeInit(JfrRecorder recorder, JfrRuntimeConfig runtimeConfig) {
        recorder.runtimeInit(runtimeConfig);
    }
}
