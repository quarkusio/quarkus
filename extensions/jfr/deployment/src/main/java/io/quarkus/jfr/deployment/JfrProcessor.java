package io.quarkus.jfr.deployment;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.Version;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.jfr.runtime.JfrRecorder;
import io.quarkus.jfr.runtime.OTelIdProducer;
import io.quarkus.jfr.runtime.QuarkusIdProducer;
import io.quarkus.jfr.runtime.http.rest.classic.ClassicServerFilter;
import io.quarkus.jfr.runtime.http.rest.classic.ClassicServerRecorderProducer;
import io.quarkus.jfr.runtime.http.rest.reactive.ReactiveServerFilters;
import io.quarkus.jfr.runtime.http.rest.reactive.ReactiveServerRecorderProducer;
import io.quarkus.jfr.runtime.http.rest.reactive.ServerStartRecordingHandler;
import io.quarkus.jfr.runtime.runtime.JfrRuntimeBean;
import io.quarkus.jfr.runtime.runtime.QuarkusRuntimeInfo;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.server.spi.GlobalHandlerCustomizerBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;

@BuildSteps
public class JfrProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JFR);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerVersion(List<FeatureBuildItem> features, BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> beanProduce, JfrRecorder recorder) {

        List<String> featureNames = features.stream().map(f -> f.getName()).toList();
        String quarkusVersion = Version.getVersion();

        beanProduce.produce(SyntheticBeanBuildItem.configure(QuarkusRuntimeInfo.class)
                .supplier(recorder.quarkusInfoSupplier(quarkusVersion, featureNames))
                .scope(ApplicationScoped.class)
                .setRuntimeInit().done());

        additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(JfrRuntimeBean.class)
                .build());
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
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<GlobalHandlerCustomizerBuildItem> globalHandlerCustomizerProducer) {

        if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(ReactiveServerRecorderProducer.class)
                    .build());

            filterBeans
                    .produce(new CustomContainerRequestFilterBuildItem(ReactiveServerFilters.class.getName()));

            globalHandlerCustomizerProducer
                    .produce(new GlobalHandlerCustomizerBuildItem(new ServerStartRecordingHandler.Customizer()));
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
                    .produce(new ResteasyJaxrsProviderBuildItem(ClassicServerFilter.class.getName()));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void runtimeInit(JfrRecorder recorder) {
        recorder.runtimeInit();
    }
}
