package io.quarkus.resteasy.server.common.deployment;

import java.util.Optional;
import java.util.function.BooleanSupplier;

import javax.servlet.DispatcherType;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;

/**
 * If resteasy metrics are enabled and the smallrye-metrics extension (specifically) is enabled,
 * register additional filters specific to smallrye metrics.
 */
public class ResteasySmallRyeMetricsProcessor {
    static final String SMALLRYE_JAXRS_FILTER_CLASS_NAME = "io.smallrye.metrics.jaxrs.JaxRsMetricsFilter";
    static final String SMALLRYE_JAXRS_SERVLET_FILTER_CLASS_NAME = "io.smallrye.metrics.jaxrs.JaxRsMetricsServletFilter";
    static final String SMALLRYE_JAXRS_QUARKUS_FILTER_CLASS_NAME = "io.quarkus.smallrye.metrics.runtime.QuarkusJaxRsMetricsFilter";

    static final Class<?> SMALLRYE_JAXRS_FILTER_CLASS = getClassForName(SMALLRYE_JAXRS_FILTER_CLASS_NAME);

    static class SmallRyeMetricsEnabled implements BooleanSupplier {
        ResteasyServerCommonProcessor.ResteasyConfig buildConfig;

        public boolean getAsBoolean() {
            return SMALLRYE_JAXRS_FILTER_CLASS != null && buildConfig.metricsEnabled;
        }
    }

    // Ensure class is present (smallrye metrics extension) and resteasy metrics are enabled
    @BuildStep(onlyIf = SmallRyeMetricsEnabled.class)
    void enableMetrics(Optional<MetricsCapabilityBuildItem> metricsCapabilityBuildItem,
            BuildProducer<ResteasyJaxrsProviderBuildItem> jaxRsProviders,
            BuildProducer<FilterBuildItem> servletFilters,
            Capabilities capabilities) {

        if (metricsCapabilityBuildItem.isPresent()) {
            if (capabilities.isPresent(Capability.SERVLET)) {
                // if running with servlet, use the MetricsFilter implementation from SmallRye
                jaxRsProviders.produce(
                        new ResteasyJaxrsProviderBuildItem(SMALLRYE_JAXRS_FILTER_CLASS_NAME));
                servletFilters.produce(
                        FilterBuildItem.builder("metricsFilter", SMALLRYE_JAXRS_SERVLET_FILTER_CLASS_NAME)
                                .setAsyncSupported(true)
                                .addFilterUrlMapping("*", DispatcherType.FORWARD)
                                .addFilterUrlMapping("*", DispatcherType.INCLUDE)
                                .addFilterUrlMapping("*", DispatcherType.REQUEST)
                                .addFilterUrlMapping("*", DispatcherType.ASYNC)
                                .addFilterUrlMapping("*", DispatcherType.ERROR)
                                .build());
            } else {
                // if running with vert.x, use the MetricsFilter implementation from Quarkus codebase
                jaxRsProviders.produce(
                        new ResteasyJaxrsProviderBuildItem(SMALLRYE_JAXRS_QUARKUS_FILTER_CLASS_NAME));
            }
        }
    }

    public static Class<?> getClassForName(String classname) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(classname, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
        }
        return clazz;
    }
}
