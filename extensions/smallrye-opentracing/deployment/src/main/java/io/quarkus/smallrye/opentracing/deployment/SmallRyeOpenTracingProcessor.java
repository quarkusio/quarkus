package io.quarkus.smallrye.opentracing.deployment;

import java.lang.reflect.Method;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.servlet.DispatcherType;

import io.opentracing.Tracer;
import io.opentracing.contrib.interceptors.OpenTracingInterceptor;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerResponseFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.DynamicFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.WriterInterceptorBuildItem;
import io.quarkus.smallrye.opentracing.runtime.QuarkusSmallRyeTracingDynamicFeature;
import io.quarkus.smallrye.opentracing.runtime.QuarkusSmallRyeTracingStandaloneContainerResponseFilter;
import io.quarkus.smallrye.opentracing.runtime.QuarkusSmallRyeTracingStandaloneVertxDynamicFeature;
import io.quarkus.smallrye.opentracing.runtime.TracerProducer;
import io.quarkus.undertow.deployment.FilterBuildItem;

public class SmallRyeOpenTracingProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerBeans(BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        // Some components obtain the tracer via CDI.current().select(Tracer.class)
        // E.g. io.quarkus.smallrye.opentracing.runtime.QuarkusSmallRyeTracingDynamicFeature and io.smallrye.graphql.cdi.tracing.TracingService
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(Tracer.class));
        return new AdditionalBeanBuildItem(OpenTracingInterceptor.class, TracerProducer.class);
    }

    @BuildStep
    ReflectiveMethodBuildItem registerMethod() throws Exception {
        Method isAsync = ObserverMethod.class.getMethod("isAsync");
        return new ReflectiveMethodBuildItem(isAsync);
    }

    @BuildStep
    void setupFilter(
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            BuildProducer<FilterBuildItem> filterProducer,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<CustomContainerResponseFilterBuildItem> customResponseFilters,
            BuildProducer<DynamicFeatureBuildItem> dynamicFeatures,
            BuildProducer<WriterInterceptorBuildItem> writerInterceptors,
            Capabilities capabilities) {

        feature.produce(new FeatureBuildItem(Feature.SMALLRYE_OPENTRACING));

        additionalBeans.produce(new AdditionalBeanBuildItem(QuarkusSmallRyeTracingDynamicFeature.class));
        providers.produce(new ResteasyJaxrsProviderBuildItem(QuarkusSmallRyeTracingDynamicFeature.class.getName()));

        if (capabilities.isPresent(Capability.SERVLET)) {
            FilterBuildItem filterInfo = FilterBuildItem.builder("tracingFilter", SpanFinishingFilter.class.getName())
                    .setAsyncSupported(true)
                    .addFilterUrlMapping("*", DispatcherType.FORWARD)
                    .addFilterUrlMapping("*", DispatcherType.INCLUDE)
                    .addFilterUrlMapping("*", DispatcherType.REQUEST)
                    .addFilterUrlMapping("*", DispatcherType.ASYNC)
                    .addFilterUrlMapping("*", DispatcherType.ERROR)
                    .build();
            filterProducer.produce(filterInfo);
        } else if (capabilities.isPresent(Capability.RESTEASY)) {
            providers.produce(
                    new ResteasyJaxrsProviderBuildItem(QuarkusSmallRyeTracingStandaloneVertxDynamicFeature.class.getName()));
        } else if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            customResponseFilters.produce(new CustomContainerResponseFilterBuildItem(
                    QuarkusSmallRyeTracingStandaloneContainerResponseFilter.class.getName()));
            dynamicFeatures.produce(new DynamicFeatureBuildItem(QuarkusSmallRyeTracingDynamicFeature.class.getName()));
            writerInterceptors.produce(
                    new WriterInterceptorBuildItem.Builder(
                            QuarkusSmallRyeTracingStandaloneContainerResponseFilter.class.getName()).build());
        }
    }
}
