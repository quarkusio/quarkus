package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import javax.inject.Singleton;
import javax.servlet.DispatcherType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.binder.vertx.VertxMeterFilter;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpClientConfig;
import io.quarkus.micrometer.runtime.config.runtime.HttpServerConfig;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.CustomContainerRequestFilterBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;

/**
 * Avoid directly referencing optional dependencies
 */
public class HttpBinderProcessor {
    // Common MeterFilter (uri variation limiter)
    static final String HTTP_METER_FILTER_CONFIGURATION = "io.quarkus.micrometer.runtime.binder.HttpMeterFilterProvider";

    // JAX-RS, Servlet Filters
    static final String RESTEASY_CONTAINER_FILTER_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderRestEasyContainerFilter";
    static final String RESTEASY_REACTIVE_CONTAINER_FILTER_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderRestEasyReactiveContainerFilter";
    static final String UNDERTOW_SERVLET_FILTER_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderUndertowServletFilter";

    // Rest client listener SPI
    private static final String REST_CLIENT_LISTENER_CLASS_NAME = "org.eclipse.microprofile.rest.client.spi.RestClientListener";
    private static final Class<?> REST_CLIENT_LISTENER_CLASS = MicrometerRecorder
            .getClassForName(REST_CLIENT_LISTENER_CLASS_NAME);

    // Rest Client listener
    private static final String REST_CLIENT_METRICS_LISTENER = "io.quarkus.micrometer.runtime.binder.RestClientMetricsListener";

    static class HttpServerBinderEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return mConfig.checkBinderEnabledWithDefault(mConfig.binder.vertx)
                    && mConfig.checkBinderEnabledWithDefault(mConfig.binder.httpServer);
        }
    }

    static class HttpClientBinderEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return REST_CLIENT_LISTENER_CLASS != null
                    && mConfig.checkBinderEnabledWithDefault(mConfig.binder.httpClient);
        }
    }

    @BuildStep(onlyIf = { MicrometerProcessor.MicrometerEnabled.class })
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem enableHttpBinders(MicrometerRecorder recorder,
            MicrometerConfig buildTimeConfig,
            HttpServerConfig serverConfig,
            HttpClientConfig clientConfig,
            VertxConfig vertxConfig,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        boolean clientEnabled = buildTimeConfig.checkBinderEnabledWithDefault(buildTimeConfig.binder.httpClient);
        boolean serverEnabled = buildTimeConfig.checkBinderEnabledWithDefault(buildTimeConfig.binder.httpServer);

        if (clientEnabled || serverEnabled) {
            // Protect from uri tag flood
            createAdditionalBean(additionalBeans, HTTP_METER_FILTER_CONFIGURATION);
        }

        // Other things use this bean to test whether or not http server/client metrics are enabled
        return SyntheticBeanBuildItem
                .configure(HttpBinderConfiguration.class)
                .scope(Singleton.class)
                .setRuntimeInit()
                .unremovable()
                .runtimeValue(recorder.configureHttpMetrics(serverEnabled, clientEnabled,
                        serverConfig, clientConfig, vertxConfig))
                .done();
    }

    @BuildStep(onlyIf = { VertxBinderProcessor.VertxBinderEnabled.class, HttpServerBinderEnabled.class })
    FilterBuildItem addVertxMeterFilter() {
        return new FilterBuildItem(new VertxMeterFilter(), Integer.MAX_VALUE);
    }

    @BuildStep(onlyIf = { VertxBinderProcessor.VertxBinderEnabled.class, HttpServerBinderEnabled.class })
    void enableHttpServerSupport(Capabilities capabilities,
            BuildProducer<ResteasyJaxrsProviderBuildItem> resteasyJaxrsProviders,
            BuildProducer<CustomContainerRequestFilterBuildItem> customContainerRequestFilter,
            BuildProducer<io.quarkus.undertow.deployment.FilterBuildItem> servletFilters,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Will have one or the other of these (exclusive)
        if (capabilities.isPresent(Capability.RESTEASY)) {
            resteasyJaxrsProviders.produce(new ResteasyJaxrsProviderBuildItem(RESTEASY_CONTAINER_FILTER_CLASS_NAME));
            createAdditionalBean(additionalBeans, RESTEASY_CONTAINER_FILTER_CLASS_NAME);
        } else if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            customContainerRequestFilter
                    .produce(new CustomContainerRequestFilterBuildItem(RESTEASY_REACTIVE_CONTAINER_FILTER_CLASS_NAME));
            createAdditionalBean(additionalBeans, RESTEASY_REACTIVE_CONTAINER_FILTER_CLASS_NAME);
        }

        // But this might be present as well (fallback. Rest URI processing preferred)
        if (capabilities.isPresent(Capability.SERVLET)) {
            servletFilters.produce(
                    io.quarkus.undertow.deployment.FilterBuildItem.builder("metricsFilter", UNDERTOW_SERVLET_FILTER_CLASS_NAME)
                            .setAsyncSupported(true)
                            .addFilterUrlMapping("*", DispatcherType.FORWARD)
                            .addFilterUrlMapping("*", DispatcherType.INCLUDE)
                            .addFilterUrlMapping("*", DispatcherType.REQUEST)
                            .addFilterUrlMapping("*", DispatcherType.ASYNC)
                            .addFilterUrlMapping("*", DispatcherType.ERROR)
                            .build());
            createAdditionalBean(additionalBeans, UNDERTOW_SERVLET_FILTER_CLASS_NAME);
        }
    }

    @BuildStep(onlyIf = { HttpClientBinderEnabled.class })
    void registerRestClientListener(BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/org.eclipse.microprofile.rest.client.spi.RestClientListener"));
        reflectiveClass
                .produce(new ReflectiveClassBuildItem(true, true, REST_CLIENT_METRICS_LISTENER));
    }

    private void createAdditionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeans, String className) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(className)
                .setUnremovable().build());
    }
}
