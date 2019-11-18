package io.quarkus.smallrye.opentracing.deployment;

import java.lang.reflect.Method;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.servlet.DispatcherType;

import io.opentracing.contrib.interceptors.OpenTracingInterceptor;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.smallrye.opentracing.runtime.QuarkusSmallRyeTracingDynamicFeature;
import io.quarkus.smallrye.opentracing.runtime.QuarkusSmallRyeTracingStandaloneVertxDynamicFeature;
import io.quarkus.smallrye.opentracing.runtime.TracerProducer;
import io.quarkus.undertow.deployment.FilterBuildItem;

public class SmallRyeOpenTracingProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return new AdditionalBeanBuildItem(OpenTracingInterceptor.class, TracerProducer.class);
    }

    @BuildStep
    ReflectiveMethodBuildItem registerMethod() throws Exception {
        Method isAsync = ObserverMethod.class.getMethod("isAsync");
        return new ReflectiveMethodBuildItem(isAsync);
    }

    @BuildStep
    void setupFilter(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            BuildProducer<FilterBuildItem> filterProducer,
            BuildProducer<FeatureBuildItem> feature,
            Capabilities capabilities) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_OPENTRACING));

        providers.produce(new ResteasyJaxrsProviderBuildItem(QuarkusSmallRyeTracingDynamicFeature.class.getName()));

        if (capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
            FilterBuildItem filterInfo = FilterBuildItem.builder("tracingFilter", SpanFinishingFilter.class.getName())
                    .setAsyncSupported(true)
                    .addFilterUrlMapping("*", DispatcherType.FORWARD)
                    .addFilterUrlMapping("*", DispatcherType.INCLUDE)
                    .addFilterUrlMapping("*", DispatcherType.REQUEST)
                    .addFilterUrlMapping("*", DispatcherType.ASYNC)
                    .addFilterUrlMapping("*", DispatcherType.ERROR)
                    .build();
            filterProducer.produce(filterInfo);
        } else {
            //otherwise we know we have RESTeasy on vert.x
            providers.produce(
                    new ResteasyJaxrsProviderBuildItem(QuarkusSmallRyeTracingStandaloneVertxDynamicFeature.class.getName()));
        }
    }

}
