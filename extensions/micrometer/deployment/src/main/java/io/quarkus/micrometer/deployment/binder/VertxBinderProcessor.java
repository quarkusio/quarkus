package io.quarkus.micrometer.deployment.binder;

import java.util.function.BooleanSupplier;

import jakarta.interceptor.Interceptor;

import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderRecorder;
import io.quarkus.micrometer.runtime.config.MicrometerConfig;
import io.quarkus.vertx.core.deployment.VertxOptionsConsumerBuildItem;

/**
 * Add support for Vert.x instrumentation.
 * HTTP instrumentation is dependent on Vert.x, but has been pulled out into its own processor
 *
 * Avoid referencing classes that in turn import optional dependencies.
 */
@BuildSteps(onlyIf = VertxBinderProcessor.VertxBinderEnabled.class)
public class VertxBinderProcessor {
    static final String METRIC_OPTIONS_CLASS_NAME = "io.vertx.core.metrics.MetricsOptions";
    static final Class<?> METRIC_OPTIONS_CLASS = MicrometerRecorder.getClassForName(METRIC_OPTIONS_CLASS_NAME);

    static class VertxBinderEnabled implements BooleanSupplier {
        MicrometerConfig mConfig;

        public boolean getAsBoolean() {
            return METRIC_OPTIONS_CLASS != null && mConfig.checkBinderEnabledWithDefault(mConfig.binder.vertx);
        }
    }

    @BuildStep
    UnremovableBeanBuildItem unremoveableAdditionalHttpServerMetrics() {
        return UnremovableBeanBuildItem.beanTypes(HttpServerMetricsTagsContributor.class);
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    VertxOptionsConsumerBuildItem build(VertxMeterBinderRecorder recorder) {
        return new VertxOptionsConsumerBuildItem(recorder.setVertxMetricsOptions(), Interceptor.Priority.LIBRARY_AFTER);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    void setVertxConfig(VertxMeterBinderRecorder recorder) {
        recorder.configureBinderAdapter();
    }
}
