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
        if (openTelemetry instanceof OpenTelemetrySdk) {
            // between flush and shutdown we will wait shutdown-wait-time, at the most.
            var waitTime = getShutdownWaitTime().dividedBy(2);
            var openTelemetrySdk = ((OpenTelemetrySdk) openTelemetry);
            openTelemetrySdk.getSdkTracerProvider().forceFlush().join(waitTime.toMillis(), MILLISECONDS);
            openTelemetrySdk.getSdkMeterProvider().forceFlush().join(waitTime.toMillis(), MILLISECONDS);
            openTelemetrySdk.shutdown().join(waitTime.toMillis(), MILLISECONDS);
        }
    }

    public static Duration getShutdownWaitTime() {
        var config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        var waitTime = config.getOptionalValue("quarkus.otel.experimental.shutdown-wait-time", Duration.class)
                .orElse(Duration.ofSeconds(1));
        return waitTime;
    }
}
