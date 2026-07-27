package io.quarkus.opentelemetry;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;

import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.quarkus.arc.BeanDestroyer;
import io.smallrye.config.SmallRyeConfig;

public class OpenTelemetryDestroyer implements BeanDestroyer<OpenTelemetry> {
    @Override
    public void destroy(OpenTelemetry openTelemetry, CreationalContext<OpenTelemetry> creationalContext,
            Map<String, Object> params) {
        if (openTelemetry instanceof OpenTelemetrySdk openTelemetrySdk) {
            // between flush and shutdown we will wait shutdown-wait-time, at the most.
            var waitTime = getShutdownWaitTime().dividedBy(4);
            openTelemetrySdk.getSdkLoggerProvider().forceFlush().join(waitTime.toMillis(), MILLISECONDS);
            openTelemetrySdk.getSdkTracerProvider().forceFlush().join(waitTime.toMillis(), MILLISECONDS);
            openTelemetrySdk.getSdkMeterProvider().forceFlush().join(waitTime.toMillis(), MILLISECONDS);
            openTelemetrySdk.shutdown().join(waitTime.toMillis(), MILLISECONDS);
            // interrupt batch processor worker threads that are still parked
            // in their poll() loop — they've been told to shut down but haven't
            // woken up from their timed wait yet
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.isAlive() && (t.getName().startsWith("BatchSpanProcessor")
                        || t.getName().startsWith("BatchLogRecordProcessor"))) {
                    t.interrupt();
                }
            }
        }
    }

    public static Duration getShutdownWaitTime() {
        var config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        return config.getOptionalValue("quarkus.otel.experimental.shutdown-wait-time", Duration.class)
                .orElse(Duration.ofSeconds(2));
    }
}
