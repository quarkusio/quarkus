package io.quarkus.tck.opentelemetry;

import java.util.function.Function;

import io.smallrye.config.RelocateConfigSourceInterceptor;

/**
 * MicroProfile Telemetry must read <code>otel.*</code>
 * <a href=
 * "https://github.com/eclipse/microprofile-telemetry/blob/1.1/tracing/spec/src/main/asciidoc/microprofile-telemetry-tracing-spec.asciidoc#configuration">properties</a>.
 * <br>
 * Quarkus only reads <code>quarkus.otel.*</code> properties to be consistent with Quarkus configuration model. This
 * interceptor relocates the Quarkus OpenTelemetry configuration in the standard OpenTelemetry configuration.
 */
public class TelemetryRelocateInterceptor extends RelocateConfigSourceInterceptor {
    public TelemetryRelocateInterceptor() {
        super(new Function<String, String>() {
            @Override
            public String apply(final String name) {
                if (name.startsWith("quarkus.otel.")) {
                    return name.substring(8);
                } else if (name.startsWith("otel.")) {
                    return "quarkus." + name;
                }
                return name;
            }
        });
    }
}
