package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import jakarta.inject.Singleton;
import jakarta.servlet.DispatcherType;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;

/**
 * Avoid directly referencing optional dependencies
 */
public class HttpBinderProcessor {
    // Common MeterFilter (uri variation limiter)
    static final String HTTP_METER_FILTER_CONFIGURATION = "io.quarkus.micrometer.runtime.binder.HttpMeterFilterProvider";

    // JAX-RS, Servlet Filters
    static final String UNDERTOW_SERVLET_FILTER_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderUndertowServletFilter";

    private static final String JAKARTA_REST_CLIENT_REQUEST_FILTER = "jakarta.ws.rs.client.ClientRequestFilter";
    private static final String RESTEASY_CLIENT_METRICS_FILTER = "io.quarkus.micrometer.runtime.binder.ResteasyClientMetricsFilter";
    private static final String REST_CLIENT_METRICS_FILTER = "io.quarkus.micrometer.runtime.binder.vertx.RestClientMetricsFilter";
    private static final String REST_CLIENT_BUILDER_METRICS_LISTENER = "io.quarkus.micrometer.runtime.binder.vertx.RestClientBuilderMetricsListener";

    static class HttpServerBinderEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return mConfig.isEnabled(mConfig.binder().vertx())
                    && mConfig.isEnabled(mConfig.binder().httpServer());
        }
    }

    static class RestClientBinderEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime(JAKARTA_REST_CLIENT_REQUEST_FILTER)
                    && mConfig.isEnabled(mConfig.binder().httpClient());
        }
    }

    @BuildStep(onlyIf = MicrometerProcessor.MicrometerEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem enableHttpBinders(MicrometerRecorder recorder,
            MicrometerConfig buildTimeConfig,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        boolean clientEnabled = buildTimeConfig.isEnabled(buildTimeConfig.binder().httpClient());
        boolean serverEnabled = buildTimeConfig.isEnabled(buildTimeConfig.binder().httpServer());

        if (clientEnabled || serverEnabled) {
            // Protect from uri tag flood
            createAdditionalBean(additionalBeans, HTTP_METER_FILTER_CONFIGURATION);
        }

        // Other things use this bean to test whether http server/client metrics are enabled
        return SyntheticBeanBuildItem
                .configure(HttpBinderConfiguration.class)
                .scope(Singleton.class)
                .setRuntimeInit()
                .unremovable()
                .runtimeValue(recorder.configureHttpMetrics(serverEnabled, clientEnabled))
                .done();
    }

    @BuildStep(onlyIf = HttpServerBinderEnabled.class)
    void enableHttpServerSupport(Capabilities capabilities,
            BuildProducer<io.quarkus.undertow.deployment.FilterBuildItem> servletFilters,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

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

    @BuildStep(onlyIf = RestClientBinderEnabled.class)
    void registerProvider(Capabilities capabilities,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexed,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        if (capabilities.isPresent(Capability.RESTEASY_CLIENT)) {
            additionalIndexed.produce(new AdditionalIndexedClassesBuildItem(RESTEASY_CLIENT_METRICS_FILTER));
            additionalBeans.produce(new AdditionalBeanBuildItem(RESTEASY_CLIENT_METRICS_FILTER));
        } else if (capabilities.isPresent(Capability.REST_CLIENT_REACTIVE)) {
            additionalIndexed.produce(new AdditionalIndexedClassesBuildItem(REST_CLIENT_METRICS_FILTER));
            additionalBeans.produce(new AdditionalBeanBuildItem(REST_CLIENT_METRICS_FILTER));
            serviceProviders
                    .produce(new ServiceProviderBuildItem("org.eclipse.microprofile.rest.client.spi.RestClientBuilderListener",
                            REST_CLIENT_BUILDER_METRICS_LISTENER));
        }
    }

    private void createAdditionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeans, String className) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(className)
                .setUnremovable().build());
    }
}
