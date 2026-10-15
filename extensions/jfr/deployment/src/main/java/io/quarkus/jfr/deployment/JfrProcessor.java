package io.quarkus.jfr.deployment;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.Version;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.jfr.runtime.internal.JfrRuntimeSetup;
import io.quarkus.jfr.runtime.internal.OTelIdProducer;
import io.quarkus.jfr.runtime.internal.QuarkusIdProducer;
import io.quarkus.jfr.runtime.internal.config.JfrRuntimeConfig;
import io.quarkus.jfr.runtime.internal.http.rest.classic.ClassicServerEventRecorderProducer;
import io.quarkus.jfr.runtime.internal.http.rest.classic.ClassicServerFilter;
import io.quarkus.jfr.runtime.internal.http.rest.client.RestClientFilter;
import io.quarkus.jfr.runtime.internal.http.rest.reactive.ReactiveServerEventRecorderProducer;
import io.quarkus.jfr.runtime.internal.http.rest.reactive.ReactiveServerFilters;
import io.quarkus.jfr.runtime.internal.http.rest.reactive.ServerStartRecordingHandler;
import io.quarkus.jfr.runtime.internal.runtime.JfrRuntimeBean;
import io.quarkus.jfr.runtime.internal.runtime.QuarkusRuntimeInfo;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.server.spi.GlobalHandlerCustomizerBuildItem;
import io.quarkus.resteasy.reactive.server.spi.KnownServerRestHandlerBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;

@BuildSteps
public class JfrProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.JFR);
    }

    @BuildStep
    void registerVersion(List<FeatureBuildItem> features, ServiceRegistrar serviceRegistrar,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> beanProduce,
            BuildProducer<ServiceStartBuildItem> serviceStart) {

        List<String> featureNames = features.stream().map(FeatureBuildItem::getName).toList();
        String quarkusVersion = Version.getVersion();

        serviceRegistrar
                .forService(QuarkusRuntimeInfo.class)
                .onStart(ctx -> QuarkusRuntimeInfo.create(quarkusVersion, featureNames));

        beanProduce.produce(SyntheticBeanBuildItem.configure(QuarkusRuntimeInfo.class)
                .serviceValue(QuarkusRuntimeInfo.class)
                .scope(ApplicationScoped.class)
                .setRuntimeInit().done());

        // makes sure the service has run before JfrRuntimeBean reads the bean in its StartupEvent observer
        serviceStart.produce(new ServiceStartBuildItem("jfr-runtime-info"));

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
            BuildProducer<GlobalHandlerCustomizerBuildItem> globalHandlerCustomizerProducer,
            BuildProducer<KnownServerRestHandlerBuildItem> knownServerRestHandlerProducer) {

        if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(ReactiveServerEventRecorderProducer.class)
                    .build());

            filterBeans
                    .produce(new CustomContainerRequestFilterBuildItem(ReactiveServerFilters.class.getName()));

            globalHandlerCustomizerProducer
                    .produce(new GlobalHandlerCustomizerBuildItem(new ServerStartRecordingHandler.Customizer()));
            knownServerRestHandlerProducer
                    .produce(new KnownServerRestHandlerBuildItem(ServerStartRecordingHandler.class.getName()));
        }
    }

    @BuildStep
    void registerRestClientIntegration(Capabilities capabilities,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexed,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (capabilities.isPresent(Capability.REST_CLIENT_REACTIVE)) {
            // the REST Client picks up the @Provider annotation from the index
            additionalIndexed.produce(new AdditionalIndexedClassesBuildItem(RestClientFilter.class.getName()));
            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(RestClientFilter.class)
                    .build());
        }
    }

    @BuildStep
    void registerResteasyClassicIntegration(Capabilities capabilities,
            BuildProducer<ResteasyJaxrsProviderBuildItem> resteasyJaxrsProviderBuildItemBuildProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (capabilities.isPresent(Capability.RESTEASY)) {

            additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                    .addBeanClasses(ClassicServerEventRecorderProducer.class)
                    .build());

            resteasyJaxrsProviderBuildItemBuildProducer
                    .produce(new ResteasyJaxrsProviderBuildItem(ClassicServerFilter.class.getName()));
        }
    }

    @BuildStep
    ServiceStartBuildItem runtimeInit(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar
                .forService("io.quarkus.jfr.disable-events")
                .require(JfrRuntimeConfig.class)
                .onStart((ctx, runtimeConfig) -> JfrRuntimeSetup.disableEvents(runtimeConfig));
        return new ServiceStartBuildItem(Feature.JFR);
    }
}
