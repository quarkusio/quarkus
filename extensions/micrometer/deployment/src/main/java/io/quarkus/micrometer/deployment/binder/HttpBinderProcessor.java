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
    static final String RESTEASY_CONTAINER_FILTER_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderRestEasyContainerFilter";
    static final String RESTEASY_REACTIVE_CONTAINER_FILTER_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderRestEasyReactiveContainerFilter";
    static final String UNDERTOW_SERVLET_FILTER_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderUndertowServletFilter";

    private static final String REST_CLIENT_REQUEST_FILTER = "jakarta.ws.rs.client.ClientRequestFilter";
    private static final String REST_CLIENT_METRICS_FILTER = "io.quarkus.micrometer.runtime.binder.RestClientMetricsFilter";

    static class HttpServerBinderEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return mConfig.checkBinderEnabledWithDefault(mConfig.binder().vertx())
                    && mConfig.checkBinderEnabledWithDefault(mConfig.binder().httpServer());
        }
    }

    static class HttpClientBinderEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return QuarkusClassLoader.isClassPresentAtRuntime(REST_CLIENT_REQUEST_FILTER)
                    && mConfig.checkBinderEnabledWithDefault(mConfig.binder().httpClient());
        }
    }

    @BuildStep(onlyIf = MicrometerProcessor.MicrometerEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem enableHttpBinders(MicrometerRecorder recorder,
            MicrometerConfig buildTimeConfig,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        boolean clientEnabled = buildTimeConfig.checkBinderEnabledWithDefault(buildTimeConfig.binder().httpClient());
        boolean serverEnabled = buildTimeConfig.checkBinderEnabledWithDefault(buildTimeConfig.binder().httpServer());

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

    @BuildStep(onlyIf = HttpClientBinderEnabled.class)
    void registerProvider(BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexed,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalIndexed.produce(new AdditionalIndexedClassesBuildItem(REST_CLIENT_METRICS_FILTER));
        additionalBeans.produce(new AdditionalBeanBuildItem(REST_CLIENT_METRICS_FILTER));
    }

    private void createAdditionalBean(BuildProducer<AdditionalBeanBuildItem> additionalBeans, String className) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(className)
                .setUnremovable().build());
    }
}
