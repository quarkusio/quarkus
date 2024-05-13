package io.quarkus.opentelemetry.runtime.config;

import java.util.Map;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.RelocateConfigSourceInterceptor;

public class MpTelemetryRelocateConfigSourceInterceptor implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
        ConfigValue mpCompatibility = context.proceed("quarkus.otel.mp.compatibility");
        if (mpCompatibility != null && mpCompatibility.getValue() != null) {
            if (Converters.getImplicitConverter(Boolean.class).convert(mpCompatibility.getValue())) {
                return new RelocateConfigSourceInterceptor(Map.of(
                        "quarkus.otel.service.name", "otel.service.name",
                        "quarkus.otel.resource.attributes", "otel.resource.attributes"));
            }
        }
        return new ConfigSourceInterceptor() {
            @Override
            public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                return context.proceed(name);
            }
        };
    }
}
