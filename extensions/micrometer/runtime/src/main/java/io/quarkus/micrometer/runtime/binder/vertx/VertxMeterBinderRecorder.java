package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.micrometer.runtime.binder.HttpBinderConfiguration;
import io.quarkus.micrometer.runtime.export.exemplars.OpenTelemetryContextUnwrapper;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.VertxOptions;
import io.vertx.core.internal.VertxBootstrap;

@Recorder
public class VertxMeterBinderRecorder {

    static VertxMeterBinderAdapter binderAdapter = new VertxMeterBinderAdapter();
    static volatile HttpBinderConfiguration devModeConfig;

    public Consumer<VertxBootstrap> configureMetricFactory() {
        return bootstrap -> {
            bootstrap.metricsFactory(binderAdapter);
        };
    }

    public Consumer<VertxOptions> configureMetricsOptions() {
        return options -> {
            options.setMetricsOptions(binderAdapter);
        };
    }

    public void configureBinderAdapter() {
        HttpBinderConfiguration httpConfig = Arc.container().instance(HttpBinderConfiguration.class).get();
        OpenTelemetryContextUnwrapper openTelemetryContextUnwrapper = Arc.container()
                .instance(OpenTelemetryContextUnwrapper.class).get();
        if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            if (devModeConfig == null) {
                // Create an object whose attributes we can update
                devModeConfig = httpConfig.unwrap();
                binderAdapter.initBinder(devModeConfig, openTelemetryContextUnwrapper);
            } else {
                // update config attributes
                devModeConfig.update(httpConfig);
            }
        } else {
            // unwrap the CDI bean (use POJO)
            binderAdapter.initBinder(httpConfig.unwrap(), openTelemetryContextUnwrapper);
        }
    }
}
