package io.quarkus.opentelemetry.runtime.tracing.binders.vertx;

import java.util.function.Consumer;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.tracing.TracingOptions;

@Recorder
public class VertxRecorder {

    /* STATIC INIT */
    public Consumer<VertxOptions> getVertxTracingOptions() {
        TracingOptions tracingOptions = new TracingOptions()
                .setFactory(new OpenTelemetryVertxTracingFactory());
        return vertxOptions -> vertxOptions.setTracingOptions(tracingOptions);
    }

    public Consumer<VertxOptions> getVertxTracingMetricsOptions() {
        MetricsOptions metricsOptions = new MetricsOptions()
                .setEnabled(true)
                .setFactory(new OpenTelemetryVertxMetricsFactory());
        return vertxOptions -> vertxOptions.setMetricsOptions(metricsOptions);
    }
}
