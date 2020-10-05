package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import javax.interceptor.Interceptor;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter;
import io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderContainerFilter;
import io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderRecorder;
import io.quarkus.micrometer.runtime.binder.vertx.VertxMeterFilter;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.micrometer.runtime.config.runtime.VertxConfig;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.vertx.core.deployment.VertxOptionsConsumerBuildItem;
import io.quarkus.vertx.http.deployment.FilterBuildItem;

/**
 * Add support for the Vert.x and other http instrumentation.
 * Note that various bits of support may not be present at deploy time,
 * e.g. Vert.x can be present while resteasy is not.
 * 
 * Avoid referencing classes that in turn import optional dependencies.
 */
public class VertxBinderProcessor {
    static final String METRIC_OPTIONS_CLASS_NAME = "io.vertx.core.metrics.MetricsOptions";
    static final Class<?> METRIC_OPTIONS_CLASS = MicrometerRecorder.getClassForName(METRIC_OPTIONS_CLASS_NAME);

    static class VertxBinderEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return METRIC_OPTIONS_CLASS != null && mConfig.checkBinderEnabledWithDefault(mConfig.binder.vertx);
        }
    }

    // avoid imports due to related deps not being there
    static final String VERTX_CONTAINER_FILTER_CLASS_NAME = "io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderContainerFilter";

    @BuildStep(onlyIf = { VertxBinderEnabled.class })
    ResteasyJaxrsProviderBuildItem enableResteasySupport(Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (!capabilities.isPresent(Capability.RESTEASY)) {
            return null;
        }

        additionalBeans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(VertxMeterBinderContainerFilter.class)
                .setUnremovable().build());

        return new ResteasyJaxrsProviderBuildItem(VERTX_CONTAINER_FILTER_CLASS_NAME);
    }

    @BuildStep(onlyIf = VertxBinderEnabled.class)
    AdditionalBeanBuildItem createVertxAdapters() {
        // Add Vertx meter adapters
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(VertxMeterBinderAdapter.class)
                .setUnremovable().build();
    }

    @BuildStep(onlyIf = VertxBinderEnabled.class)
    FilterBuildItem addVertxMeterFilter() {
        return new FilterBuildItem(new VertxMeterFilter(), Integer.MAX_VALUE);
    }

    @BuildStep(onlyIf = VertxBinderEnabled.class)
    @Record(value = ExecutionTime.STATIC_INIT)
    VertxOptionsConsumerBuildItem build(VertxMeterBinderRecorder recorder) {
        return new VertxOptionsConsumerBuildItem(recorder.configureMetricsAdapter(), Interceptor.Priority.LIBRARY_AFTER);
    }

    @BuildStep(onlyIf = VertxBinderEnabled.class)
    @Record(value = ExecutionTime.RUNTIME_INIT)
    void setVertxConfig(VertxMeterBinderRecorder recorder, VertxConfig config) {
        recorder.setVertxConfig(config);
    }
}
